package com.azhi.controller;

import com.azhi.service.AiCodeHelperService;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai")
public class AiControlller {

    @Resource
    private AiCodeHelperService aiCodeHelperService;

    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chat(
            @RequestParam int memoryId,
            @RequestParam String message
    ) {
        return aiCodeHelperService.chatStream(memoryId, message)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .event("message")
                        .data(chunk)
                        .build())
                .concatWithValues(ServerSentEvent.<String>builder()
                        .event("done")
                        .data("[DONE]")
                        .build())
                .onErrorResume(error -> Flux.just(ServerSentEvent.<String>builder()
                        .event("error")
                        .data(error.getMessage())
                        .build()));
    }
}
