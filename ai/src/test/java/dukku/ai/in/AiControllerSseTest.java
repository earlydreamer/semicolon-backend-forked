package dukku.ai.in;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import dukku.ai.app.AiFacade;
import dukku.common.shared.ai.dto.ChatRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiControllerSseTest {

    private static final List<String> CHUNKS = List.of(
            "hello",
            " world",
            "\n  indented",
            "한글",
            " plain text",
            "   ",
            "\n\n",
            "끝\n");
    private static final String REQUEST_BODY = """
            {"conversationId":"sse-whitespace-test","userUuid":"20000000-0000-0000-0000-000000000001","message":"synthetic"}
            """;

    @Test
    void httpSseSerializationPreservesConcatenatedFluxTextExactly() throws Exception {
        AiFacade aiFacade = mock(AiFacade.class);
        when(aiFacade.chat(any(ChatRequest.class))).thenReturn(Flux.just(CHUNKS.toArray(String[]::new)));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AiController(aiFacade))
                .setMessageConverters(
                        new StringHttpMessageConverter(StandardCharsets.UTF_8),
                        new JacksonJsonHttpMessageConverter())
                .build();

        MvcResult initialResult = mockMvc.perform(post("/api/v1/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content(REQUEST_BODY))
                .andExpect(request().asyncStarted())
                .andReturn();
        MvcResult completedResult = mockMvc.perform(asyncDispatch(initialResult))
                .andExpect(status().isOk())
                .andReturn();

        String sseBody = completedResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<String> dataEvents = parseDataEvents(sseBody);
        String reconstructed = String.join("", dataEvents);
        String expected = String.join("", CHUNKS);

        assertThat(reconstructed)
                .as("WHATWG 표준 파싱 뒤 Flux 결합 텍스트가 그대로 보존되어야 해; content-type=%s encoding=%s body=%s",
                        completedResult.getResponse().getContentType(),
                        completedResult.getResponse().getCharacterEncoding(),
                        sseBody)
                .isEqualTo(expected);
    }

    private static List<String> parseDataEvents(String sseBody) {
        List<String> events = new ArrayList<>();
        StringBuilder dataBuffer = new StringBuilder();
        String normalized = sseBody.replace("\r\n", "\n").replace('\r', '\n');

        for (String line : normalized.split("\n", -1)) {
            if (line.isEmpty()) {
                if (!dataBuffer.isEmpty()) {
                    dataBuffer.setLength(dataBuffer.length() - 1);
                    events.add(dataBuffer.toString());
                    dataBuffer.setLength(0);
                }
                continue;
            }
            if (line.charAt(0) == ':') {
                continue;
            }

            int colon = line.indexOf(':');
            String field = colon < 0 ? line : line.substring(0, colon);
            if (!field.equals("data")) {
                continue;
            }
            String value = colon < 0 ? "" : line.substring(colon + 1);
            if (value.startsWith(" ")) {
                value = value.substring(1);
            }
            dataBuffer.append(value).append('\n');
        }
        return events;
    }
}
