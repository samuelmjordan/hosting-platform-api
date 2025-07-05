package com.mc_host.api.service.processor;

import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.queue.Job;
import com.mc_host.api.model.queue.JobType;
import com.mc_host.api.model.resource.dns.DnsCNameRecord;
import com.mc_host.api.model.subscription.ContentSubscription;
import com.mc_host.api.queue.JobScheduler;
import com.mc_host.api.queue.processor.JobProcessor;
import com.mc_host.api.repository.GameServerRepository;
import com.mc_host.api.repository.ServerExecutionContextRepository;
import com.mc_host.api.repository.SubscriptionRepository;
import com.mc_host.api.service.resources.DnsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.logging.Logger;

@Service
@RequiredArgsConstructor
public class UserSubdomainUpdateJobProcessor implements JobProcessor {
	private static final Logger LOGGER = Logger.getLogger(UserSubdomainUpdateJobProcessor.class.getName());

	private final DnsService dnsService;
	private final SubscriptionRepository subscriptionRepository;
	private final ServerExecutionContextRepository serverExecutionContextRepository;
	private final GameServerRepository gameServerRepository;
	private final JobScheduler jobScheduler;

	@Override
	public JobType getJobType() {
		return JobType.SUBSCRIPTION_SUBDOMAIN_UPDATE;
	}

	@Override
	public void process(Job job) throws Exception {
		LOGGER.info("Processing %s job: %s".formatted(getJobType(), job.jobId()));
		process(job.payload());
		LOGGER.info("%s job completed for: %s".formatted(getJobType(), job.jobId()));
	}

	private void process(String subscriptionId) {
		Context context = serverExecutionContextRepository.selectSubscription(subscriptionId)
			.orElseThrow(() -> new IllegalStateException("No context found for subscription " + subscriptionId));
		ContentSubscription subscription = subscriptionRepository.selectSubscription(subscriptionId)
			.orElseThrow(() -> new IllegalStateException("No subscription found " + subscriptionId));

		DnsCNameRecord dnsCNameRecord = gameServerRepository.selectDnsCNameRecord(context.getCNameRecordId())
			.orElseThrow(() -> new IllegalStateException("DNS CNAME record not found: " + context.getCNameRecordId()));
		DnsCNameRecord newDnsCNameRecord = dnsService.updateCNameRecordName(dnsCNameRecord, subscription.subdomain());
		gameServerRepository.updateDnsCNameRecord(newDnsCNameRecord);

		jobScheduler.scheduleCustomerSubscriptionSync(subscription.customerId());
	}
}
