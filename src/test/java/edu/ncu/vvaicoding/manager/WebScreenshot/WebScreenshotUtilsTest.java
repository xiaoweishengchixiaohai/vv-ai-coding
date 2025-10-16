package edu.ncu.vvaicoding.manager.WebScreenshot;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class WebScreenshotUtilsTest {

    @Resource
    private WebScreenshotUtils webScreenshotUtils;

    @Test
    void generateWebPageScreenshot() throws ExecutionException, InterruptedException {
        String url = "https://www.baidu.com";

        String s = webScreenshotUtils.takeScreenshot(url).get();

        Assertions.assertNotNull(s);

    }
}