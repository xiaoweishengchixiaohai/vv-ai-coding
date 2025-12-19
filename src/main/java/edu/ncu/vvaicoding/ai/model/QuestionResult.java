package edu.ncu.vvaicoding.ai.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

@Data
@Description("问题结果")
public class QuestionResult {

    @Description("问题")
    private String question;

    @Description("答案")
    private String answer;
}
