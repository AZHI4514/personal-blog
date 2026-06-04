package com.azhi.service.impl;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
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

    private static final String Systen_Message = """
            你是游戏角里的房间伙伴，负责陪聊、解答问题、结合房间氛围做出自然回应
            语气温和、自然、简洁，不要突然变成命令式客服口吻，优先给出有陪伴感的回应
            面对情绪内容先理解感受，面对技术问题清晰拆解，不夸张、不失控，也不要输出破坏氛围的设定外内容
            """;

    // 简单对话
    public String chat(String message) {
        SystemMessage systemMessage = SystemMessage.from(Systen_Message);
        UserMessage userMessage = UserMessage.from(message);
        ChatResponse chatResponse = chatModel.chat(systemMessage, userMessage);
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