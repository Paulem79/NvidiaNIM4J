package io.github.paulem.nvidianim4j.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response from the NVIDIA NIM chat completions API (non-streaming).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatResponse {

    @JsonProperty("id")
    private String id;

    @JsonProperty("object")
    private String object;

    @JsonProperty("created")
    private long created;

    @JsonProperty("model")
    private String model;

    @JsonProperty("choices")
    private List<Choice> choices;

    @JsonProperty("usage")
    private Usage usage;

    public ChatResponse() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getObject() { return object; }
    public void setObject(String object) { this.object = object; }

    public long getCreated() { return created; }
    public void setCreated(long created) { this.created = created; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public List<Choice> getChoices() { return choices; }
    public void setChoices(List<Choice> choices) { this.choices = choices; }

    public Usage getUsage() { return usage; }
    public void setUsage(Usage usage) { this.usage = usage; }

    /**
     * Convenience method to get the text content of the first choice.
     *
     * @return the content string of the first choice, or {@code null} if absent
     */
    public String getFirstChoiceContent() {
        if (choices == null || choices.isEmpty()) return null;
        Message msg = choices.get(0).getMessage();
        if (msg == null) return null;
        return msg.getContentAsString();
    }

    @Override
    public String toString() {
        return "ChatResponse{id='" + id + "', model='" + model +
               "', choices=" + choices + ", usage=" + usage + '}';
    }
}
