package edu.ncu.vvaicoding.cores;

import cn.hutool.json.JSONUtil;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import edu.ncu.vvaicoding.ai.AIChatModelFactory;
import edu.ncu.vvaicoding.ai.AICodeGenerateService;
import edu.ncu.vvaicoding.ai.model.HtmlCodeResult;
import edu.ncu.vvaicoding.ai.model.MultiFileCodeResult;
import edu.ncu.vvaicoding.ai.model.enums.CodeGenTypeEnum;
import edu.ncu.vvaicoding.ai.model.message.AiResponseMessage;
import edu.ncu.vvaicoding.ai.model.message.ToolExecutedMessage;
import edu.ncu.vvaicoding.ai.model.message.ToolRequestMessage;
import edu.ncu.vvaicoding.cores.parser.CodeParserExecutor;
import edu.ncu.vvaicoding.cores.processor.StreamHandlerExecutor;
import edu.ncu.vvaicoding.cores.processor.StreamTextHandler;
import edu.ncu.vvaicoding.cores.saver.CodeSaverExecutor;
import edu.ncu.vvaicoding.exception.BusinessException;
import edu.ncu.vvaicoding.exception.ErrorCode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

import static edu.ncu.vvaicoding.ai.model.enums.CodeGenTypeEnum.*;

@Service
@Slf4j
public class AICodeGenerateFacade {

    @Resource
    private AIChatModelFactory aiChatModelFactory;

    public Flux<String> generateCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成模式为空");

        AICodeGenerateService aiCodeGenerateService = aiChatModelFactory.getAICodeGenerateService(appId, codeGenTypeEnum);
        log.info(aiCodeGenerateService.toString());
        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> result = aiCodeGenerateService.generateHtmlStreamCode(userMessage);
                yield generateCodeStream(result, HTML, appId);
            }
            case MULTI_FILE -> {
                Flux<String> result = aiCodeGenerateService.generateMultiFileStreamCode(userMessage);
                yield generateCodeStream(result, MULTI_FILE, appId);
            }
            case VUE_PROJECT -> {
                TokenStream result = aiCodeGenerateService.generateVueProjectCodeStream(appId, userMessage);
                yield processTokenStream(result);
            }
        };
    }


    private Flux<String> generateCodeStream(Flux<String> result, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        // 当流式返回生成代码完成后，再保存代码
        StringBuilder codeBuilder = new StringBuilder();
        // 实时收集代码片段
        return result.doOnNext(codeBuilder::append).doOnComplete(() -> {
            // 流式返回完成后保存代码
            try {
                String completeCode = codeBuilder.toString();
                Object codeResult = CodeParserExecutor.parseCode(completeCode, codeGenTypeEnum);
                // 保存代码到文件
                File savedDir = CodeSaverExecutor.codeSave(codeResult, codeGenTypeEnum, appId);
                log.info("保存成功，路径为：{}", savedDir.getAbsolutePath());
            } catch (Exception e) {
                log.error("保存失败: {}", e.getMessage());
            }
        });
    }

    /**
     * 将 TokenStream 转换为 Flux<String>，并传递工具调用信息
     *
     * @param tokenStream TokenStream 对象
     * @return Flux<String> 流式响应
     */
    private Flux<String> processTokenStream(TokenStream tokenStream) {
        return Flux.create(sink -> {
            tokenStream.onPartialResponse((String partialResponse) -> {
                        AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                        sink.next(JSONUtil.toJsonStr(aiResponseMessage));
                    })
                    .onPartialToolExecutionRequest((index, toolExecutionRequest) -> {
                        ToolRequestMessage toolRequestMessage = new ToolRequestMessage(toolExecutionRequest);
                        sink.next(JSONUtil.toJsonStr(toolRequestMessage));
                    })
                    .onToolExecuted((ToolExecution toolExecution) -> {
                        ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution);
                        sink.next(JSONUtil.toJsonStr(toolExecutedMessage));
                    })
                    .onCompleteResponse((ChatResponse response) -> {
                        sink.complete();
                    })
                    .onError((Throwable error) -> {
                        error.printStackTrace();
                        sink.error(error);
                    })
                    .start();
        });
    }

}
