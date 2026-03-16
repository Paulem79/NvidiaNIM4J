package io.github.paulem.nvidianim4j.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Request payload for the NVIDIA NIM chat completions API.
 *
 * <p>Use {@link Builder} to construct instances:</p>
 * <pre>{@code
 * ChatRequest request = new ChatRequest.Builder("meta/llama-3.1-70b-instruct")
 *     .message(Message.user("Hello!"))
 *     .maxTokens(1024)
 *     .temperature(0.2)
 *     .stream(false)
 *     .build();
 * }</pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatRequest {

    @JsonProperty("model")
    private String model;

    @JsonProperty("messages")
    private List<Message> messages;

    @JsonProperty("max_tokens")
    private Integer maxTokens;

    @JsonProperty("temperature")
    private Double temperature;

    @JsonProperty("top_p")
    private Double topP;

    @JsonProperty("frequency_penalty")
    private Double frequencyPenalty;

    @JsonProperty("presence_penalty")
    private Double presencePenalty;

    @JsonProperty("seed")
    private Long seed;

    @JsonProperty("stop")
    private List<String> stop;

    @JsonProperty("stream")
    private Boolean stream;

    private ChatRequest() {}

    public String getModel() { return model; }
    public List<Message> getMessages() { return messages; }
    public Integer getMaxTokens() { return maxTokens; }
    public Double getTemperature() { return temperature; }
    public Double getTopP() { return topP; }
    public Double getFrequencyPenalty() { return frequencyPenalty; }
    public Double getPresencePenalty() { return presencePenalty; }
    public Long getSeed() { return seed; }
    public List<String> getStop() { return stop; }
    public Boolean getStream() { return stream; }

    /** Builder for {@link ChatRequest}. */
    public static final class Builder {
        private final String model;
        private List<Message> messages;
        private Integer maxTokens;
        private Double temperature;
        private Double topP;
        private Double frequencyPenalty;
        private Double presencePenalty;
        private Long seed;
        private List<String> stop;
        private Boolean stream;

        /**
         * Creates a new builder for the given model.
         *
         * @param model the model identifier (e.g. {@code "meta/llama-3.1-70b-instruct"})
         */
        public Builder(String model) {
            if (model == null || model.isBlank()) {
                throw new IllegalArgumentException("model must not be null or blank");
            }
            this.model = model;
        }

        /** Sets the conversation messages. */
        public Builder messages(List<Message> messages) {
            this.messages = messages;
            return this;
        }

        /** Sets the conversation messages. */
        public Builder messages(Message... messages) {
            this.messages = List.of(messages);
            return this;
        }

        /** Sets the maximum number of tokens to generate (1–8192). */
        public Builder maxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        /** Sets the sampling temperature (0–1). */
        public Builder temperature(double temperature) {
            this.temperature = temperature;
            return this;
        }

        /** Sets the nucleus-sampling probability mass (≤ 1). */
        public Builder topP(double topP) {
            this.topP = topP;
            return this;
        }

        /** Sets the frequency penalty (−2 to 2). */
        public Builder frequencyPenalty(double frequencyPenalty) {
            this.frequencyPenalty = frequencyPenalty;
            return this;
        }

        /** Sets the presence penalty (−2 to 2). */
        public Builder presencePenalty(double presencePenalty) {
            this.presencePenalty = presencePenalty;
            return this;
        }

        /** Sets a seed for reproducible outputs. */
        public Builder seed(long seed) {
            this.seed = seed;
            return this;
        }

        /** Sets stop sequences. */
        public Builder stop(List<String> stop) {
            this.stop = stop;
            return this;
        }

        /** Sets stop sequences. */
        public Builder stop(String... stop) {
            this.stop = List.of(stop);
            return this;
        }

        /**
         * Enables or disables streaming (server-sent events).
         *
         * <p>When {@code true}, use
         * {@link io.github.paulem.nvidianim4j.NvidiaClient#chatStream} instead of
         * {@link io.github.paulem.nvidianim4j.NvidiaClient#chat}.</p>
         */
        public Builder stream(boolean stream) {
            this.stream = stream;
            return this;
        }

        /** Builds the {@link ChatRequest}. */
        public ChatRequest build() {
            if (messages == null || messages.isEmpty()) {
                throw new IllegalStateException("messages must not be null or empty");
            }
            ChatRequest req = new ChatRequest();
            req.model = model;
            req.messages = messages;
            req.maxTokens = maxTokens;
            req.temperature = temperature;
            req.topP = topP;
            req.frequencyPenalty = frequencyPenalty;
            req.presencePenalty = presencePenalty;
            req.seed = seed;
            req.stop = stop;
            req.stream = stream;
            return req;
        }
    }
}
