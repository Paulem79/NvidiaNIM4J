package io.github.paulem.nvidianim4j.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single choice (completion candidate) in the API response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Choice {

    @JsonProperty("index")
    private int index;

    /** Present in non-streaming responses. */
    @JsonProperty("message")
    private Message message;

    /** Present in streaming delta responses. */
    @JsonProperty("delta")
    private Message delta;

    @JsonProperty("finish_reason")
    private String finishReason;

    public Choice() {}

    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }

    public Message getMessage() { return message; }
    public void setMessage(Message message) { this.message = message; }

    public Message getDelta() { return delta; }
    public void setDelta(Message delta) { this.delta = delta; }

    public String getFinishReason() { return finishReason; }
    public void setFinishReason(String finishReason) { this.finishReason = finishReason; }

    @Override
    public String toString() {
        return "Choice{index=" + index +
               ", message=" + message +
               ", delta=" + delta +
               ", finishReason='" + finishReason + "'}";
    }
}
