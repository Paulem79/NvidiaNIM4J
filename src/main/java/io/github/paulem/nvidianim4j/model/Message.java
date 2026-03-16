package io.github.paulem.nvidianim4j.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.List;

/**
 * Represents a message in the conversation.
 *
 * <p>The {@code content} field can be either a plain string or a list of
 * {@link ContentPart} objects (for passing images alongside text).</p>
 *
 * <p>Examples:</p>
 * <pre>{@code
 * // Simple text message
 * Message userMsg = Message.user("Hello!");
 *
 * // Message with an image (base64)
 * Message imageMsg = Message.userWithParts(
 *     ContentPart.ofText("Is there a car in this image?"),
 *     ContentPart.ofImageUrl("data:image/png;base64,<base64data>")
 * );
 * }</pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Message {

    @JsonProperty("role")
    private String role;

    /** Content can be a String or a List of ContentPart – handled by a custom serializer. */
    @JsonProperty("content")
    private Object content;

    public Message() {}

    private Message(String role, Object content) {
        this.role = role;
        this.content = content;
    }

    /**
     * Creates a user message with plain text content.
     *
     * @param content the text content
     * @return a user Message
     */
    public static Message user(String content) {
        return new Message("user", content);
    }

    /**
     * Creates a user message with a list of content parts (text + images).
     *
     * @param parts the content parts
     * @return a user Message
     */
    public static Message userWithParts(List<ContentPart> parts) {
        return new Message("user", parts);
    }

    /**
     * Creates a user message with a list of content parts (text + images).
     *
     * @param parts the content parts
     * @return a user Message
     */
    public static Message userWithParts(ContentPart... parts) {
        return new Message("user", List.of(parts));
    }

    /**
     * Creates an assistant message with plain text content.
     *
     * @param content the text content
     * @return an assistant Message
     */
    public static Message assistant(String content) {
        return new Message("assistant", content);
    }

    /**
     * Creates a system message with plain text content.
     *
     * @param content the text content
     * @return a system Message
     */
    public static Message system(String content) {
        return new Message("system", content);
    }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Object getContent() { return content; }
    public void setContent(Object content) { this.content = content; }

    /**
     * Returns the content as a plain string if it is a string, otherwise {@code null}.
     *
     * @return string content or {@code null}
     */
    public String getContentAsString() {
        if (content instanceof String s) return s;
        return null;
    }

    /**
     * Returns the content as a list of {@link ContentPart} if it is a list, otherwise {@code null}.
     *
     * @return list of content parts or {@code null}
     */
    @SuppressWarnings("unchecked")
    public List<ContentPart> getContentAsParts() {
        if (content instanceof List<?> list) return (List<ContentPart>) list;
        return null;
    }
}
