package io.github.paulem.nvidianim4j;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.paulem.nvidianim4j.model.ChatRequest;
import io.github.paulem.nvidianim4j.model.ChatResponse;
import io.github.paulem.nvidianim4j.model.ContentPart;
import io.github.paulem.nvidianim4j.model.Message;
import io.github.paulem.nvidianim4j.streaming.StreamCallback;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link NvidiaClient} using {@link MockWebServer}.
 */
class NvidiaClientTest {

    private MockWebServer mockServer;
    private NvidiaClient client;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        mockServer = new MockWebServer();
        mockServer.start();

        objectMapper = new ObjectMapper();
        client = buildClientWithMockServer();
    }

    /**
     * Builds a client that points to the mock server by replacing the base URL
     * via reflection on the Retrofit-generated service.
     */
    private NvidiaClient buildClientWithMockServer() throws Exception {
        // We create the real client but swap its internal service to point at the mock server
        NvidiaClient real = new NvidiaClient.Builder("test-api-key").build();

        // Override using a package-private builder trick: rebuild using same logic but mock URL
        return buildClientForUrl(mockServer.url("/").toString());
    }

    private NvidiaClient buildClientForUrl(String baseUrl) throws Exception {
        // Use a custom subclass to expose the URL override — instead, we use the
        // Builder and rely on testable design. Since the service is created internally,
        // we use reflection to swap it out.
        NvidiaClient baseClient = new NvidiaClient.Builder("test-api-key").build();

        // Rebuild with mock service using reflection
        retrofit2.Retrofit retrofit = new retrofit2.Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(retrofit2.converter.jackson.JacksonConverterFactory.create(objectMapper))
                .build();

        io.github.paulem.nvidianim4j.api.NvidiaNIMService mockService =
                retrofit.create(io.github.paulem.nvidianim4j.api.NvidiaNIMService.class);

        Field serviceField = NvidiaClient.class.getDeclaredField("service");
        serviceField.setAccessible(true);
        serviceField.set(baseClient, mockService);

        Field omField = NvidiaClient.class.getDeclaredField("objectMapper");
        omField.setAccessible(true);
        omField.set(baseClient, objectMapper);

        return baseClient;
    }

    @AfterEach
    void tearDown() throws IOException {
        mockServer.shutdown();
    }

    // -------------------------------------------------------------------------
    // Model tests
    // -------------------------------------------------------------------------

    @Test
    void message_user_text() {
        Message msg = Message.user("Hello");
        assertEquals("user", msg.getRole());
        assertEquals("Hello", msg.getContentAsString());
        assertNull(msg.getContentAsParts());
    }

    @Test
    void message_assistant() {
        Message msg = Message.assistant("Hi there!");
        assertEquals("assistant", msg.getRole());
        assertEquals("Hi there!", msg.getContentAsString());
    }

    @Test
    void message_system() {
        Message msg = Message.system("You are a helpful assistant.");
        assertEquals("system", msg.getRole());
    }

    @Test
    void message_userWithParts_varargs() {
        ContentPart text = ContentPart.ofText("What is this?");
        ContentPart image = ContentPart.ofImageUrl("data:image/png;base64,abc123");
        Message msg = Message.userWithParts(text, image);
        assertEquals("user", msg.getRole());
        assertNull(msg.getContentAsString());
        List<ContentPart> parts = msg.getContentAsParts();
        assertNotNull(parts);
        assertEquals(2, parts.size());
        assertEquals("text", parts.get(0).getType());
        assertEquals("image_url", parts.get(1).getType());
    }

    @Test
    void chatRequest_builder_validatesModel() {
        assertThrows(IllegalArgumentException.class, () -> new ChatRequest.Builder(null));
        assertThrows(IllegalArgumentException.class, () -> new ChatRequest.Builder(""));
        assertThrows(IllegalArgumentException.class, () -> new ChatRequest.Builder("   "));
    }

    @Test
    void chatRequest_builder_validatesMessages() {
        assertThrows(IllegalStateException.class, () ->
                new ChatRequest.Builder("model/test").build());
    }

    @Test
    void chatRequest_builder_fullBuild() {
        ChatRequest req = new ChatRequest.Builder("meta/llama-3.1-70b-instruct")
                .messages(Message.user("Hi"))
                .maxTokens(512)
                .temperature(0.5)
                .topP(0.9)
                .frequencyPenalty(0.1)
                .presencePenalty(0.2)
                .seed(42L)
                .stop("\\n")
                .stream(false)
                .build();

        assertEquals("meta/llama-3.1-70b-instruct", req.getModel());
        assertEquals(512, req.getMaxTokens());
        assertEquals(0.5, req.getTemperature());
        assertEquals(0.9, req.getTopP());
        assertEquals(0.1, req.getFrequencyPenalty());
        assertEquals(0.2, req.getPresencePenalty());
        assertEquals(42L, req.getSeed());
        assertFalse(req.getStream());
    }

    // -------------------------------------------------------------------------
    // NvidiaClient.Builder tests
    // -------------------------------------------------------------------------

    @Test
    void client_builder_rejectsNullApiKey() {
        assertThrows(IllegalArgumentException.class, () -> new NvidiaClient.Builder(null));
    }

    @Test
    void client_builder_rejectsBlankApiKey() {
        assertThrows(IllegalArgumentException.class, () -> new NvidiaClient.Builder("  "));
    }

    @Test
    void client_builder_rejectsNullObjectMapper() {
        assertThrows(IllegalArgumentException.class, () ->
                new NvidiaClient.Builder("test-key").objectMapper(null));
    }

    @Test
    void client_builder_rejectsNullExecutor() {
        assertThrows(IllegalArgumentException.class, () ->
                new NvidiaClient.Builder("test-key").executor(null));
    }

    // -------------------------------------------------------------------------
    // HTTP / integration tests with MockWebServer
    // -------------------------------------------------------------------------

    @Test
    void chat_success() throws Exception {
        String responseJson = """
                {
                  "id": "chatcmpl-abc",
                  "object": "chat.completion",
                  "created": 1714000000,
                  "model": "meta/llama-3.1-70b-instruct",
                  "choices": [
                    {
                      "index": 0,
                      "message": { "role": "assistant", "content": "Hello!" },
                      "finish_reason": "stop"
                    }
                  ],
                  "usage": { "prompt_tokens": 5, "completion_tokens": 2, "total_tokens": 7 }
                }
                """;

        mockServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(responseJson));

        ChatRequest req = new ChatRequest.Builder("meta/llama-3.1-70b-instruct")
                .messages(Message.user("Hello"))
                .build();

        ChatResponse response = client.chat(req).get();

        assertNotNull(response);
        assertEquals("chatcmpl-abc", response.getId());
        assertEquals("Hello!", response.getFirstChoiceContent());
        assertEquals(7, response.getUsage().getTotalTokens());

        RecordedRequest recorded = mockServer.takeRequest();
        assertEquals("POST", recorded.getMethod());
        assertEquals("/v1/chat/completions", recorded.getPath());
        assertTrue(recorded.getHeader("Authorization").startsWith("Bearer "));
    }

    @Test
    void chat_apiError_throws() {
        mockServer.enqueue(new MockResponse()
                .setResponseCode(401)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"error\": \"Unauthorized\"}"));

        ChatRequest req = new ChatRequest.Builder("meta/llama-3.1-70b-instruct")
                .messages(Message.user("Hello"))
                .build();

        ExecutionException ex = assertThrows(ExecutionException.class,
                () -> client.chat(req).get());
        assertInstanceOf(NvidiaApiException.class, ex.getCause());
        assertEquals(401, ((NvidiaApiException) ex.getCause()).getStatusCode());
    }

    @Test
    void chatStream_success() throws Exception {
        String sseBody = """
                data: {"id":"c1","object":"chat.completion.chunk","created":1714000001,"model":"meta/llama","choices":[{"index":0,"delta":{"role":"assistant","content":"Hi"},"finish_reason":null}]}

                data: {"id":"c1","object":"chat.completion.chunk","created":1714000001,"model":"meta/llama","choices":[{"index":0,"delta":{"content":" there"},"finish_reason":null}]}

                data: [DONE]

                """;

        mockServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody(sseBody));

        ChatRequest req = new ChatRequest.Builder("meta/llama")
                .messages(Message.user("Hello"))
                .stream(true)
                .build();

        List<String> received = new ArrayList<>();
        boolean[] completed = {false};

        CompletableFuture<Void> future = client.chatStream(req, new StreamCallback() {
            @Override
            public void onChunk(ChatResponse chunk) {
                String delta = chunk.getChoices().get(0).getDelta().getContentAsString();
                if (delta != null) received.add(delta);
            }
            @Override
            public void onComplete() { completed[0] = true; }
            @Override
            public void onError(Throwable t) { fail("Unexpected error: " + t.getMessage()); }
        });

        future.get(); // wait for completion

        assertEquals(List.of("Hi", " there"), received);
        assertTrue(completed[0]);
    }

    @Test
    void chatStream_apiError_callsOnError() throws Exception {
        mockServer.enqueue(new MockResponse()
                .setResponseCode(403)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"error\": \"Forbidden\"}"));

        ChatRequest req = new ChatRequest.Builder("meta/llama")
                .messages(Message.user("Hello"))
                .stream(true)
                .build();

        Throwable[] error = {null};
        CompletableFuture<Void> future = client.chatStream(req, new StreamCallback() {
            @Override public void onChunk(ChatResponse chunk) {}
            @Override public void onComplete() {}
            @Override public void onError(Throwable t) { error[0] = t; }
        });

        assertThrows(ExecutionException.class, future::get);
        assertInstanceOf(NvidiaApiException.class, error[0]);
        assertEquals(403, ((NvidiaApiException) error[0]).getStatusCode());
    }

    // -------------------------------------------------------------------------
    // ImageUtils tests
    // -------------------------------------------------------------------------

    @Test
    void imageUtils_fromBytes() {
        byte[] fakeImage = {(byte) 0x89, 0x50, 0x4E, 0x47}; // PNG magic bytes
        ContentPart part = ImageUtils.imagePartFromBytes(fakeImage, "image/png");
        assertEquals("image_url", part.getType());
        assertNotNull(part.getImageUrl());
        assertTrue(part.getImageUrl().getUrl().startsWith("data:image/png;base64,"));
    }

    @Test
    void imageUtils_nvcfAsset() {
        ContentPart part = ImageUtils.imagePartFromNvcfAsset("my-uuid-1234");
        assertEquals("image_url", part.getType());
        assertEquals("data-nvcf-asset-id://my-uuid-1234", part.getImageUrl().getUrl());
    }

    // -------------------------------------------------------------------------
    // NvidiaApiException tests
    // -------------------------------------------------------------------------

    @Test
    void apiException_statusCode() {
        NvidiaApiException ex = new NvidiaApiException(404, "Not found");
        assertEquals(404, ex.getStatusCode());
        assertTrue(ex.getMessage().contains("404"));
    }

    @Test
    void apiException_networkError() {
        IOException cause = new IOException("timeout");
        NvidiaApiException ex = new NvidiaApiException("timeout error", cause);
        assertEquals(-1, ex.getStatusCode());
        assertSame(cause, ex.getCause());
    }
}
