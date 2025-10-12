package edu.ncu.vvaicoding.cores.saver;

import edu.ncu.vvaicoding.ai.model.HtmlCodeResult;
import edu.ncu.vvaicoding.ai.model.MultiFileCodeResult;
import edu.ncu.vvaicoding.ai.model.enums.CodeGenTypeEnum;

import java.io.File;

public class CodeSaverExecutor {

    public static final MultiCodeSaver multiFileCodeSaver = new MultiCodeSaver();

    public static final HtmlCodeSaver singleFileCodeSaver = new HtmlCodeSaver();

    public static File codeSave(Object codeResult, CodeGenTypeEnum codeGenTypeEnum,Long appId) {

        return switch (codeGenTypeEnum) {
            case HTML -> singleFileCodeSaver.saveFile((HtmlCodeResult) codeResult,appId);
            case MULTI_FILE -> multiFileCodeSaver.saveFile((MultiFileCodeResult) codeResult,appId);
        };
    }
}
