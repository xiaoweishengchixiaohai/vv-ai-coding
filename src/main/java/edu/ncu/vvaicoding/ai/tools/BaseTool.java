package edu.ncu.vvaicoding.ai.tools;

import cn.hutool.json.JSONObject;

public abstract class BaseTool {

    //工具名称(英文)
    public abstract String getToolName();

    //工具名称(中文)
    public abstract String getToolNameCN();

    //完整返回工具信息
    public String getToolDescription() {
        return String.format("\n\n[选择工具] %s \n\n", getToolNameCN());
    }

    //工具调用具体内容
    public abstract String getToolContent(JSONObject arguments);

}
