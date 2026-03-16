package io.github.paulem.nvidianim4j.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a single content part within a message (text or image_url).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContentPart {

    @JsonProperty("type")
    private String type;

    @JsonProperty("text")
    private String text;

    @JsonProperty("image_url")
    private ImageUrl imageUrl;

    public ContentPart() {}

    /**
     * Creates a text content part.
     *
     * @param text the text content
     * @return a text ContentPart
     */
    public static ContentPart ofText(String text) {
        ContentPart part = new ContentPart();
        part.type = "text";
        part.text = text;
        return part;
    }

    /**
     * Creates an image content part from a URL or base64 data URI.
     *
     * @param url the image URL or base64 data URI (e.g. "data:image/png;base64,...")
     * @return an image ContentPart
     */
    public static ContentPart ofImageUrl(String url) {
        ContentPart part = new ContentPart();
        part.type = "image_url";
        part.imageUrl = new ImageUrl(url);
        return part;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public ImageUrl getImageUrl() { return imageUrl; }
    public void setImageUrl(ImageUrl imageUrl) { this.imageUrl = imageUrl; }
}
