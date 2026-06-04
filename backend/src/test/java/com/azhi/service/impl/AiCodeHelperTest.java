package com.azhi.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class AiCodeHelperTest {

    @Autowired
    private AiCodeHelper aiCodeHelper;

    @Test
    void testChat() {
        String response = aiCodeHelper.chat("你好，你是谁");
        System.out.println("AI 回复: " + response);
        assertNotNull(response);
        assertTrue(response.length() > 0);
    }
}
