package com.mc_host.api.model.resource.hetzner;

public class HetznerServerResponse {
    public Server server;
    
    public static class Server {
        public long id;
        public String name;
        public String status;
        public PublicNet public_net;
        public Datacenter datacenter;
        
        public static class PublicNet {
            public IPv4 ipv4;
            
            public static class IPv4 {
                public String ip;
                public boolean blocked;
            }
        }
        
        public static class Datacenter {
            public long id;
            public String name;
            public String description;
            public Location location;
            
            public static class Location {
                public long id;
                public String name;
                public String description;
                public String country;
                public String city;
                public double latitude;
                public double longitude;
            }
        }
    }
}
