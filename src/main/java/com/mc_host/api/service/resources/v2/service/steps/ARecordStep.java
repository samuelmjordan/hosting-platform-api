package com.mc_host.api.service.resources.v2.service.steps;

import org.springframework.stereotype.Service;

import com.mc_host.api.model.node.DnsARecord;
import com.mc_host.api.model.node.HetznerNode;
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
    public StepTransition create(Context context) {
        HetznerNode hetznerNode = nodeRepository.selectHetznerNode(context.getSubscriptionId())
            .orElseThrow(() -> new IllegalStateException("Hetzner node not found for subscription: " + context.getSubscriptionId()));
        DnsARecord dnsARecord = dnsService.createARecord(hetznerNode);
        nodeRepository.insertDnsARecord(dnsARecord);
        contextRepository.updateNewARecordId(context.getSubscriptionId(), dnsARecord.aRecordId());
        
        return transitionService.persistAndProgress(context, StepType.PTERODACTYL_NODE);
    }

    @Override
    public StepTransition destroy(Context context) {
        DnsARecord dnsARecord = nodeRepository.selectDnsARecord(context.getSubscriptionId())
            .orElseThrow(() -> new IllegalStateException("DNS A Record not found for subscription: " + context.getSubscriptionId()));
        dnsService.deleteARecord(dnsARecord);

        return transitionService.persistAndProgress(context, StepType.CLOUD_NODE);
    }

}
