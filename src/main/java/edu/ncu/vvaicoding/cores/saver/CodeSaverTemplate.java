package edu.ncu.vvaicoding.cores.saver;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import edu.ncu.vvaicoding.ai.model.enums.CodeGenTypeEnum;

import java.io.File;

public abstract class CodeSaverTemplate<T> {

    public static final String FILE_SAVE_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_output";

    public final File saveFile(T codeResult) {
        //参数校验
        validCodeResult(codeResult);
        //创建文件夹
        String path = createDir();
        //保存文件
        saveFile(path, codeResult);

        return new File(path);
    }

    protected void validCodeResult(T codeResult) {
        if (codeResult == null) throw new IllegalArgumentException("代码生成结果为空");
    }

    private String createDir() {
        String codeType = codeGenTypeEnum().getValue();
        String path = FILE_SAVE_ROOT_DIR + "/" + codeType + "_" + IdUtil.getSnowflakeNextId();

        FileUtil.mkdir(path);

        return path;
    }

    protected abstract void saveFile(String path, T codeResult);

    protected abstract CodeGenTypeEnum codeGenTypeEnum();


}
