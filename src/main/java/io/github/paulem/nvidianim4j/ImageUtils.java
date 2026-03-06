package io.github.paulem.nvidianim4j;

import io.github.paulem.nvidianim4j.model.ContentPart;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

/**
 * Utility methods for working with images in NVIDIA NIM API requests.
 *
 * <p>Images can be passed as base64-encoded data URIs in the message content.
 * Supported formats are PNG, JPG/JPEG.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * ContentPart imagePart = ImageUtils.imagePartFromFile(Path.of("photo.png"), "image/png");
 * Message msg = Message.userWithParts(
 *     ContentPart.ofText("Is there a car in this image?"),
 *     imagePart
 * );
 * }</pre>
 */
public final class ImageUtils {

    private ImageUtils() {}

    /**
     * Creates a {@link ContentPart} of type {@code image_url} from a file on disk,
     * encoding it as a base64 data URI.
     *
     * @param imagePath the path to the image file (PNG, JPG, or JPEG)
     * @param mimeType  the MIME type (e.g. {@code "image/png"} or {@code "image/jpeg"})
     * @return a ContentPart containing the base64-encoded image
     * @throws IOException if the file cannot be read
     */
    public static ContentPart imagePartFromFile(Path imagePath, String mimeType) throws IOException {
        byte[] bytes = Files.readAllBytes(imagePath);
        return imagePartFromBytes(bytes, mimeType);
    }

    /**
     * Creates a {@link ContentPart} of type {@code image_url} from raw image bytes,
     * encoding them as a base64 data URI.
     *
     * @param imageBytes the raw image bytes
     * @param mimeType   the MIME type (e.g. {@code "image/png"} or {@code "image/jpeg"})
     * @return a ContentPart containing the base64-encoded image
     */
    public static ContentPart imagePartFromBytes(byte[] imageBytes, String mimeType) {
        String base64 = Base64.getEncoder().encodeToString(imageBytes);
        String dataUri = "data:" + mimeType + ";base64," + base64;
        return ContentPart.ofImageUrl(dataUri);
    }

    /**
     * Creates a {@link ContentPart} of type {@code image_url} from an {@link InputStream},
     * encoding the data as a base64 data URI.
     *
     * @param stream   the input stream of the image
     * @param mimeType the MIME type (e.g. {@code "image/png"} or {@code "image/jpeg"})
     * @return a ContentPart containing the base64-encoded image
     * @throws IOException if the stream cannot be read
     */
    public static ContentPart imagePartFromStream(InputStream stream, String mimeType) throws IOException {
        byte[] bytes = stream.readAllBytes();
        return imagePartFromBytes(bytes, mimeType);
    }

    /**
     * Creates a {@link ContentPart} of type {@code image_url} from an NVCF asset ID.
     *
     * <p>Use this when the image has been uploaded via the NVCF Asset API and you have
     * obtained an asset ID.</p>
     *
     * @param assetId the NVCF asset UUID
     * @return a ContentPart referencing the NVCF asset
     */
    public static ContentPart imagePartFromNvcfAsset(String assetId) {
        return ContentPart.ofImageUrl("data-nvcf-asset-id://" + assetId);
    }
}
