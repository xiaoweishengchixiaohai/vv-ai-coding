package edu.ncu.vvaicoding.cores.saver;

import cn.hutool.core.io.FileUtil;
import edu.ncu.vvaicoding.ai.model.MultiFileCodeResult;
import edu.ncu.vvaicoding.ai.model.enums.CodeGenTypeEnum;

public class MultiCodeSaver extends CodeSaverTemplate<MultiFileCodeResult> {


    @Override
    protected void saveFile(String path, MultiFileCodeResult codeResult) {
        FileUtil.writeString(codeResult.getHtmlCode(), path + "/index.html", "UTF-8");
        FileUtil.writeString(codeResult.getCssCode(), path + "/style.css", "UTF-8");
        FileUtil.writeString(codeResult.getJsCode(), path + "/script.js", "UTF-8");
    }

    @Override
    protected CodeGenTypeEnum codeGenTypeEnum() {
        return CodeGenTypeEnum.MULTI_FILE;
    }
}
