package dukku.ai.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.util.Collections;

import com.google.genai.Client;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

class GeminiEmbeddingServiceTest {

    @Test
    void aiRootPropertiesOverrideModelAndGenerateMatchingProfile() {
        new ApplicationContextRunner()
                .withUserConfiguration(EmbeddingConfiguration.class)
                .withBean(Client.class, () -> mock(Client.class))
                .withPropertyValues(
                        "ai.embedding.model=alternate-test-model",
                        "ai.embedding.dimensions=1536")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    GeminiEmbeddingService service = context.getBean(GeminiEmbeddingService.class);
                    assertThat(service.model()).isEqualTo("alternate-test-model");
                    assertThat(service.profile()).isEqualTo(
                            "alternate-test-model:1536:retrieval-document:normalization-v1");
                });
    }

    @Test
    void normalizeRejectsWrongDimensionNonFiniteAndZeroVectors() {
        assertThatThrownBy(() -> GeminiEmbeddingService.normalize(Collections.nCopies(1535, 1.0f), 1536))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("dimension 1535");
        assertThatThrownBy(() -> GeminiEmbeddingService.normalize(Collections.nCopies(1536, Float.NaN), 1536))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("non-finite");
        assertThatThrownBy(() -> GeminiEmbeddingService.normalize(Collections.nCopies(1536, 0.0f), 1536))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("zero");
    }

    @Test
    void profileMustFitDatabaseColumn() {
        assertThatThrownBy(() -> GeminiEmbeddingService.profileFor("m".repeat(150), 1536))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("160 characters");
    }

    @Configuration(proxyBeanMethods = false)
    @Import(GeminiEmbeddingService.class)
    static class EmbeddingConfiguration {
    }
}
