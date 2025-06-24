package com.mc_host.api.controller.data;

import com.mc_host.api.model.plan.Plan;
import com.mc_host.api.model.plan.SpecificationType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("plan")
public interface PlanController {

	@GetMapping("{specType}")
	public ResponseEntity<List<Plan>> getPlansForSpecType(
		@PathVariable SpecificationType specType
	);
}
