package edu.ncu.vvaicoding.cores;

import com.baomidou.mybatisplus.core.toolkit.Assert;
import edu.ncu.vvaicoding.ai.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AICodeGenerateFacadeTest {

    @Resource
    private AICodeGenerateFacade aiCodeGenerateFacade;

    @Test
    void generateCode() {
        Flux<String> flux = aiCodeGenerateFacade.generateCode("生成一个简单的网页", CodeGenTypeEnum.HTML,1L);
        List<String> block = flux.collectList().block();

        assertNotNull(block);
    }

}