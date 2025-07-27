package com.mc_host.api.service.resources;

import com.mc_host.api.client.CloudflareClient;
import com.mc_host.api.client.CloudflareClient.DNSRecordResponse;
import com.mc_host.api.configuration.ApplicationConfiguration;
import com.mc_host.api.model.resource.dns.DnsARecord;
import com.mc_host.api.model.resource.dns.DnsCNameRecord;
import com.mc_host.api.model.resource.hetzner.node.HetznerNodeInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
@RequiredArgsConstructor
public class DnsService {
    private static final Logger LOGGER = Logger.getLogger(DnsService.class.getName());

    private final ApplicationConfiguration applicationConfiguration;
    private final CloudflareClient cloudflareClient;

    public DnsARecord createARecord(HetznerNodeInterface hetznerNode, String subscriptionId) {
        LOGGER.log(Level.INFO, String.format("[hetznerNodeId: %s] Creating DNS A record", hetznerNode.hetznerNodeId()));
        try {
            String zoneId = cloudflareClient.getZoneId(applicationConfiguration.getInfraDomain());
            String subdomain = UUID.randomUUID().toString().replace("-", "");
            DNSRecordResponse dnsARecordResponse = cloudflareClient.createARecord(
                zoneId,
                subdomain,
                hetznerNode.ipv4(),
                true
            );
            DnsARecord dnsARecord = new DnsARecord(
                subscriptionId,
                dnsARecordResponse.id(), 
                zoneId, 
                applicationConfiguration.getInfraDomain(),
                dnsARecordResponse.name(), 
                dnsARecordResponse.content()
            );
            LOGGER.log(Level.INFO, String.format("[hetznerNodeId: %s] Created DNS A record", hetznerNode.hetznerNodeId()));
            return dnsARecord;
        } catch (Exception e) {
            throw new RuntimeException(String.format("[hetznerNodeId: %s] Error Creating DNS A record", hetznerNode.hetznerNodeId()), e);
        }
    }

    public void deleteARecord(DnsARecord dnsARecord) {
        LOGGER.log(Level.INFO, String.format("[aRecordId: %s] Deleting DNS A record", dnsARecord.aRecordId()));
        try {
            cloudflareClient.deleteDNSRecord(dnsARecord.zoneId(), dnsARecord.aRecordId());
            LOGGER.log(Level.INFO, String.format("[aRecordId: %s] Deleted DNS A record", dnsARecord.aRecordId()));
        } catch (Exception e) {
            throw new RuntimeException(String.format("[aRecordId: %s] Error deleting DNS A record", dnsARecord.aRecordId()), e);
        }
    }

    public DnsCNameRecord createCNameRecord(DnsARecord dnsARecord, String subdomain) {
        LOGGER.log(Level.INFO, String.format("[subscriptionId: %s] Creating DNS C NAME record", dnsARecord.subscriptionId()));
        try {
            DNSRecordResponse dnsRecordResponse = cloudflareClient.createCNameRecord(
                dnsARecord.zoneId(),
                subdomain,
                dnsARecord.recordName(),
                false
            );
            DnsCNameRecord dnsCNameRecord = new DnsCNameRecord(
                dnsARecord.subscriptionId(), 
                dnsRecordResponse.id(), 
                dnsARecord.zoneId(), 
                dnsARecord.zoneName(),
                dnsRecordResponse.name(),
                dnsRecordResponse.content()
            );
            LOGGER.log(Level.INFO, String.format("[subscriptionId: %s] Created DNS C NAME record", dnsARecord.subscriptionId()));
            return dnsCNameRecord;
        } catch (Exception e) {
            throw new RuntimeException(String.format("[subscriptionId: %s] Error creating DNS C NAME record", dnsARecord.subscriptionId()), e);
        }
    }

    public DnsCNameRecord redirectCNameRecord(DnsARecord dnsARecord, DnsCNameRecord dnsCNameRecord) {
        LOGGER.log(Level.INFO, String.format("[subscriptionId: %s] Updating DNS C NAME record", dnsARecord.subscriptionId()));
        try {
            DNSRecordResponse dnsCCNameRecordResponse = cloudflareClient.updateCNameRecord(
                dnsARecord.zoneId(), 
                dnsCNameRecord.cNameRecordId(),
                dnsCNameRecord.recordName(),
                dnsARecord.recordName(),
                false
            );
            DnsCNameRecord newDnsCNameRecord = new DnsCNameRecord(
                dnsARecord.subscriptionId(), 
                dnsCCNameRecordResponse.id(), 
                dnsARecord.zoneId(), 
                dnsARecord.zoneName(),
                dnsCCNameRecordResponse.name(),
                dnsCCNameRecordResponse.content()
            );
            LOGGER.log(Level.INFO, String.format("[subscriptionId: %s] Updated DNS C NAME record", dnsARecord.subscriptionId()));
            return newDnsCNameRecord;
        } catch (Exception e) {
            throw new RuntimeException(String.format("[subscriptionId: %s] Error updating DNS C NAME record", dnsARecord.subscriptionId()), e);
        }
    }

    public DnsCNameRecord updateCNameRecordName(DnsCNameRecord dnsCNameRecord, String subdomain) {
        LOGGER.log(Level.INFO, String.format("[subscriptionId: %s] Updating DNS C NAME record", dnsCNameRecord.subscriptionId()));
        try {
            DNSRecordResponse dnsCNameRecordResponse = cloudflareClient.updateCNameRecord(
                dnsCNameRecord.zoneId(), 
                dnsCNameRecord.cNameRecordId(),
                subdomain,
                dnsCNameRecord.content(),
                false
            );
            DnsCNameRecord newDnsCNameRecord = new DnsCNameRecord(
                dnsCNameRecord.subscriptionId(), 
                dnsCNameRecordResponse.id(), 
                dnsCNameRecord.zoneId(), 
                dnsCNameRecord.zoneName(),
                dnsCNameRecordResponse.name(),
                dnsCNameRecordResponse.content()
            );
            LOGGER.log(Level.INFO, String.format("[subscriptionId: %s] Updated DNS C NAME record", dnsCNameRecord.subscriptionId()));
            return newDnsCNameRecord;
        } catch (Exception e) {
            throw new RuntimeException(String.format("[subscriptionId: %s] Error updating DNS C NAME record", dnsCNameRecord.subscriptionId()), e);
        }
    }

    public void deleteCNameRecord(DnsCNameRecord dnsCNameRecord) {
        LOGGER.log(Level.INFO, String.format("[cNameRecordId: %s] Deleting DNS C NAME record", dnsCNameRecord.cNameRecordId()));
        try {
            cloudflareClient.deleteDNSRecord(dnsCNameRecord.zoneId(), dnsCNameRecord.cNameRecordId());
            LOGGER.log(Level.INFO, String.format("[cNameRecordId: %s] Deleted DNS C NAME record", dnsCNameRecord.cNameRecordId()));
        } catch (Exception e) {
            throw new RuntimeException(String.format("[cNameRecordId: %s] Error deleting DNS C NAME record", dnsCNameRecord.cNameRecordId()), e);
        }
    }

    public static String serverPortToSubdomainMapping(String serverId, int port) throws NoSuchAlgorithmException, InvalidKeyException {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(serverId.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);

            byte[] hash = mac.doFinal(String.valueOf(port).getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("hmac failed", e);
        }
    }

}
