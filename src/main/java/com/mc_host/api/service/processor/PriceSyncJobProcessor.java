package com.mc_host.api.service.processor;

import com.mc_host.api.model.plan.AcceptedCurrency;
import com.mc_host.api.model.plan.ContentPrice;
import com.mc_host.api.model.plan.PricePair;
import com.mc_host.api.model.queue.Job;
import com.mc_host.api.model.queue.JobType;
import com.mc_host.api.queue.processor.JobProcessor;
import com.mc_host.api.repository.PriceRepository;
import com.mc_host.api.util.Task;
import com.stripe.exception.StripeException;
import com.stripe.model.Price;
import com.stripe.param.PriceListParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
@RequiredArgsConstructor
public class PriceSyncJobProcessor implements JobProcessor {
	private static final Logger LOGGER = Logger.getLogger(PriceSyncJobProcessor.class.getName());

	private final PriceRepository priceRepository;

	@Override
	public JobType getJobType() {
		return JobType.PRODUCT_PRICE_SYNC;
	}

	@Override
	public void process(Job job) throws Exception {
		LOGGER.info("Processing %s job: %s".formatted(getJobType(), job.jobId()));
		process(job.payload());
		LOGGER.info("%s job completed for: %s".formatted(getJobType(), job.jobId()));
	}

	public void process(String productId) {
		try {
			PriceListParams priceListParams = PriceListParams.builder()
				.setProduct(productId)
				.build();
			List<ContentPrice> stripePrices = Price.list(priceListParams).getData().stream()
				.map(price -> stripePriceToEntity(price, productId))
				.toList();
			List<ContentPrice> dbPrices = priceRepository.selectPricesByProductId(productId);

			List<ContentPrice> pricesToDelete = dbPrices.stream()
				.filter(dbPrice -> stripePrices.stream().noneMatch(dbPrice::isAlike))
				.toList();
			List<ContentPrice> pricesToCreate = stripePrices.stream()
				.filter(stripePrice -> dbPrices.stream().noneMatch(stripePrice::isAlike))
				.toList();
			List<PricePair> pricesToUpdate = dbPrices.stream()
				.flatMap(dbPrice -> stripePrices.stream()
					.filter(dbPrice::isAlike)
					.map(stripeSubscription -> new PricePair(dbPrice, stripeSubscription)))
				.toList();

			List<CompletableFuture<Void>> deleteTasks = pricesToDelete.stream()
				.map(price -> Task.alwaysAttempt(
					"Delete price " + price.priceId(),
					() -> priceRepository.deleteProductPrice(price.priceId(), productId)
				)).toList();

			List<CompletableFuture<Void>> createTasks = pricesToCreate.stream()
				.map(price -> Task.alwaysAttempt(
					"Create price " + price.priceId(),
					() -> priceRepository.insertPrice(price)
				)).toList();

			List<CompletableFuture<Void>> updateTasks = pricesToUpdate.stream()
				.map(pricePair -> Task.alwaysAttempt(
					"Update price " + pricePair.getOldPrice().priceId(),
					() -> priceRepository.insertPrice(pricePair.getNewPrice())
				)).toList();

			List<CompletableFuture<Void>> allTasks = new ArrayList<>();
			allTasks.addAll(deleteTasks);
			allTasks.addAll(createTasks);
			allTasks.addAll(updateTasks);
			Task.awaitCompletion(allTasks);

			LOGGER.log(Level.INFO, "Executed price db sync for product: " + productId);
		} catch (StripeException e) {
			LOGGER.log(Level.SEVERE, "Failed to sync price data for product: " + productId, e);
			throw new RuntimeException("Failed to sync subscription data", e);
		}
	}

	private ContentPrice stripePriceToEntity(Price price, String productId) {
		return new ContentPrice(
			price.getId(),
			productId,
			price.getActive(),
			AcceptedCurrency.fromCode(price.getCurrency()),
			price.getUnitAmount()
		);
	}
}
