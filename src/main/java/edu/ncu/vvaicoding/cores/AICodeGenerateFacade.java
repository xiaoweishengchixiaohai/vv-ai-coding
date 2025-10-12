package edu.ncu.vvaicoding.cores;

import edu.ncu.vvaicoding.ai.AICodeGenerateService;
import edu.ncu.vvaicoding.ai.model.HtmlCodeResult;
import edu.ncu.vvaicoding.ai.model.MultiFileCodeResult;
import edu.ncu.vvaicoding.ai.model.enums.CodeGenTypeEnum;
import edu.ncu.vvaicoding.cores.parser.CodeParserExecutor;
import edu.ncu.vvaicoding.cores.saver.CodeSaverExecutor;
import edu.ncu.vvaicoding.exception.BusinessException;
import edu.ncu.vvaicoding.exception.ErrorCode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

import static edu.ncu.vvaicoding.ai.model.enums.CodeGenTypeEnum.HTML;
import static edu.ncu.vvaicoding.ai.model.enums.CodeGenTypeEnum.MULTI_FILE;

@Service
@Slf4j
public class AICodeGenerateFacade {

    @Resource
    private AICodeGenerateService aICodeGenerateService;


    public Flux<String> generateCode(String userManager, CodeGenTypeEnum codeGenTypeEnum) {
        if (codeGenTypeEnum == null) throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成模式为空");

        return switch (codeGenTypeEnum) {
            case HTML -> generateAndSaveHtmlCodeStream(userManager);
            case MULTI_FILE -> generateAndSaveMultiFileCodeStream(userManager);
        };
    }

    private File generateHtmlCode(String userManager) {
        HtmlCodeResult htmlCodeResult = aICodeGenerateService.generateHtmlCode(userManager);

        return CodeSaverExecutor.codeSave(htmlCodeResult, HTML);
    }

    private File generateMultiFileCode(String userManager) {
        MultiFileCodeResult multiFileCodeResult = aICodeGenerateService.generateMultiFileCode(userManager);

        return CodeSaverExecutor.codeSave(multiFileCodeResult, MULTI_FILE);
    }



    /**
     * 生成 HTML 模式的代码并保存（流式）
     *
     * @param userMessage 用户提示词
     * @return 保存的目录
     */
    private Flux<String> generateAndSaveHtmlCodeStream(String userMessage) {
        Flux<String> result = aICodeGenerateService.generateHtmlStreamCode(userMessage);
        return generateCodeStream(result, HTML);
    }

    /**
     * 生成多文件模式的代码并保存（流式）
     *
     * @param userMessage 用户提示词
     * @return 保存的目录
     */
    private Flux<String> generateAndSaveMultiFileCodeStream(String userMessage) {
        Flux<String> result = aICodeGenerateService.generateMultiFileStreamCode(userMessage);
        return generateCodeStream(result, MULTI_FILE);
    }

    private Flux<String> generateCodeStream(Flux<String> result, CodeGenTypeEnum codeGenTypeEnum) {
        // 当流式返回生成代码完成后，再保存代码
        StringBuilder codeBuilder = new StringBuilder();
        // 实时收集代码片段
        return result.doOnNext(codeBuilder::append).doOnComplete(() -> {
            // 流式返回完成后保存代码
            try {
                String completeCode = codeBuilder.toString();
                Object codeResult = CodeParserExecutor.parseCode(completeCode, codeGenTypeEnum);
                // 保存代码到文件
                File savedDir = CodeSaverExecutor.codeSave(codeResult, codeGenTypeEnum);
                log.info("保存成功，路径为：{}", savedDir.getAbsolutePath());
            } catch (Exception e) {
                log.error("保存失败: {}", e.getMessage());
            }
        });
    }
}
