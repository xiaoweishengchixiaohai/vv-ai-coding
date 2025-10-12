package edu.ncu.vvaicoding.cores.saver;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import edu.ncu.vvaicoding.ai.model.enums.CodeGenTypeEnum;
import edu.ncu.vvaicoding.constant.AppConstant;

import java.io.File;

public abstract class CodeSaverTemplate<T> {

    // 文件保存根目录
    protected static final String FILE_SAVE_ROOT_DIR = AppConstant.CODE_OUTPUT_ROOT_DIR;

    public final File saveFile(T codeResult,Long appId) {
        //参数校验
        validCodeResult(codeResult);
        //创建文件夹
        String path = createDir(appId);
        //保存文件
        saveFile(path, codeResult);

        return new File(path);
    }

    protected void validCodeResult(T codeResult) {
        if (codeResult == null) throw new IllegalArgumentException("代码生成结果为空");
    }

    private String createDir(Long appId) {
        String codeType = codeGenTypeEnum().getValue();
        String path = FILE_SAVE_ROOT_DIR + "/" + codeType + "_" + appId;

        FileUtil.mkdir(path);

        return path;
    }

    protected abstract void saveFile(String path, T codeResult);

    protected abstract CodeGenTypeEnum codeGenTypeEnum();


}
