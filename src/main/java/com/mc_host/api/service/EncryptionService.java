package com.mc_host.api.service;

import com.mc_host.api.configuration.PterodactylConfiguration;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class EncryptionService {
	private static final String ALGORITHM = "AES";
	private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

	private final SecretKey secretKey;

	public EncryptionService(PterodactylConfiguration pterodactylConfiguration) {
		byte[] decodedKey = Base64.getDecoder().decode(pterodactylConfiguration.getPasswordKey());
		this.secretKey = new SecretKeySpec(decodedKey, ALGORITHM);
	}

	public String encrypt(String plaintext) {
		try {
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.ENCRYPT_MODE, secretKey);
			byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(encrypted);
		} catch (Exception e) {
			throw new RuntimeException("encryption failed: " + e.getMessage(), e);
		}
	}

	public String decrypt(String encryptedText) {
		try {
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.DECRYPT_MODE, secretKey);
			byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
			return new String(decrypted, StandardCharsets.UTF_8);
		} catch (Exception e) {
			throw new RuntimeException("decryption failed: " + e.getMessage(), e);
		}
	}
}