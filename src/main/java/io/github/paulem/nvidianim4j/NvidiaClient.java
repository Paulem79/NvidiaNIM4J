package io.github.paulem.nvidianim4j;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.paulem.nvidianim4j.api.NvidiaNIMService;
import io.github.paulem.nvidianim4j.model.ChatRequest;
import io.github.paulem.nvidianim4j.model.ChatResponse;
import io.github.paulem.nvidianim4j.streaming.StreamCallback;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

/**
 * Thread-safe client for the NVIDIA NIM chat completions API.
 *
 * <p>Create an instance via {@link Builder}:</p>
 * <pre>{@code
 * NvidiaClient client = new NvidiaClient.Builder("nvapi-your-key-here")
 *     .build();
 *
 * // Non-streaming
 * ChatRequest req = new ChatRequest.Builder("meta/llama-3.1-70b-instruct")
 *     .messages(Message.user("Hello!"))
 *     .maxTokens(512)
 *     .build();
 * ChatResponse response = client.chat(req).get();
 * System.out.println(response.getFirstChoiceContent());
 *
 * // Streaming
 * ChatRequest streamReq = new ChatRequest.Builder("meta/llama-3.1-70b-instruct")
 *     .messages(Message.user("Tell me a story"))
 *     .stream(true)
 *     .build();
 * client.chatStream(streamReq, new StreamCallback() {
 *     public void onChunk(ChatResponse chunk) {
 *         System.out.print(chunk.getChoices().get(0).getDelta().getContentAsString());
 *     }
 *     public void onComplete() { System.out.println("\nDone"); }
 *     public void onError(Throwable t) { t.printStackTrace(); }
 * });
 * }</pre>
 */
public class NvidiaClient {

    private static final String SSE_DATA_PREFIX = "data: ";
    private static final String SSE_DONE_SIGNAL = "[DONE]";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String EVENT_STREAM_ACCEPT = "text/event-stream";

    private final String apiKey;
    private final NvidiaNIMService service;
    private final ObjectMapper objectMapper;
    private final Executor executor;

    private NvidiaClient(Builder builder) {
        this.apiKey = builder.apiKey;
        this.objectMapper = builder.objectMapper;
        this.executor = builder.executor;
        this.service = buildService(builder);
    }

    private NvidiaNIMService buildService(Builder builder) {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(builder.connectTimeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(builder.readTimeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(builder.writeTimeoutSeconds, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(NvidiaNIMService.BASE_URL)
                .client(httpClient)
                .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                .build();

        return retrofit.create(NvidiaNIMService.class);
    }

    /**
     * Sends a non-streaming chat completion request asynchronously.
     *
     * @param request the chat request (must not have {@code stream=true})
     * @return a {@link CompletableFuture} that resolves to a {@link ChatResponse}
     */
    public CompletableFuture<ChatResponse> chat(ChatRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            Call<ChatResponse> call = service.chatCompletion(BEARER_PREFIX + apiKey, request);
            try {
                Response<ChatResponse> response = call.execute();
                if (response.isSuccessful() && response.body() != null) {
                    return response.body();
                }
                String errorBody = response.errorBody() != null ? response.errorBody().string() : "(no body)";
                throw new NvidiaApiException(response.code(), errorBody);
            } catch (NvidiaApiException e) {
                throw e;
            } catch (IOException e) {
                throw new NvidiaApiException("Network error while calling NVIDIA NIM API", e);
            }
        }, executor);
    }

    /**
     * Sends a streaming chat completion request asynchronously.
     *
     * <p>The provided {@link StreamCallback} is invoked on the executor thread for
     * each SSE chunk, and once when the stream finishes or errors.</p>
     *
     * @param request  the chat request (should have {@code stream=true})
     * @param callback receives chunks, completion, and error events
     * @return a {@link CompletableFuture} that completes when streaming is done
     */
    public CompletableFuture<Void> chatStream(ChatRequest request, StreamCallback callback) {
        return CompletableFuture.runAsync(() -> {
            Call<ResponseBody> call = service.chatCompletionStream(
                    BEARER_PREFIX + apiKey,
                    EVENT_STREAM_ACCEPT,
                    request
            );
            try {
                Response<ResponseBody> response = call.execute();
                if (!response.isSuccessful() || response.body() == null) {
                    String errorBody = response.errorBody() != null
                            ? response.errorBody().string() : "(no body)";
                    throw new NvidiaApiException(response.code(), errorBody);
                }

                try (ResponseBody body = response.body();
                     BufferedReader reader = new BufferedReader(
                             new InputStreamReader(body.byteStream(), StandardCharsets.UTF_8))) {

                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith(SSE_DATA_PREFIX)) {
                            String data = line.substring(SSE_DATA_PREFIX.length()).trim();
                            if (SSE_DONE_SIGNAL.equals(data)) {
                                break;
                            }
                            if (!data.isEmpty()) {
                                ChatResponse chunk = objectMapper.readValue(data, ChatResponse.class);
                                callback.onChunk(chunk);
                            }
                        }
                    }
                }
                callback.onComplete();
            } catch (NvidiaApiException e) {
                callback.onError(e);
                throw e;
            } catch (IOException e) {
                NvidiaApiException wrapped = new NvidiaApiException(
                        "Network error while streaming from NVIDIA NIM API", e);
                callback.onError(wrapped);
                throw wrapped;
            }
        }, executor);
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    /** Builder for {@link NvidiaClient}. */
    public static final class Builder {

        private final String apiKey;
        private ObjectMapper objectMapper;
        private Executor executor;
        private long connectTimeoutSeconds = 30;
        private long readTimeoutSeconds = 120;
        private long writeTimeoutSeconds = 30;

        /**
         * Creates a new builder.
         *
         * @param apiKey your NVIDIA NIM API key (e.g. {@code "nvapi-..."})
         */
        public Builder(String apiKey) {
            if (apiKey == null || apiKey.isBlank()) {
                throw new IllegalArgumentException("apiKey must not be null or blank");
            }
            this.apiKey = apiKey;
            this.objectMapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            this.executor = ForkJoinPool.commonPool();
        }

        /**
         * Overrides the default {@link ObjectMapper}.
         *
         * @param objectMapper a custom Jackson {@link ObjectMapper}
         * @return this builder
         */
        public Builder objectMapper(ObjectMapper objectMapper) {
            if (objectMapper == null) throw new IllegalArgumentException("objectMapper must not be null");
            this.objectMapper = objectMapper;
            return this;
        }

        /**
         * Overrides the {@link Executor} used for async calls.
         *
         * @param executor a custom executor
         * @return this builder
         */
        public Builder executor(Executor executor) {
            if (executor == null) throw new IllegalArgumentException("executor must not be null");
            this.executor = executor;
            return this;
        }

        /**
         * Sets the HTTP connect timeout in seconds (default: 30).
         *
         * @param seconds timeout value
         * @return this builder
         */
        public Builder connectTimeout(long seconds) {
            this.connectTimeoutSeconds = seconds;
            return this;
        }

        /**
         * Sets the HTTP read timeout in seconds (default: 120).
         *
         * @param seconds timeout value
         * @return this builder
         */
        public Builder readTimeout(long seconds) {
            this.readTimeoutSeconds = seconds;
            return this;
        }

        /**
         * Sets the HTTP write timeout in seconds (default: 30).
         *
         * @param seconds timeout value
         * @return this builder
         */
        public Builder writeTimeout(long seconds) {
            this.writeTimeoutSeconds = seconds;
            return this;
        }

        /** Builds the {@link NvidiaClient}. */
        public NvidiaClient build() {
            return new NvidiaClient(this);
        }
    }
}
