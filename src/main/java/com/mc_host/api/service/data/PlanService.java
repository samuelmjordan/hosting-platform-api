package com.mc_host.api.service.data;

import com.mc_host.api.controller.data.PlanController;
import com.mc_host.api.model.plan.Plan;
import com.mc_host.api.model.plan.SpecificationType;
import com.mc_host.api.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
@RequiredArgsConstructor
public class PlanService implements PlanController {
	private static final Logger LOGGER = Logger.getLogger(PlanService.class.getName());

	private final PlanRepository planRepository;

	@Override
	public ResponseEntity<List<Plan>> getPlansForSpecType(SpecificationType specType) {
		LOGGER.log(Level.INFO, String.format(String.format("Fetching plans for specType %s", specType)));

		try {
			List<Plan> plans;
			switch(specType)  {
				case SpecificationType.GAME_SERVER:
					plans = planRepository.selectJavaServerPlans();
					break;
				default:
					throw new IllegalStateException(String.format("specType %s is unhandled", specType));
			}

			if (plans.isEmpty()) {
				throw new RuntimeException(String.format("specType %s had no plans. Is this the correct Id?", specType));
			}

			return ResponseEntity.ok(plans);
		} catch (RuntimeException e) {
			LOGGER.log(Level.SEVERE, String.format("Failed to fetch plans for specType %s", specType), e);
			return ResponseEntity.internalServerError().build();
		}
	}
}
