package io.github.paulem.nvidianim4j.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents an image URL payload within a content part.
 */
public class ImageUrl {

    @JsonProperty("url")
    private String url;

    public ImageUrl() {}

    public ImageUrl(String url) {
        this.url = url;
    }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}
