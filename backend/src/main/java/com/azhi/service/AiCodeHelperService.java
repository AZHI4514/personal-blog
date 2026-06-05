package com.azhi.service;

import com.azhi.service.impl.SafeInputGuardrail;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.guardrail.InputGuardrails;

import java.util.List;

@InputGuardrails({ SafeInputGuardrail.class })
public interface AiCodeHelperService {
    @SystemMessage(fromResource = "system-prompt.txt")
    String chat(@MemoryId int memoryId, @UserMessage String userMessage);

    @SystemMessage(fromResource = "system-prompt.txt")
    Report chatForReport(@MemoryId int memoryId, @UserMessage String userMessage);

    // 学习报告
    record Report(String name, List<String> suggesttionlist){
    }
}
