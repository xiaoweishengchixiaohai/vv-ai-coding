package edu.ncu.vvaicoding.manager.cos;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import edu.ncu.vvaicoding.config.COSClientConfig;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.UUID;

@Component
public class COSUpload {

    @Resource
    private COSClient cosClient;

    @Resource
    private COSClientConfig cosClientConfig;

    private final static String PREFIX = "avatar/";

    public PutObjectResult upload(File file) {
        String type = FileUtil.getType(file);
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucketName(),
                PREFIX + UUID.randomUUID() + "." + type, file);

        PicOperations picOperations = new PicOperations();

        picOperations.setIsPicInfo(1);
        putObjectRequest.setPicOperations(picOperations);

        return cosClient.putObject(putObjectRequest);
    }

    public boolean delete(String oldUrl) {
        cosClient.deleteObject(cosClientConfig.getBucketName(), StrUtil.removePrefix(oldUrl, cosClientConfig.getHost() + "/"));

        return true;
    }
}
