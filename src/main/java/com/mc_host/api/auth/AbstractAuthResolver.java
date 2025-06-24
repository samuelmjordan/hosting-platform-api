package com.mc_host.api.auth;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerMapping;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Set;

public abstract class AbstractAuthResolver<T extends Annotation, E> implements HandlerMethodArgumentResolver {

	private final Class<T> annotationType;
	private final Set<Class<?>> supportedTypes;

	protected AbstractAuthResolver(
		Class<T> annotationType,
		Class<E> entityType
	) {
		this.annotationType = annotationType;
		this.supportedTypes = Set.of(String.class, entityType);
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(annotationType)
			&& supportedTypes.contains(parameter.getParameterType());
	}

	@Override
	public Object resolveArgument(
		MethodParameter parameter,
		ModelAndViewContainer mavContainer,
		NativeWebRequest webRequest,
		WebDataBinderFactory binderFactory
	) {
		if (parameter.getParameterType().equals(String.class)) {
			return doResolveId(parameter, webRequest);
		} else {
			return doResolveEntity(parameter, webRequest);
		}
	}

	protected abstract String doResolveId(MethodParameter parameter, NativeWebRequest webRequest);
	protected abstract E doResolveEntity(MethodParameter parameter, NativeWebRequest webRequest);

	protected String getCurrentUserId() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
			return jwt.getSubject();
		}
		throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "no authenticated user");
	}

	protected String getPathVariable(
		NativeWebRequest request,
		String variableName
	) {
		Map<String, String> pathVariables = (Map<String, String>)
			request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);

		return pathVariables != null ? pathVariables.get(variableName) : null;
	}

	protected void validateOwnership(
		String userCustomerId,
		String resourceCustomerId,
		String userId,
		String resourceId,
		String resourceType
	) {
		if (!userCustomerId.equals(resourceCustomerId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN,
				"User %s not authorised for %s %s".formatted(userId, resourceType, resourceId));
		}
	}
}