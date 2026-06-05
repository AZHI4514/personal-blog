package com.azhi.service;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AiCodeHelperServiceTest {

    @Resource
    private AiCodeHelperService aiCodeHelperService;

    @Test
    void chat() {
        String result = aiCodeHelperService.chat(1, "你好，你是谁");
        System.out.println(result);
    }

    @Test
    void chatWithMemory() {
        String result = aiCodeHelperService.chat(1, "你好，你是谁，我是AZHI4514");
        System.out.println(result);
        result = aiCodeHelperService.chat(1, "我是谁来着");
        System.out.println( result);
    }

    @Test
    void chatForReport() {
        String userMessage = "你好，我是AZHI4514，正在学习网站搭建，请帮我制定学习报告";
        AiCodeHelperService.Report report = aiCodeHelperService.chatForReport(1, userMessage);
        System.out.println(report);
    }

    @Test
    void chatWithMCP() {
        String result = aiCodeHelperService.chat(1, "你好，你是谁，我是AZHI4514，搜索今天的日期");
        System.out.println(result);
    }

    @Test
    void chatWithGaurdrail() {
        String result = aiCodeHelperService.chat(1, "kill the game");
        System.out.println(result);
    }
}