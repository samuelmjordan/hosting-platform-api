package com.mc_host.api.service.provisioning.steps;

import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.provisioning.StepTransition;
import com.mc_host.api.model.provisioning.StepType;
import com.mc_host.api.model.resource.dns.DnsARecord;
import com.mc_host.api.model.resource.hetzner.node.HetznerCloudNode;
import com.mc_host.api.model.resource.hetzner.node.HetznerNode;
import com.mc_host.api.repository.NodeAccessoryRepository;
import com.mc_host.api.repository.NodeRepository;
import com.mc_host.api.service.resources.DnsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NodeARecordStep extends AbstractStep {

    private final NodeRepository nodeRepository;
    private final NodeAccessoryRepository nodeAccessoryRepository;
    private final DnsService dnsService;

    @Override
    public StepType getType() {
        return StepType.NODE_A_RECORD;
    }

    @Override
    @Transactional
    public StepTransition create(Context context) {
        //Skip for dedicated resources
        Boolean dedicated = nodeRepository.selectHetznerNode(context.getNewNodeId())
            .map(HetznerNode::dedicated)
            .orElseThrow(() -> new IllegalStateException(String.format("Node %s not found", context.getNewNodeId())));
        if (dedicated) {
            LOGGER.warning("%s step is illegal for dedicated resources. Skipping. subId: %s".formatted(getType(), context.getSubscriptionId()));
            return transitionService.persistAndProgress(context, StepType.ASSIGN_PTERODACTYL_ALLOCATION);
        }

        HetznerCloudNode hetznerCloudNode = nodeRepository.selectHetznerCloudNode(context.getNewNodeId())
            .orElseThrow(() -> new IllegalStateException("Hetzner node not found: " + context.getNewNodeId()));
        DnsARecord dnsARecord = dnsService.createARecord(hetznerCloudNode, context.getSubscriptionId());

        Context transitionedContext = context.withNewARecordId(dnsARecord.aRecordId());
        nodeAccessoryRepository.insertDnsARecord(dnsARecord);
        
        return transitionService.persistAndProgress(transitionedContext, StepType.PTERODACTYL_NODE);
    }

    @Override
    @Transactional
    public StepTransition destroy(Context context) {
        //Skip for dedicated resources
        Boolean dedicated = nodeRepository.selectHetznerNode(context.getNodeId())
            .map(HetznerNode::dedicated)
            .orElseThrow(() -> new IllegalStateException(String.format("Node %s not found", context.getNodeId())));
        if (dedicated) {
            LOGGER.warning("%s step is illegal for dedicated resources. Skipping. subId: %s".formatted(getType(), context.getSubscriptionId()));
            return transitionService.persistAndProgress(context, StepType.TRY_ALLOCATE_DEDICATED_NODE);
        }

        DnsARecord dnsARecord = nodeAccessoryRepository.selectDnsARecord(context.getARecordId())
            .orElseThrow(() -> new IllegalStateException("DNS A Record not found: " + context.getARecordId()));
        Context transitionedContext = context.promoteNewARecordId();
        nodeAccessoryRepository.deleteDnsARecord(dnsARecord.aRecordId());


        dnsService.deleteARecord(dnsARecord);

        return transitionService.persistAndProgress(transitionedContext, StepType.ALLOCATE_CLOUD_NODE);
    }

}
