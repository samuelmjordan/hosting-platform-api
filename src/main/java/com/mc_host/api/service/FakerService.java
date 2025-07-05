package com.mc_host.api.service;

import com.mc_host.api.repository.SubscriptionRepository;
import com.mc_host.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import net.datafaker.Faker;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class FakerService {
	private static final Faker FAKER = new Faker();

	private final UserRepository userRepository;
	private final SubscriptionRepository subscriptionRepository;

	public String generateUsername() {
		String username;
		do {
			username = Stream.of(
					FAKER.word().adjective(),
					FAKER.color().name(),
					FAKER.animal().name(),
					FAKER.word().verb(),
					String.valueOf(FAKER.number().numberBetween(10, 99)))
				.reduce(String::concat).get()
				.replace(" ", "")
				.toLowerCase();
		} while (userRepository.usernameExists(username));
		return username;
	}

	public String generatePassword() {
		SecureRandom random = new SecureRandom();
		String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+-=?";

		return random.ints(24, 0, chars.length())
			.mapToObj(chars::charAt)
			.map(Object::toString)
			.collect(Collectors.joining());
	}

	public String generateFirstname() {
		return FAKER.name().firstName();
	}

	public String generateLastname() {
		return FAKER.name().lastName();
	}

	public String generateSubdomain() {
		String subdomain;
		do {
			subdomain = Stream.of(
					FAKER.color().name(),
					FAKER.animal().name(),
					FAKER.word().verb(),
					String.valueOf(FAKER.number().numberBetween(10000, 99999)))
				.map(s -> s.replaceAll("\\s+", ""))
				.collect(Collectors.joining("-"))
				.toLowerCase();
		} while (
			subdomain.length() >= 28 ||
				subscriptionRepository.domainExists(subdomain));
		return subdomain;
	}

}
