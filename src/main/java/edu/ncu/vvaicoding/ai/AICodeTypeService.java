package edu.ncu.vvaicoding.ai;

import dev.langchain4j.service.SystemMessage;
import edu.ncu.vvaicoding.ai.model.enums.CodeGenTypeEnum;

public interface AICodeTypeService {

    @SystemMessage(fromResource = "prompt/code-type.txt")
    CodeGenTypeEnum codeType(String userMessage);
}
