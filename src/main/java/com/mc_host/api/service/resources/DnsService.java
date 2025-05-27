package com.mc_host.api.service.resources;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.mc_host.api.client.CloudflareClient;
import com.mc_host.api.client.CloudflareClient.DNSRecordResponse;
import com.mc_host.api.configuration.ApplicationConfiguration;
import com.mc_host.api.exceptions.resources.CloudflareException;
import com.mc_host.api.model.game_server.DnsCNameRecord;
import com.mc_host.api.model.node.DnsARecord;
import com.mc_host.api.model.node.HetznerNode;
import com.mc_host.api.repository.GameServerRepository;
import com.mc_host.api.repository.NodeRepository;
import com.mc_host.api.util.PersistenceContext;

@Service
public class DnsService {
    private static final Logger LOGGER = Logger.getLogger(DnsService.class.getName());

    private final ApplicationConfiguration applicationConfiguration;
    private final CloudflareClient cloudflareClient;
    private final NodeRepository nodeRepository;
    private final GameServerRepository gameServerRepository;
    private final PersistenceContext persistenceContext;

    public DnsService(
        CloudflareClient cloudflareClient,
        ApplicationConfiguration applicationConfiguration,
        NodeRepository nodeRepository,
        GameServerRepository gameServerRepository,
        PersistenceContext persistenceContext
    ) {
        this.cloudflareClient = cloudflareClient;
        this.applicationConfiguration = applicationConfiguration;
        this.nodeRepository = nodeRepository;
        this.gameServerRepository = gameServerRepository;
        this.persistenceContext = persistenceContext;
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
            throw new CloudflareException(String.format("[subscriptionId: %s] Error Creating DNS A record", hetznerNode.subscriptionId()), e);
        }
    }

    public void deleteARecord(DnsARecord dnsARecord) {
        LOGGER.log(Level.INFO, String.format("[aRecordId: %s] Deleting DNS A record", dnsARecord.aRecordId()));
        try {
            persistenceContext.inTransaction(() -> {
                nodeRepository.deleteDnsARecord(dnsARecord.aRecordId());
                cloudflareClient.deleteDNSRecord(dnsARecord.zoneId(), dnsARecord.aRecordId());
            });
            LOGGER.log(Level.INFO, String.format("[aRecordId: %s] Deleted DNS A record", dnsARecord.aRecordId()));
        } catch (Exception e) {
            throw new CloudflareException(String.format("[aRecordId: %s] Error deleting DNS A record", dnsARecord.aRecordId()), e);
        }
    }

    public void deleteARecordWithGameServerId(String nodeId) {
        DnsARecord dnsARecord = nodeRepository.selectDnsARecord(nodeId)
            .orElseThrow(() -> new IllegalStateException(String.format("[gameServerId: %s] No DNS A record associated with node", nodeId)));
        deleteARecord(dnsARecord);
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
            throw new CloudflareException(String.format("[subscriptionId: %s] Error creating DNS C NAME record", dnsARecord.subscriptionId()), e);
        }
    }

    public void deleteCNameRecord(DnsCNameRecord dnsCNameRecord) {
        LOGGER.log(Level.INFO, String.format("[cNameRecordId: %s] Deleting DNS C NAME record", dnsCNameRecord.cNameRecordId()));
        try {
            persistenceContext.inTransaction(() -> {
                gameServerRepository.deleteDnsCNameRecord(dnsCNameRecord.cNameRecordId());
                cloudflareClient.deleteDNSRecord(dnsCNameRecord.zoneId(), dnsCNameRecord.cNameRecordId());
            });
            LOGGER.log(Level.INFO, String.format("[cNameRecordId: %s] Deleted DNS C NAME record", dnsCNameRecord.cNameRecordId()));
        } catch (Exception e) {
            throw new CloudflareException(String.format("[cNameRecordId: %s] Error deleting DNS C NAME record", dnsCNameRecord.cNameRecordId()), e);
        }
    }

    public void deleteCNameRecordWithGameServerId(String gameServerId) {
        DnsCNameRecord dnsCNameRecord = gameServerRepository.selectDnsCNameRecord(gameServerId)
            .orElseThrow(() -> new IllegalStateException(String.format("[gameServerId: %s] No DNS C NAME record associated with game server", gameServerId)));
        deleteCNameRecord(dnsCNameRecord);
    }
    
}
