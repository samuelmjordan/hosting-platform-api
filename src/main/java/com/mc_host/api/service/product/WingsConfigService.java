package com.mc_host.api.service.product;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mc_host.api.configuration.HetznerConfiguration;
import com.mc_host.api.model.node.DnsARecord;
import com.mc_host.api.model.node.HetznerNode;

@Service
public class WingsConfigService {
    private static final Logger LOGGER = Logger.getLogger(WingsConfigService.class.getName());
    private static final String USERNAME = "root";
    private static final int PORT = 22;
    private static final int TIMEOUT = 300;
    private static final int DELAY = 3000;

    private final HetznerConfiguration hetznerConfiguration;
    private final ObjectMapper objectMapper;
    private final ObjectMapper yamlMapper;

    WingsConfigService(
        HetznerConfiguration hetznerConfiguration,
        ObjectMapper objectMapper,
        ObjectMapper yamlMapper
    ) {
        this.hetznerConfiguration = hetznerConfiguration;
        this.objectMapper = objectMapper;
        this.yamlMapper = yamlMapper;
    }

    public void setupWings(DnsARecord dnsARecord, String jsonConfig) throws IOException {
        try (SSHClient ssh = new SSHClient()) {
            String privateKey = hetznerConfiguration.getSshPrivateKey()
                .replace("\\n", "\n")
                .replace("\r", "")
                .trim();
            
            Path keyPath = Files.createTempFile("ssh-key", null);
            Files.write(keyPath, privateKey.getBytes(), StandardOpenOption.WRITE);
            
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
                    if (retries >= 3) {
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
                Map<String, Object> configMap = objectMapper.readValue(jsonConfig, Map.class);
                yamlConfig =  yamlMapper.writeValueAsString(configMap);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to convert JSON to YAML", e);
                throw new RuntimeException("Failed to convert configuration format: " + e.getMessage(), e);
            }

            String escapedConfig = yamlConfig.replace("'", "'\\''");

            String[] commands = {
                "apt-get update && apt-get install -y certbot",
                "certbot certonly --standalone --non-interactive --agree-tos --email samuelmjordandev@gmail.com -d " + dnsARecord.recordName(),
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
            throw new RuntimeException("Failed to setup Wings: " + e.getMessage(), e);
        }
    }
}