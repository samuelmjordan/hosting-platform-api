package com.mc_host.api.service.resources;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mc_host.api.configuration.CloudflareConfiguration;
import com.mc_host.api.configuration.SshConfiguration;
import com.mc_host.api.model.resource.dns.DnsARecord;
import lombok.RequiredArgsConstructor;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
@RequiredArgsConstructor
public class WingsService {
    private static final Logger LOGGER = Logger.getLogger(WingsService.class.getName());
    private static final String USERNAME = "root";
    private static final int PORT = 22;
    private static final int TIMEOUT = 300;
    private static final int DELAY = 500;

    private final SshConfiguration sshConfiguration;
    private final CloudflareConfiguration cloudflareConfiguration;
    private final ObjectMapper objectMapper;
    private final ObjectMapper yamlMapper;

    public void setupWings(DnsARecord dnsARecord, String jsonConfig) {
        try (SSHClient ssh = new SSHClient()) {
            String sshPrivateKey = sshConfiguration.getPrivateKey()
                .replace("\\n", "\n")
                .replace("\r", "")
                .trim();
            String originCertificate = cloudflareConfiguration.getOriginCert()
                .replace("\\n", "\n")
                .replace("\r", "")
                .trim();
            String certPrivateKey = cloudflareConfiguration.getPrivateKey()
                .replace("\\n", "\n")
                .replace("\r", "")
                .trim();
            
            Path keyPath = Files.createTempFile("ssh-key", null);
            Files.write(keyPath, sshPrivateKey.getBytes(), StandardOpenOption.WRITE);
            
            int retries = 0;
            int delay = DELAY;
            while (true) {
                try {
                    ssh.addHostKeyVerifier(new PromiscuousVerifier());
                    ssh.loadKeys(keyPath.toString());
                    ssh.connect(dnsARecord.content(), PORT);
                    ssh.authPublickey(USERNAME, keyPath.toString());
                    break;
                } catch(Exception e) {
                    Thread.sleep(delay);
                    delay *= 1.2;
                    retries++;
                    if (retries >= 15) {
                        LOGGER.severe("Failed to authenticate server %s".formatted(dnsARecord.aRecordId()));
                        throw e;
                    }
                }
            }
            
            if (!ssh.isAuthenticated()) {
                throw new RuntimeException("Authentication failed after attempting key auth");
            }
            
            LOGGER.log(Level.INFO, "Successfully authenticated");

            String yamlConfig;
            try {
                Map<String, Object> configMap = objectMapper.readValue(jsonConfig, new TypeReference<Map<String, Object>>() {});
                yamlConfig =  yamlMapper.writeValueAsString(configMap);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to convert JSON to YAML", e);
                throw new RuntimeException("Failed to convert configuration format: " + e.getMessage(), e);
            }

            String escapedConfig = yamlConfig.replace("'", "'\\''");

            String[] commands = {
                "apt-get update",
                "mkdir -p /etc/letsencrypt/live/%s".formatted(dnsARecord.recordName()),
                "echo '%s' > /etc/letsencrypt/live/%s/fullchain.pem".formatted(originCertificate, dnsARecord.recordName()),
                "echo '%s' > /etc/letsencrypt/live/%s/privkey.pem".formatted(certPrivateKey, dnsARecord.recordName()),
                "curl -sSL https://get.docker.com/ | CHANNEL=stable bash",
                "apt install -y tar unzip make gcc g++ python3",
                "mkdir -p /etc/pterodactyl",
                "cd /etc/pterodactyl && curl -L -o wings \"https://github.com/pterodactyl/wings/releases/latest/download/wings_linux_$([[ \"$(uname -m)\" == \"x86_64\" ]] && echo \"amd64\" || echo \"arm64\")\"",
                "chmod u+x /etc/pterodactyl/wings",
                "echo '[Unit]\nDescription=Pterodactyl Wings Daemon\nAfter=docker.service\nRequires=docker.service\n\n[Service]\nUser=root\nWorkingDirectory=/etc/pterodactyl\nLimitNOFILE=4096\nPIDFile=/var/run/wings/daemon.pid\nExecStart=/etc/pterodactyl/wings\nRestart=on-failure\nStartLimitInterval=180\nStartLimitBurst=30\n\n[Install]\nWantedBy=multi-user.target' > /etc/systemd/system/wings.service",
                "cat > /etc/pterodactyl/config.yml << 'EOL'\n" + escapedConfig + "\nEOL",
                "systemctl enable wings",
                "systemctl start wings"
            };

            for (String command : commands) {
                try (Session session = ssh.startSession()) {
                    Command cmd = session.exec(command);
                    
                    String output = new String(cmd.getInputStream().readAllBytes());
                    String error = new String(cmd.getErrorStream().readAllBytes());
                    
                    cmd.join(TIMEOUT, TimeUnit.SECONDS);
                    
                    if (cmd.getExitStatus() != 0 && !command.equals("systemctl status wings")) {
                        throw new RuntimeException(String.format(
                            "Command failed with status %d: %s\nOutput: %s\nError: %s",
                            cmd.getExitStatus(), command, output, error
                        ));
                    }
                }
            }

            try (Session session = ssh.startSession()) {
                Command cmd = session.exec("systemctl status wings");
                String output = new String(cmd.getInputStream().readAllBytes());
                cmd.join(10, TimeUnit.SECONDS);
                
                if (!output.contains("Active: active (running)")) {
                    throw new RuntimeException("wings service was not properly installed");
                }
                LOGGER.log(Level.INFO, "wings system service successfully configured");

            }

        } catch (Exception e) {
            throw new RuntimeException(String.format("[aRecordId: %s] Failed to setup Wings", dnsARecord.aRecordId()), e);
        }
    }
}