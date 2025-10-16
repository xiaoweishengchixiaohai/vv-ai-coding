package edu.ncu.vvaicoding.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import edu.ncu.vvaicoding.ai.model.HtmlCodeResult;
import edu.ncu.vvaicoding.ai.model.MultiFileCodeResult;
import reactor.core.publisher.Flux;

public interface AICodeGenerateService {

    /**
     * 根据用户消息生成HTML代码的方法
     * 该方法使用系统消息提示，从资源文件"prompt/single-file-native-code.txt"中加载提示内容
     *
     * @param userMessage 用户输入的消息，用于生成对应的HTML代码
     * @return HtmlCodeResult 包含生成HTML代码的结果对象
     */

    @SystemMessage(fromResource = "prompt/single-file-native-code.txt")
    HtmlCodeResult generateHtmlCode(String userMessage);

    @SystemMessage(fromResource = "prompt/multi-file-native-code.txt")
    MultiFileCodeResult generateMultiFileCode(String userMessage);

    @SystemMessage(fromResource = "prompt/single-file-native-code.txt")
    Flux<String> generateHtmlStreamCode(String userMessage);

    @SystemMessage(fromResource = "prompt/multi-file-native-code.txt")
    Flux<String> generateMultiFileStreamCode(String userMessage);

    /**
     * 生成 Vue 项目代码（流式）
     *
     * @param userMessage 用户消息
     * @return 生成过程的流式响应
     */
    @SystemMessage(fromResource = "prompt/vue-project-code.txt")
    TokenStream generateVueProjectCodeStream(@MemoryId long appId, @UserMessage String userMessage);

}
