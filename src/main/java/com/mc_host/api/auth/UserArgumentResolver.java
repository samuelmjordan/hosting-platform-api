package com.mc_host.api.auth;

import com.mc_host.api.model.user.ApplicationUser;
import com.mc_host.api.repository.UserRepository;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.server.ResponseStatusException;

@Component
public class UserArgumentResolver extends AbstractAuthResolver<CurrentUser, ApplicationUser> {

	private final UserRepository userRepository;

	public UserArgumentResolver(UserRepository userRepository) {
		super(CurrentUser.class, ApplicationUser.class);
		this.userRepository = userRepository;
	}

	@Override
	protected String doResolveId(
		MethodParameter parameter,
		NativeWebRequest webRequest
	) {
		return getCurrentUserId();
	}

	@Override
	protected ApplicationUser doResolveEntity(
		MethodParameter parameter,
		NativeWebRequest webRequest
	) {
		String userId = getCurrentUserId();
		return userRepository.selectUser(userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
				"User %s not found".formatted(userId)));
	}
}