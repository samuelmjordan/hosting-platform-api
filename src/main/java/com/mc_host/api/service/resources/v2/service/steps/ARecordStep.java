package com.mc_host.api.service.resources.v2.service.steps;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mc_host.api.model.resource.DnsARecord;
import com.mc_host.api.model.resource.HetznerNode;
import com.mc_host.api.repository.NodeRepository;
import com.mc_host.api.repository.ServerExecutionContextRepository;
import com.mc_host.api.service.resources.DnsService;
import com.mc_host.api.service.resources.v2.context.Context;
import com.mc_host.api.service.resources.v2.context.StepTransition;
import com.mc_host.api.service.resources.v2.context.StepType;
import com.mc_host.api.service.resources.v2.service.TransitionService;

@Service
public class ARecordStep extends AbstractStep {

    private final NodeRepository nodeRepository;
    private final DnsService dnsService;

    protected ARecordStep(
        ServerExecutionContextRepository contextRepository,
        TransitionService transitionService,
        NodeRepository nodeRepository,
        DnsService dnsService
    ) {
        super(contextRepository, transitionService);
        this.nodeRepository = nodeRepository;
        this.dnsService = dnsService;
    }

    @Override
    public StepType getType() {
        return StepType.A_RECORD;
    }

    @Override
    @Transactional
    public StepTransition create(Context context) {
        HetznerNode hetznerNode = nodeRepository.selectHetznerNode(context.getNewNodeId())
            .orElseThrow(() -> new IllegalStateException("Hetzner node not found: " + context.getNewNodeId()));
        DnsARecord dnsARecord = dnsService.createARecord(hetznerNode);

        Context transitionedContext = context.withNewARecordId(dnsARecord.aRecordId());
        nodeRepository.insertDnsARecord(dnsARecord);
        
        return transitionService.persistAndProgress(transitionedContext, StepType.PTERODACTYL_NODE);
    }

    @Override
    @Transactional
    public StepTransition destroy(Context context) {
        DnsARecord dnsARecord = nodeRepository.selectDnsARecord(context.getARecordId())
            .orElseThrow(() -> new IllegalStateException("DNS A Record not found: " + context.getARecordId()));
        Context transitionedContext = context.promoteNewARecordId();
        nodeRepository.deleteDnsARecord(dnsARecord.aRecordId());


        dnsService.deleteARecord(dnsARecord);

        return transitionService.persistAndProgress(transitionedContext, StepType.CLOUD_NODE);
    }

}
