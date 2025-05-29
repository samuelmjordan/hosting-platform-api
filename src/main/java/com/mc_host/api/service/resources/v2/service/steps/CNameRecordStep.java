package com.mc_host.api.service.resources.v2.service.steps;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mc_host.api.model.game_server.DnsCNameRecord;
import com.mc_host.api.model.node.DnsARecord;
import com.mc_host.api.repository.GameServerRepository;
import com.mc_host.api.repository.NodeRepository;
import com.mc_host.api.repository.ServerExecutionContextRepository;
import com.mc_host.api.service.resources.DnsService;
import com.mc_host.api.service.resources.v2.context.Context;
import com.mc_host.api.service.resources.v2.context.StepTransition;
import com.mc_host.api.service.resources.v2.context.StepType;
import com.mc_host.api.service.resources.v2.service.TransitionService;

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
        DnsCNameRecord dnsCNameRecord = gameServerRepository.selectDnsCNameRecord(context.getSubscriptionId())
            .orElseThrow(() -> new IllegalStateException("DNS CNAME record not found for subscription: " + context.getSubscriptionId()));

        if (context.getMode().isMigrate()) {

        } else {
            dnsService.deleteCNameRecord(dnsCNameRecord);
        }

        return transitionService.persistAndProgress(context, StepType.PTERODACTYL_SERVER);
    }

}
