package com.mc_host.api.service.provisioning.steps;

import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.provisioning.StepTransition;
import com.mc_host.api.model.provisioning.StepType;
import com.mc_host.api.model.resource.dns.DnsARecord;
import com.mc_host.api.model.resource.dns.DnsCNameRecord;
import com.mc_host.api.repository.GameServerRepository;
import com.mc_host.api.repository.NodeAccessoryRepository;
import com.mc_host.api.service.resources.DnsService;
import lombok.RequiredArgsConstructor;
import net.datafaker.Faker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class CNameRecordStep extends AbstractStep {
    private static final Faker FAKER = new Faker();

    private final NodeAccessoryRepository nodeAccessoryRepository;
    private final GameServerRepository gameServerRepository;
    private final DnsService dnsService;

    @Override
    public StepType getType() {
        return StepType.C_NAME_RECORD;
    }

    @Override
    @Transactional
    public StepTransition create(Context context) {
        //Skip for migrations
        if (context.getMode().isMigrate()) {
            LOGGER.warning("%s step is illegal for migrating. Skipping. subId: %s".formatted(getType(), context.getSubscriptionId()));
            return transitionService.persistAndProgress(context, StepType.SYNC_NODE_ROUTE);
        }

        DnsARecord dnsARecord = nodeAccessoryRepository.selectDnsARecord(context.getNewARecordId())
            .orElseThrow(() -> new IllegalStateException("DNS A record not found: " + context.getNewARecordId()));
        DnsCNameRecord dnsCNameRecord = dnsService.createCNameRecord(dnsARecord, generateDomain());

        Context transitionedContext = context.withNewCNameRecordId(dnsCNameRecord.cNameRecordId());
        gameServerRepository.insertDnsCNameRecord(dnsCNameRecord);

        //TODO if dedicated
        if (false) {
            return transitionService.persistAndProgress(transitionedContext, StepType.SYNC_NODE_ROUTE);
        }

        return transitionService.persistAndProgress(transitionedContext, StepType.FINALISE);
    }

    @Override
    @Transactional
    public StepTransition destroy(Context context) {
        DnsCNameRecord dnsCNameRecord = gameServerRepository.selectDnsCNameRecord(context.getCNameRecordId())
            .orElseThrow(() -> new IllegalStateException("DNS CNAME record not found: " + context.getCNameRecordId()));

        //Redirect cname record for migrations
        //Destroy for non migrations
        Context transitionedContext = context;
        if (context.getMode().isMigrate()) {
            DnsARecord dnsARecord = nodeAccessoryRepository.selectDnsARecord(context.getNewARecordId())
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

    private String generateDomain() {
        String subdomain;
        do {
            subdomain = Stream.of(
                    FAKER.word().adjective(),
                    FAKER.color().name(),
                    FAKER.animal().name(),
                    FAKER.word().verb(),
                    String.valueOf(FAKER.number().numberBetween(1000, 9999)))
                .map(s -> s.replaceAll("\\s+", ""))
                .collect(Collectors.joining("-"))
                .toLowerCase();
        } while (gameServerRepository.domainExists(subdomain));
        return subdomain;
    }

}
