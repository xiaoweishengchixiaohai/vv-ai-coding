package edu.ncu.vvaicoding.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import edu.ncu.vvaicoding.service.ChatHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@Slf4j
public class AIChatModelFactory {

    @Resource
    private ChatModel chatModel;

    @Resource
    private StreamingChatModel streamingChatModel;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private ChatHistoryService chatHistoryService;

    /**
     * AI 服务实例缓存
     * 缓存策略：
     * - 最大缓存 1000 个实例
     * - 写入后 30 分钟过期
     * - 访问后 10 分钟过期
     */
    private final Cache<Long, AICodeGenerateService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("AI 服务实例被移除，appId: {}, 原因: {}", key, cause);
            })
            .build();


    public AICodeGenerateService getAICodeGenerateService(Long id) {
        return serviceCache.get(id, this::createAICodeGenerateService);
    }

    private AICodeGenerateService createAICodeGenerateService(Long id) {
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.
                builder()
                .id(id)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(10)
                .build();

        int i = chatHistoryService.initChatMessage(id, chatMemory, 10);
        log.debug("初始化聊天记录，appId: {}, 初始化条数: {}", id, i);

        return AiServices.
                builder(AICodeGenerateService.class).
                chatModel(chatModel).
                chatMemory(chatMemory).
                streamingChatModel(streamingChatModel).
                build();
    }
}
