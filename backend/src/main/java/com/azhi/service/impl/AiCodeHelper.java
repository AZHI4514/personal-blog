package com.azhi.service.impl;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AiCodeHelper {

    @Resource
    private ChatModel chatModel;

    // 简单对话
    public String chat(String message) {
        UserMessage userMessage = UserMessage.from(message);
        ChatResponse chatResponse = chatModel.chat(userMessage);
        AiMessage aiMessage = chatResponse.aiMessage();
        log.info("AI 回复：" + aiMessage.toString());
        return aiMessage.text();
    }

    // 简单对话
    public String chatWithMessage(UserMessage  userMessage) {
        ChatResponse chatResponse = chatModel.chat(userMessage);
        AiMessage aiMessage = chatResponse.aiMessage();
        log.info("AI 回复：" + aiMessage.toString());
        return aiMessage.text();
    }
}