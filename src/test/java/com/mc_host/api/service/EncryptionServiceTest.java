package com.mc_host.api.service;

import com.mc_host.api.configuration.PterodactylConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EncryptionServiceTest {

    @Mock
    private PterodactylConfiguration pterodactylConfiguration;

    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        when(pterodactylConfiguration.getPasswordKey())
            .thenReturn("MTIzNDU2NzgxMjM0NTY3ODEyMzQ1Njc4MTIzNDU2Nzg=");
        encryptionService = new EncryptionService(pterodactylConfiguration);
    }

    @Test
    void encrypt_shouldEncryptPlaintext() {
        String plaintext = "test-password-123";

        String encrypted = encryptionService.encrypt(plaintext);

        assertThat(encrypted).isNotNull();
        assertThat(encrypted).isNotEqualTo(plaintext);
        assertThat(encrypted).isBase64();
    }

    @Test
    void decrypt_shouldDecryptEncryptedText() {
        String plaintext = "test-password-123";
        String encrypted = encryptionService.encrypt(plaintext);

        String decrypted = encryptionService.decrypt(encrypted);

        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    void encryptDecryptRoundTrip_shouldMaintainOriginalValue() {
        String originalText = "complex-password!@#$%^&*()";

        String encrypted = encryptionService.encrypt(originalText);
        String decrypted = encryptionService.decrypt(encrypted);

        assertThat(decrypted).isEqualTo(originalText);
    }

    @Test
    void encrypt_shouldProduceSameOutputForSameInput() {
        String plaintext = "test-password";

        String encrypted1 = encryptionService.encrypt(plaintext);
        String encrypted2 = encryptionService.encrypt(plaintext);

        assertThat(encrypted1).isEqualTo(encrypted2);
    }

    @Test
    void decrypt_shouldThrowExceptionForInvalidInput() {
        String invalidEncryptedText = "invalid-base64";

        assertThatThrownBy(() -> encryptionService.decrypt(invalidEncryptedText))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("decryption failed");
    }

    @Test
    void encrypt_shouldHandleEmptyString() {
        String emptyString = "";

        String encrypted = encryptionService.encrypt(emptyString);
        String decrypted = encryptionService.decrypt(encrypted);

        assertThat(decrypted).isEmpty();
    }

    @Test
    void encrypt_shouldHandleUnicodeCharacters() {
        String unicodeText = "测试密码🔐";

        String encrypted = encryptionService.encrypt(unicodeText);
        String decrypted = encryptionService.decrypt(encrypted);

        assertThat(decrypted).isEqualTo(unicodeText);
    }
}