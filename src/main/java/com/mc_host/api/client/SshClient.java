package com.mc_host.api.client;

import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.mc_host.api.configuration.HetznerConfiguration;

@Service
public class SshClient {
    private static final Logger LOGGER = Logger.getLogger(SshClient.class.getName());

    private static final String USERNAME = "root";
    private static final int PORT = 22;

    private final HetznerConfiguration hetznerConfiguration;

    SshClient(
        HetznerConfiguration hetznerConfiguration
    ) {
        this.hetznerConfiguration = hetznerConfiguration;
    }
    
    public void setupWings(String host) throws JSchException {
        JSch jsch = new JSch();
        Session session = null;
        
        try {
            byte[] keyBytes = Base64.getDecoder().decode(hetznerConfiguration.getSshKey());

            jsch.addIdentity("dev", keyBytes, null, null);
            session = jsch.getSession(USERNAME, host, PORT);
            session.setConfig("StrictHostKeyChecking", "no");
            LOGGER.log(Level.INFO, String.format("Connecting via SSH %s@%s:%s", USERNAME, host, PORT));
            session.connect(60000);

            String[] commands = {
                "curl -sSL https://get.docker.com/ | CHANNEL=stable bash",
                "apt install -y tar unzip make gcc g++ python",
                "mkdir -p /etc/pterodactyl",
                "cd /etc/pterodactyl && curl -L -o wings \"https://github.com/pterodactyl/wings/releases/latest/download/wings_linux_$([[ \"$(uname -m)\" == \"x86_64\" ]] && echo \"amd64\" || echo \"arm64\")\"",
                "chmod u+x /etc/pterodactyl/wings",
                "echo '[Unit]\nDescription=Pterodactyl Wings Daemon\nAfter=docker.service\nRequires=docker.service\n\n[Service]\nUser=root\nWorkingDirectory=/etc/pterodactyl\nLimitNOFILE=4096\nPIDFile=/var/run/wings/daemon.pid\nExecStart=/etc/pterodactyl/wings\nRestart=on-failure\nStartLimitInterval=180\nStartLimitBurst=30\n\n[Install]\nWantedBy=multi-user.target' > /etc/systemd/system/wings.service",
                "systemctl enable wings",
                "systemctl start wings"
            };

            for (String command : commands) {
                Channel channel = session.openChannel("exec");
                ((ChannelExec) channel).setCommand(command);
                
                channel.setInputStream(null);
                ((ChannelExec) channel).setErrStream(System.err);

                channel.connect();

                while (!channel.isClosed()) {
                    Thread.sleep(1000);
                }

                channel.disconnect();

                if (((ChannelExec) channel).getExitStatus() != 0) {
                    throw new RuntimeException("Command failed: " + command);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to setup Wings: " + e.getMessage(), e);
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }
}
