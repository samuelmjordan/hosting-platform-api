package com.mc_host.api.service.resources;

import com.mc_host.api.client.CloudflareClient;
import com.mc_host.api.client.CloudflareClient.DNSRecordResponse;
import com.mc_host.api.configuration.ApplicationConfiguration;
import com.mc_host.api.model.resource.dns.DnsARecord;
import com.mc_host.api.model.resource.dns.DnsCNameRecord;
import com.mc_host.api.model.resource.hetzner.HetznerNode;
import com.mc_host.api.repository.GameServerRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class DnsService {
    private static final Logger LOGGER = Logger.getLogger(DnsService.class.getName());

    private final ApplicationConfiguration applicationConfiguration;
    private final CloudflareClient cloudflareClient;
    private final GameServerRepository gameServerRepository;
    public DnsService(
        CloudflareClient cloudflareClient,
        ApplicationConfiguration applicationConfiguration,
        GameServerRepository gameServerRepository
    ) {
        this.cloudflareClient = cloudflareClient;
        this.applicationConfiguration = applicationConfiguration;
        this.gameServerRepository = gameServerRepository;
    }

    public DnsARecord createARecord(HetznerNode hetznerNode) {
        LOGGER.log(Level.INFO, String.format("[subscriptionId: %s] Creating DNS A record", hetznerNode.subscriptionId()));
        try {
            String zoneId = cloudflareClient.getZoneId(applicationConfiguration.getDomain());
            String recordName = UUID.randomUUID().toString().replace("-", "");
            DNSRecordResponse dnsARecordResponse = cloudflareClient.createARecord(zoneId, recordName, hetznerNode.ipv4(), false);
            DnsARecord dnsARecord = new DnsARecord(
                hetznerNode.subscriptionId(), 
                dnsARecordResponse.id(), 
                zoneId, 
                applicationConfiguration.getDomain(), 
                dnsARecordResponse.name(), 
                dnsARecordResponse.content()
            );
            LOGGER.log(Level.INFO, String.format("[subscriptionId: %s] Created DNS A record", hetznerNode.subscriptionId()));
            return dnsARecord;
        } catch (Exception e) {
            throw new RuntimeException(String.format("[subscriptionId: %s] Error Creating DNS A record", hetznerNode.subscriptionId()), e);
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

    public DnsCNameRecord updateCNameRecord(DnsARecord dnsARecord, DnsCNameRecord dnsCNameRecord) {
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

    public void deleteCNameRecordWithGameServerId(String gameServerId) {
        DnsCNameRecord dnsCNameRecord = gameServerRepository.selectDnsCNameRecord(gameServerId)
            .orElseThrow(() -> new IllegalStateException(String.format("[gameServerId: %s] No DNS C NAME record associated with game server", gameServerId)));
        deleteCNameRecord(dnsCNameRecord);
    }
    
}
