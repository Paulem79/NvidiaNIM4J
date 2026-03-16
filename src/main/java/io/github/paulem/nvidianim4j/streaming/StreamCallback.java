package io.github.paulem.nvidianim4j.streaming;

import io.github.paulem.nvidianim4j.model.ChatResponse;

/**
 * Callback interface for handling server-sent events (SSE) during a streaming
 * chat completion.
 *
 * <p>Implement this interface and pass it to
 * {@link io.github.paulem.nvidianim4j.NvidiaClient#chatStream}.</p>
 */
public interface StreamCallback {

    /**
     * Called for each SSE chunk received from the server.
     *
     * @param chunk the parsed chunk (contains delta in {@code choices[0].delta})
     */
    void onChunk(ChatResponse chunk);

    /**
     * Called when the stream has finished successfully (after {@code [DONE]}).
     */
    void onComplete();

    /**
     * Called when an error occurs during streaming.
     *
     * @param throwable the error
     */
    void onError(Throwable throwable);
}
