package com.mc_host.api.service.resources.v2.service.steps;

import com.mc_host.api.repository.NodeRepository;
import com.mc_host.api.repository.ServerExecutionContextRepository;
import com.mc_host.api.service.resources.DnsService;
import com.mc_host.api.service.resources.v2.context.Context;
import com.mc_host.api.service.resources.v2.context.StepTransition;
import com.mc_host.api.service.resources.v2.context.StepType;

public class ARecordStep extends AbstractStep {

    private final NodeRepository nodeRepository;
    private final DnsService dnsService;

    protected ARecordStep(
        ServerExecutionContextRepository contextRepository,
        NodeRepository nodeRepository,
        DnsService dnsService
    ) {
        super(contextRepository);
        this.nodeRepository = nodeRepository;
        this.dnsService = dnsService;
    }

    @Override
    public StepType getType() {
        return StepType.A_RECORD;
    }

    @Override
    public StepTransition create(Context context) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'create'");
    }

    @Override
    public StepTransition destroy(Context context) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'destroy'");
    }

    @Override
    public StepTransition migrate(Context context) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'migrate'");
    }

    @Override
    public StepTransition update(Context context) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'update'");
    }

}
