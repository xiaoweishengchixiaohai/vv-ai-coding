package edu.ncu.vvaicoding.cores.saver;

import cn.hutool.core.io.FileUtil;
import edu.ncu.vvaicoding.ai.model.HtmlCodeResult;
import edu.ncu.vvaicoding.ai.model.enums.CodeGenTypeEnum;

public class HtmlCodeSaver extends CodeSaverTemplate<HtmlCodeResult> {

    @Override
    protected void saveFile(String path, HtmlCodeResult codeResult) {
        FileUtil.writeString(codeResult.getHtmlCode(), path + "/index.html", "UTF-8");
    }

    @Override
    protected CodeGenTypeEnum codeGenTypeEnum() {
        return CodeGenTypeEnum.HTML;
    }
}
