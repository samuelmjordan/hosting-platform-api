package com.mc_host.api.service.provisioning.steps;

import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.provisioning.StepTransition;
import com.mc_host.api.model.provisioning.StepType;
import com.mc_host.api.model.resource.dns.DnsARecord;
import com.mc_host.api.model.resource.dns.DnsCNameRecord;
import com.mc_host.api.repository.GameServerRepository;
import com.mc_host.api.repository.NodeRepository;
import com.mc_host.api.repository.ServerExecutionContextRepository;
import com.mc_host.api.service.provisioning.TransitionService;
import com.mc_host.api.service.resources.DnsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CNameRecordStep extends AbstractStep {

    private final NodeRepository nodeRepository;
    private final GameServerRepository gameServerRepository;
    private final DnsService dnsService;

    protected CNameRecordStep(
        ServerExecutionContextRepository contextRepository,
        GameServerRepository gameServerRepository,
        TransitionService transitionService,
        NodeRepository nodeRepository,
        DnsService dnsService
    ) {
        super(contextRepository, transitionService);
        this.nodeRepository = nodeRepository;
        this.gameServerRepository = gameServerRepository;
        this.dnsService = dnsService;
    }

    @Override
    public StepType getType() {
        return StepType.C_NAME_RECORD;
    }

    @Override
    @Transactional
    public StepTransition create(Context context) {
        DnsARecord dnsARecord = nodeRepository.selectDnsARecord(context.getNewARecordId())
            .orElseThrow(() -> new IllegalStateException("DNS A record not found: " + context.getNewARecordId()));
        DnsCNameRecord dnsCNameRecord = dnsService.createCNameRecord(dnsARecord, UUID.randomUUID().toString().replace("-", ""));

        Context transitionedContext = context.withNewCNameRecordId(dnsCNameRecord.cNameRecordId());
        gameServerRepository.insertDnsCNameRecord(dnsCNameRecord);

        return transitionService.persistAndProgress(transitionedContext, StepType.START_SERVER);
    }

    @Override
    @Transactional
    public StepTransition destroy(Context context) {
        DnsCNameRecord dnsCNameRecord = gameServerRepository.selectDnsCNameRecord(context.getCNameRecordId())
            .orElseThrow(() -> new IllegalStateException("DNS CNAME record not found: " + context.getCNameRecordId()));

        Context transitionedContext = context;
        if (context.getMode().isMigrate()) {
            DnsARecord dnsARecord = nodeRepository.selectDnsARecord(context.getNewARecordId())
                .orElseThrow(() -> new IllegalStateException("DNS A record not found: " + context.getNewARecordId()));
            DnsCNameRecord newDnsCNameRecord = dnsService.updateCNameRecord(dnsARecord, dnsCNameRecord);

            gameServerRepository.updateDnsCNameRecord(newDnsCNameRecord);
        } else {
            transitionedContext = context.promoteNewCNameRecordId();
            gameServerRepository.deleteDnsCNameRecord(dnsCNameRecord.cNameRecordId());
            
            dnsService.deleteCNameRecord(dnsCNameRecord);
        }

        return transitionService.persistAndProgress(transitionedContext, StepType.PTERODACTYL_SERVER);
    }

}
