package edu.ncu.vvaicoding.ai.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONObject;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import edu.ncu.vvaicoding.constant.AppConstant;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
@Component
public class ModifyFileTool extends BaseTool {

    @Tool("修改指定路径的文件内容")
    public String readFile(@P("修改前的内容") String oldContent, @P("修改后的内容") String newContent, @P("文件的相对路径") String relativeFilePath, @ToolMemoryId Long appId) {
        try {
            Path path = Paths.get(relativeFilePath);
            if (!path.isAbsolute()) {
                String projectDirName = "vue_project_" + appId;
                Path projectRoot = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, projectDirName);
                path = projectRoot.resolve(relativeFilePath);
            }
            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                return "错误：文件不存在或不是文件 - " + relativeFilePath;
            }

            File file = FileUtil.file(relativeFilePath);

            String originalContent = FileUtil.readString(file, Charset.defaultCharset());


            if (!originalContent.contains(oldContent)) {
                return "错误：文件中不包含指定的内容 - " + relativeFilePath;
            }

            String content = originalContent.replace(oldContent, newContent);

            if (content.equals(originalContent)) {
                return "错误：文件内容未发生变化 - " + relativeFilePath;
            }

            Files.writeString(path, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            log.info("修改文件内容: {}", relativeFilePath);

            return "修改成功:" + relativeFilePath;
        } catch (IOException e) {
            String errorMessage = "读取文件失败: " + relativeFilePath + ", 错误: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }

    @Override
    public String getToolName() {
        return "";
    }

    @Override
    public String getToolNameCN() {
        return "";
    }

    @Override
    public String getToolContent(JSONObject arguments) {
        return "";
    }
}
