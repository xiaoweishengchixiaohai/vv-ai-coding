package edu.ncu.vvaicoding.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import edu.ncu.vvaicoding.ai.model.enums.CodeGenTypeEnum;
import edu.ncu.vvaicoding.ai.tools.FileWriteTool;
import edu.ncu.vvaicoding.exception.BusinessException;
import edu.ncu.vvaicoding.exception.ErrorCode;
import edu.ncu.vvaicoding.service.ChatHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@Slf4j
public class AIChatModelFactory {

    @Resource
    private ChatModel chatModel;

    @Resource
    private StreamingChatModel openAiStreamingChatModel;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private StreamingChatModel reasoningStreamingChatModel;
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


    @Bean
    public AICodeTypeService aiCodeTypeService() {
        return AiServices.builder(AICodeTypeService.class).chatModel(chatModel).build();
    }

    public AICodeGenerateService getAICodeGenerateService(Long id) {
        return serviceCache.get(id, this::createAICodeGenerateService);
    }

    public AICodeGenerateService getAICodeGenerateService(Long id, CodeGenTypeEnum codeGenTypeEnum) {
        if (codeGenTypeEnum == null)
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成模式为空");
        return serviceCache.get(id, appId -> createAICodeGenerateService(appId, codeGenTypeEnum));
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
                streamingChatModel(openAiStreamingChatModel).
                build();
    }

    private AICodeGenerateService createAICodeGenerateService(Long id, CodeGenTypeEnum codeGenTypeEnum) {
        if (codeGenTypeEnum == null)
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成模式为空");


        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.
                builder()
                .id(id)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(50)
                .build();

        int i = chatHistoryService.initChatMessage(id, chatMemory, 10);
        log.debug("初始化聊天记录，appId: {}, 初始化条数: {}", id, i);

        return switch (codeGenTypeEnum) {
            case VUE_PROJECT -> AiServices.
                    builder(AICodeGenerateService.class).
                    chatModel(chatModel).
                    chatMemoryProvider(appId -> chatMemory).
                    streamingChatModel(reasoningStreamingChatModel).
                    tools(new FileWriteTool()).
                    hallucinatedToolNameStrategy(toolExecutionRequest ->
                            ToolExecutionResultMessage.from(toolExecutionRequest,
                                    "Error: there is no tool called" + toolExecutionRequest.name())).
                    build();
            case HTML, MULTI_FILE -> AiServices.
                    builder(AICodeGenerateService.class).
                    chatModel(chatModel).
                    chatMemory(chatMemory).
                    streamingChatModel(openAiStreamingChatModel).
                    build();
        };
    }

}
