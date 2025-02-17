package com.mc_host.api.model.hetzner;

public class HetznerServerResponse {
    public Server server;
    public static class Server {
        public long id;
        public String name;
        public String status;
        public PublicNet public_net;
        
        public static class PublicNet {
            public IPv4 ipv4;
            public static class IPv4 {
                public String ip;
                public boolean blocked;
            }
        }
    }
}
