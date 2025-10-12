package edu.ncu.vvaicoding.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIChatModelFactory {

    @Resource
    private ChatModel chatModel;

    @Resource
    private StreamingChatModel streamingChatModel;

    @Bean
    public AICodeGenerateService getAIServices() {
        return AiServices.builder(AICodeGenerateService.class).
                chatModel(chatModel).
                streamingChatModel(streamingChatModel).
                build();
    }

}
