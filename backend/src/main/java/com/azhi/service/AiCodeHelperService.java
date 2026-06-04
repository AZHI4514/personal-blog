package com.azhi.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;

public interface AiCodeHelperService {
    @SystemMessage(fromResource = "system-prompt.txt")
    String chat(@MemoryId int memoryId, String userMessage);
}
