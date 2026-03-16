package io.github.paulem.nvidianim4j.api;

import io.github.paulem.nvidianim4j.model.ChatRequest;
import io.github.paulem.nvidianim4j.model.ChatResponse;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Streaming;

/**
 * Retrofit service interface for the NVIDIA NIM chat completions API.
 */
public interface NvidiaNIMService {

    String BASE_URL = "https://integrate.api.nvidia.com/";

    /**
     * Sends a non-streaming chat completion request.
     *
     * @param authorization the {@code Authorization} header value (e.g. {@code "Bearer <key>"})
     * @param request       the request payload
     * @return a Retrofit {@link Call} that resolves to a {@link ChatResponse}
     */
    @POST("v1/chat/completions")
    Call<ChatResponse> chatCompletion(
            @Header("Authorization") String authorization,
            @Body ChatRequest request
    );

    /**
     * Sends a streaming chat completion request. The response body is a raw
     * {@code text/event-stream} that must be read line-by-line.
     *
     * @param authorization the {@code Authorization} header value (e.g. {@code "Bearer <key>"})
     * @param accept        should be {@code "text/event-stream"}
     * @param request       the request payload (must have {@code stream=true})
     * @return a Retrofit {@link Call} wrapping a raw {@link ResponseBody}
     */
    @Streaming
    @POST("v1/chat/completions")
    Call<ResponseBody> chatCompletionStream(
            @Header("Authorization") String authorization,
            @Header("Accept") String accept,
            @Body ChatRequest request
    );
}
