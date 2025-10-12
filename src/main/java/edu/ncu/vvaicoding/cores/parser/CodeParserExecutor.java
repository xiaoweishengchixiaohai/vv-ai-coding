package edu.ncu.vvaicoding.cores.parser;

import edu.ncu.vvaicoding.ai.model.enums.CodeGenTypeEnum;

public class CodeParserExecutor {

    public static final HtmlParser htmlParser = new HtmlParser();

    public static final MultiParser multiParser = new MultiParser();

    public static Object parseCode(String codeContent, CodeGenTypeEnum codeGenTypeEnum) {

        if (codeGenTypeEnum == null) throw new IllegalArgumentException("生成类型为空");

        return switch (codeGenTypeEnum) {
            case HTML -> htmlParser.parseCode(codeContent);
            case MULTI_FILE -> multiParser.parseCode(codeContent);
        };
    }
}
