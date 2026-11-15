package com.promptform.api.generation;

import com.promptform.api.ratelimit.RateLimitException;
import com.promptform.api.ratelimit.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;
import java.util.concurrent.Executor;

import static com.promptform.api.generation.GenerationModels.GenerateRequest;

@RestController
@RequestMapping("/api")
public class GenerationController {
    private final GenerationService service;
    private final Executor generationExecutor;
    private final RateLimiter rateLimiter;

    public GenerationController(GenerationService service, Executor generationExecutor, RateLimiter rateLimiter) {
        this.service = service;
        this.generationExecutor = generationExecutor;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping(value = "/generate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generate(@Valid @RequestBody GenerateRequest request,
                               HttpServletRequest httpRequest,
                               Principal principal) {
        String identity = principal == null ? httpRequest.getRemoteAddr() : principal.getName();
        if (!rateLimiter.allow("generation:" + identity)) {
            throw new RateLimitException("Generation rate limit reached. Try again in a minute.");
        }
        SseEmitter emitter = new SseEmitter(60_000L);
        generationExecutor.execute(() -> {
            try {
                send(emitter, "progress", Map.of("message", "Validating the form schema…"));
                send(emitter, "result", Map.of("schema", service.generate(request.prompt())));
                emitter.complete();
            } catch (Exception exception) {
                try {
                    send(emitter, "error", Map.of("message", safeErrorMessage(exception)));
                } catch (IOException ignored) {
                    emitter.complete();
                    return;
                }
                emitter.complete();
            }
        });
        return emitter;
    }

    private void send(SseEmitter emitter, String event, Object data) throws IOException {
        emitter.send(SseEmitter.event().name(event).data(data));
    }

    private String safeErrorMessage(Exception exception) {
        if (exception instanceof GenerationException generationException
                && generationException.getMessage() != null) {
            return generationException.getMessage();
        }
        return "Generation failed. Please try again.";
    }

}
