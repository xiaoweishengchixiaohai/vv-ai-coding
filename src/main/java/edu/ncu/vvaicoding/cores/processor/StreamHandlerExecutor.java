package edu.ncu.vvaicoding.cores.processor;


import edu.ncu.vvaicoding.ai.model.enums.CodeGenTypeEnum;
import edu.ncu.vvaicoding.domain.VO.UserVO;
import edu.ncu.vvaicoding.service.ChatHistoryService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class StreamHandlerExecutor {

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private StreamTextHandler streamTextHandler;

    @Resource
    private JSONMessageStreamHandler jsonMessageStreamHandler;

    public Flux<String> process(CodeGenTypeEnum codeGenTypeEnum, Flux<String> result, Long appId, UserVO user) {
        return switch (codeGenTypeEnum) {
            case HTML, MULTI_FILE -> streamTextHandler.handle(result, chatHistoryService, appId, user);
            case VUE_PROJECT -> jsonMessageStreamHandler.handle(result, chatHistoryService, appId, user);
        };
    }
}
