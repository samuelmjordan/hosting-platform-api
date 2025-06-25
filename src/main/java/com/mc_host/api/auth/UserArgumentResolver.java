package com.mc_host.api.auth;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.NativeWebRequest;

@Component
public class UserArgumentResolver extends AbstractAuthResolver<CurrentUser> {

	public UserArgumentResolver() {
		super(CurrentUser.class);
	}

	@Override
	protected String doResolve(MethodParameter parameter, NativeWebRequest webRequest) {
		return getCurrentUserId();
	}
}