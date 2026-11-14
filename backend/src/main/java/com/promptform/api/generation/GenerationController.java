package com.promptform.api.generation;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executor;

import static com.promptform.api.generation.GenerationModels.GenerateRequest;

@RestController
@RequestMapping("/api")
public class GenerationController {
    private final GenerationService service;
    private final Executor generationExecutor;

    public GenerationController(GenerationService service, Executor generationExecutor) {
        this.service = service;
        this.generationExecutor = generationExecutor;
    }

    @PostMapping(value = "/generate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generate(@Valid @RequestBody GenerateRequest request) {
        SseEmitter emitter = new SseEmitter(60_000L);
        generationExecutor.execute(() -> {
            try {
                send(emitter, "progress", Map.of("message", "Validating the form schema…"));
                send(emitter, "result", Map.of("schema", service.generate(request.prompt())));
                emitter.complete();
            } catch (Exception exception) {
                try {
                    send(emitter, "error", Map.of("message", exception.getMessage() == null ? "Generation failed." : exception.getMessage()));
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

}
