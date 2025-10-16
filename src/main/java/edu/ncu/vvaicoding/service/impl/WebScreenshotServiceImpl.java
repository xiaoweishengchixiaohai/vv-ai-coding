package edu.ncu.vvaicoding.service.impl;

import com.qcloud.cos.model.PutObjectResult;
import edu.ncu.vvaicoding.config.COSClientConfig;
import edu.ncu.vvaicoding.exception.BusinessException;
import edu.ncu.vvaicoding.exception.ErrorCode;
import edu.ncu.vvaicoding.manager.WebScreenshot.WebScreenshotUtils;
import edu.ncu.vvaicoding.manager.cos.COSUpload;
import edu.ncu.vvaicoding.service.WebScreenshotService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class WebScreenshotServiceImpl implements WebScreenshotService {

    @Resource
    private WebScreenshotUtils webScreenshotUtils;

    @Resource
    private COSUpload cosUpload;

    @Resource
    private COSClientConfig cosClientConfig;

    /**
     * 截图方法，用于获取指定URL的网页截图并上传到COS存储
     *
     * @param url 需要截图的网页URL地址
     * @return 返回截图在COS中的存储路径
     * @throws BusinessException 当URL为空或截图过程中出现异常时抛出
     */
    @Override
    public String takeScreenshot(String url) {
        // 参数校验，如果URL为空则抛出业务异常
        if (url == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "URL不能为空");
        }

        // 使用CompletableFuture异步执行截图任务
        CompletableFuture<String> screenshot =
                webScreenshotUtils.takeScreenshot(url);

        try {
            // 获取截图结果
            String path = screenshot.get();

            // 将截图上传到COS并获取上传结果
            PutObjectResult putObjectResult = cosUpload.uploadCover(path);
            // 从上传结果中获取COS中的文件路径
            String COSUrl = putObjectResult.getCiUploadResult().getOriginalInfo().getKey();
            // 记录截图上传成功的日志
            log.info("截图上传成功，url:{}", COSUrl);
            return cosClientConfig.getHost() + "/" +COSUrl;
        } catch (Exception exception) {
            // 记录截图失败的错误日志
            log.error("截图失败", exception);
            // 抛出系统异常，提示截图失败
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "截图失败");
        }
    }

}
