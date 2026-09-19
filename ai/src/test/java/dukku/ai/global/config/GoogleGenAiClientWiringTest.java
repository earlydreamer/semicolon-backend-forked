package dukku.ai.global.config;

import dukku.ai.app.service.GeminiEmbeddingService;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiConnectionProperties;
import org.junit.jupiter.api.Test;
import org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiChatAutoConfiguration;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.context.annotation.Bean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class GoogleGenAiClientWiringTest {

    private static final String SYNTHETIC_API_KEY_MARKER = "synthetic-gemini-client-wiring-marker";

    @Test
    void directClientConfigurationUsesTheSameSyntheticKeyAsTheSdkBuilder() throws Exception {
        GoogleGenAiConnectionProperties properties = new GoogleGenAiConnectionProperties();
        properties.setApiKey(SYNTHETIC_API_KEY_MARKER);

        try (Client configuredClient = new GoogleGenAiClientConfig().googleGenAiClient(properties);
                Client sdkClient = Client.builder()
                        .apiKey(SYNTHETIC_API_KEY_MARKER)
                        .vertexAI(false)
                        .httpOptions(HttpOptions.builder().timeout(60_000).build())
                        .build()) {
            assertEquals(SYNTHETIC_API_KEY_MARKER, configuredClient.apiKey());
            assertEquals(sdkClient.apiKey(), configuredClient.apiKey());
            assertFalse(configuredClient.vertexAI());
        }
    }

    @Test
    void applicationClientConfigWinsAndBindsSyntheticMarkerAlongsideSpringAiAutoConfiguration() {
        ApplicationContextRunner runner = runner()
                .withUserConfiguration(GoogleGenAiClientConfig.class);

        assertSelectedClient(runner, GoogleGenAiClientConfig.class, "googleGenAiClient");
    }

    @Test
    void springAiAutoConfigurationBindsSyntheticMarkerWhenApplicationClientConfigIsAbsent() {
        assertSelectedClient(runner(), GoogleGenAiChatAutoConfiguration.class, "googleGenAiClient");
    }

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        PropertyPlaceholderAutoConfiguration.class,
                        GoogleGenAiChatAutoConfiguration.class))
                .withUserConfiguration(ToolCallingManagerTestConfig.class)
                .withPropertyValues(
                        "spring.ai.model.chat=google-genai",
                        "probe.gemini-key=" + SYNTHETIC_API_KEY_MARKER,
                        "spring.ai.google.genai.api-key=${probe.gemini-key}",
                        "spring.ai.google.genai.vertex-ai=false",
                        "spring.ai.google.genai.chat.model=gemini-3.5-flash-lite",
                        "ai.embedding.model=gemini-embedding-001",
                        "ai.embedding.dimensions=1536");
    }

    private void assertSelectedClient(
            ApplicationContextRunner runner,
            Class<?> expectedFactoryType,
            String expectedBeanName) {
        runner.run(context -> {
            assertTrue(context.getStartupFailure() == null,
                    "Spring synthetic-key context failed: " + safeFailureChain(context.getStartupFailure()));
            assertTrue(SYNTHETIC_API_KEY_MARKER.equals(
                            context.getEnvironment().getProperty("spring.ai.google.genai.api-key")),
                    "Spring Environment should hold the configured synthetic marker");
            assertEquals(1, context.getBeansOfType(Client.class).size(),
                    "exactly one Google GenAI Client bean should be selected");

            var clientDefinition = context.getBeanFactory().getBeanDefinition(expectedBeanName);
            String factoryBeanName = clientDefinition.getFactoryBeanName();
            assertTrue(factoryBeanName != null, "Client should come from the expected @Bean method");
            assertTrue(expectedFactoryType.isInstance(context.getBean(factoryBeanName)),
                    "the expected Client bean factory should be selected");

            Client client = context.getBean(Client.class);
            assertTrue(SYNTHETIC_API_KEY_MARKER.equals(client.apiKey()),
                    "Client should hold the configured synthetic marker");
            assertFalse(client.vertexAI(), "Client should use Gemini Developer API mode");
            assertTrue(context.containsBean("googleGenAiChatModel"),
                    "Spring AI chat auto-configuration should consume the selected Client");

            GoogleGenAiChatModel chatModel = context.getBean(GoogleGenAiChatModel.class);
            GeminiEmbeddingService embeddingService = context.getBean(GeminiEmbeddingService.class);
            assertSame(client, ReflectionTestUtils.getField(chatModel, "genAiClient"),
                    "chat model should consume the selected Client instance");
            assertSame(client, ReflectionTestUtils.getField(embeddingService, "client"),
                    "embedding service should consume the selected Client instance");
            assertTrue(SYNTHETIC_API_KEY_MARKER.equals(
                            ((Client) ReflectionTestUtils.getField(chatModel, "genAiClient")).apiKey())
                            && SYNTHETIC_API_KEY_MARKER.equals(
                                    ((Client) ReflectionTestUtils.getField(embeddingService, "client")).apiKey()),
                    "chat and embedding should use the same configured synthetic API key");
        });
    }

    private static String safeFailureChain(Throwable failure) {
        if (failure == null) {
            return "none";
        }
        StringBuilder chain = new StringBuilder();
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        Throwable current = failure;
        while (current != null && seen.add(current)) {
            if (!chain.isEmpty()) {
                chain.append(" -> ");
            }
            chain.append(current.getClass().getName());
            current = current.getCause();
        }
        return chain.toString();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ToolCallingManagerTestConfig {

        @Bean
        ToolCallingManager toolCallingManager() {
            return mock(ToolCallingManager.class);
        }

        @Bean
        GeminiEmbeddingService geminiEmbeddingService(Client client) {
            return new GeminiEmbeddingService(client, "gemini-embedding-001", 1536);
        }
    }

}
