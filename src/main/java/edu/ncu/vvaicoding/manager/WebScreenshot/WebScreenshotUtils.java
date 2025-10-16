package edu.ncu.vvaicoding.manager.WebScreenshot;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Validator;
import edu.ncu.vvaicoding.constant.AppConstant;
import edu.ncu.vvaicoding.exception.BusinessException;
import edu.ncu.vvaicoding.exception.ErrorCode;
import io.github.bonigarcia.wdm.WebDriverManager;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Component
@Slf4j
public class WebScreenshotUtils {

    private static final WebDriver webDriver;

    private final Executor executor = Executors.newSingleThreadExecutor();

    static {
        final int DEFAULT_WIDTH = 1600;
        final int DEFAULT_HEIGHT = 900;
        System.setProperty("wdm.chromeDriverMirrorUrl", "https://registry.npmmirror.com/binary.html?path=chromedriver");
        webDriver = initChromeDriver(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public CompletableFuture<String> takeScreenshot(String url) {
        return CompletableFuture.supplyAsync(() -> generateWebPageScreenshot(url), executor);
    }

    private static String generateWebPageScreenshot(String webUrl) {
        //校验参数
        boolean urlValid = Validator.isUrl(webUrl);

        if (!urlValid) {
            log.error("URL格式不正确");
            return null;
        }
        File tempFile = new File(AppConstant.SCREEN_SHOT_DIR);
        try {
            //打开浏览器
            webDriver.get(webUrl);
            //等待页面加载
            waitForPageLoad();
            //截图
            byte[] screenshot = ((TakesScreenshot) webDriver).getScreenshotAs(OutputType.BYTES);

            //构造路径
            String path = AppConstant.SCREEN_SHOT_DIR + File.separator +
                    DateTime.now().toString("yyyy/MM/dd") + File.separator + UUID.randomUUID().toString().substring(0, 8) + ".png";

            String compressor_path = AppConstant.SCREEN_SHOT_DIR + File.separator +
                    DateTime.now().toString("yyyy/MM/dd") + File.separator + UUID.randomUUID().toString().substring(0, 8) + ".png";

            //保存截图
            FileUtil.writeBytes(screenshot, path);
            //压缩图片
            ImgUtil.compress(FileUtil.file(path), FileUtil.file(compressor_path), 0.5f);
            FileUtil.del(path);
            //返回压缩图片信息
            return compressor_path;

        } catch (Exception e) {
            log.error("截图失败", e);
            return null;
        }

    }

    private static void waitForPageLoad() {
        try {
            WebDriverWait driverWait = new WebDriverWait(webDriver, Duration.ofSeconds(10));

            driverWait.until(driver -> Objects.equals(((JavascriptExecutor) driver).executeScript("return document.readyState"), "complete"));

            Thread.sleep(2000);
            log.info("");
        } catch (InterruptedException e) {
            log.error("等待页面加载失败", e);
            throw new RuntimeException(e);
        }
    }


    @PreDestroy
    public void destroy() {
        webDriver.quit();
    }

    /**
     * 初始化WebDriver的方法
     * 该方法用于创建并配置一个WebDriver实例，通常用于浏览器自动化测试
     *
     * @return 返回一个配置好的WebDriver实例
     */

    private static WebDriver initChromeDriver(int width, int height) {
        try {
            // 自动管理 ChromeDriver
            WebDriverManager.chromedriver().useMirror().setup();
            // 配置 Chrome 选项
            ChromeOptions options = new ChromeOptions();
            // 无头模式
            options.addArguments("--headless");
            // 禁用GPU（在某些环境下避免问题）
            options.addArguments("--disable-gpu");
            // 禁用沙盒模式（Docker环境需要）
            options.addArguments("--no-sandbox");
            // 禁用开发者shm使用
            options.addArguments("--disable-dev-shm-usage");
            // 设置窗口大小
            options.addArguments(String.format("--window-size=%d,%d", width, height));
            // 禁用扩展
            options.addArguments("--disable-extensions");
            // 设置用户代理
            options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            // 创建驱动
            WebDriver driver = new ChromeDriver(options);
            // 设置页面加载超时
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            // 设置隐式等待
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            return driver;
        } catch (Exception e) {
            log.error("初始化 Chrome 浏览器失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "初始化 Chrome 浏览器失败");
        }
    }
}
