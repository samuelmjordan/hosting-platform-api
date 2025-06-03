package com.mc_host.api.service.reconciliation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.mc_host.api.client.CloudflareClient;
import com.mc_host.api.client.CloudflareClient.DNSRecordResponse;
import com.mc_host.api.model.resource.ResourceType;
import com.mc_host.api.repository.NodeRepository;
import com.mc_host.api.util.Task;


@Service
public class ARecordReconciler implements ResourceReconciler {
    private static final Logger LOGGER = Logger.getLogger(ARecordReconciler.class.getName());

    private final CloudflareClient cloudflareClient;
    private final NodeRepository nodeRepository;

    ARecordReconciler(
        CloudflareClient cloudflareClient,
        NodeRepository nodeRepository
    ) {
        this.cloudflareClient = cloudflareClient;
        this.nodeRepository = nodeRepository;
    }

    @Override
    public ResourceType getType() {
        return ResourceType.A_RECORD;
    }

    @Override
    public void reconcile() {
        LOGGER.log(Level.INFO, String.format("Reconciling a records with db"));
        try {
            List<DnsARecordZone> actualARecords = fetchActualResources();
            List<DnsARecordZone> expectedARecords = fetchExpectedResources();
            List<DnsARecordZone> aRecordsToDestroy = actualARecords.stream()
                .filter(aRecordZone -> expectedARecords.stream().noneMatch(aRecordZone::alike))
                .toList();
            LOGGER.log(Level.INFO, String.format("Found %s a records to destroy", aRecordsToDestroy.size()));

            if (aRecordsToDestroy.size() == 0) return;

            List<CompletableFuture<Void>> deleteTasks = aRecordsToDestroy.stream()
                .map(aRecordZone -> Task.alwaysAttempt(
                    "Delete a record " + aRecordZone,
                    () -> {
                        cloudflareClient.deleteDNSRecord(aRecordZone.zoneId(), aRecordZone.aRecordId());
                    }
                )).toList();

            Task.awaitCompletion(deleteTasks);
            LOGGER.log(Level.INFO, "Executed a record reconciliation");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed a record reconciliation", e);
            throw new RuntimeException("Failed a record reconciliation", e);
        }
    }
    
    private List<DnsARecordZone> fetchActualResources() throws Exception {
        List<String> zoneIds = cloudflareClient.getAllZones();
        List<DnsARecordZone> records = new ArrayList<>();
        for (String zoneId : zoneIds) {
            List<DNSRecordResponse>  responses = cloudflareClient.getAllARecords(zoneId);
            records.addAll(responses.stream()
                .map(record -> new DnsARecordZone(
                    record.id(),
                    zoneId                       
                )).toList()
            );
        };
        return records;
    }

    private List<DnsARecordZone> fetchExpectedResources() {
        return nodeRepository.selectAllARecordIds().stream()
            .map(record -> new DnsARecordZone(
                record.aRecordId(),
                record.zoneId()
            ))
            .toList();
    }

    private record DnsARecordZone(
        String aRecordId,
        String zoneId
    ) {
        public Boolean alike(DnsARecordZone other) {
            return this.aRecordId().equals(other.aRecordId());
        }
    }
    
}
