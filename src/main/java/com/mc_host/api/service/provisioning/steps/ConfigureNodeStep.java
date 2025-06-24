package com.mc_host.api.service.provisioning.steps;

import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.provisioning.StepTransition;
import com.mc_host.api.model.provisioning.StepType;
import com.mc_host.api.model.resource.dns.DnsARecord;
import com.mc_host.api.model.resource.pterodactyl.PterodactylNode;
import com.mc_host.api.repository.NodeRepository;
import com.mc_host.api.service.resources.PterodactylService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConfigureNodeStep extends AbstractStep {

    private final NodeRepository nodeRepository;
    private final PterodactylService pterodactylService;

    @Override
    public StepType getType() {
        return StepType.CONFIGURE_NODE;
    }

    @Override
    public StepTransition create(Context context) {
        PterodactylNode pterodactylNode = nodeRepository.selectPterodactylNode(context.getNewPterodactylNodeId())
            .orElseThrow(() -> new IllegalStateException("Pterodactyl node not found: " + context.getNewPterodactylNodeId()));
        DnsARecord dnsARecord = nodeRepository.selectDnsARecord(context.getNewARecordId())
            .orElseThrow(() -> new IllegalStateException("DNS A Record not found: " + context.getNewARecordId()));
        pterodactylService.configureNode(pterodactylNode.pterodactylNodeId(), dnsARecord);

        return transitionService.persistAndProgress(context, StepType.PTERODACTYL_ALLOCATION);
    }

    @Override
    public StepTransition destroy(Context context) {
        throw new UnsupportedOperationException("Node configuration step cannot be destroyed directly. Try destroying the Pterodactyl Node step instead.");
    }

}
