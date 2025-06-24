package com.mc_host.api.service.provisioning.steps;

import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.provisioning.StepTransition;
import com.mc_host.api.model.provisioning.StepType;
import com.mc_host.api.model.resource.dns.DnsARecord;
import com.mc_host.api.model.resource.hetzner.HetznerNode;
import com.mc_host.api.repository.NodeRepository;
import com.mc_host.api.service.resources.DnsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ARecordStep extends AbstractStep {

    private final NodeRepository nodeRepository;
    private final DnsService dnsService;

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
