package com.supermall.utils;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;

/**
 * 阿里云 OSS 工具。
 * <p>
 * 商品图片放 OSS 的好处：项目拷贝到别的机器 / 换数据库后，图片仍然是同一批 URL，不用跟着仓库搬文件。
 */
@Slf4j
public class AliOssUtil {

    private final String endpoint;
    private final String accessKeyId;
    private final String accessKeySecret;
    private final String bucketName;

    public AliOssUtil(String endpoint, String accessKeyId, String accessKeySecret, String bucketName) {
        this.endpoint = endpoint;
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
        this.bucketName = bucketName;
    }

    /**
     * 上传字节数组到 OSS，返回可公网访问的 URL。
     *
     * @param bytes       文件内容
     * @param objectName  OSS 对象名（如 supermall/products/xxx.svg）
     * @param contentType MIME 类型，必须设置正确，否则浏览器会当成附件下载
     */
    public String upload(byte[] bytes, String objectName, String contentType) {
        ClientBuilderConfiguration conf = new ClientBuilderConfiguration();
        conf.setConnectionTimeout(10_000);
        conf.setSocketTimeout(30_000);
        OSS client = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret, conf);
        try {
            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentLength(bytes.length);
            if (contentType != null && !contentType.isBlank()) {
                meta.setContentType(contentType);
            }
            // 图片内联展示，避免被当成附件下载
            meta.setContentDisposition("inline");
            meta.setCacheControl("public, max-age=31536000");
            client.putObject(new PutObjectRequest(bucketName, objectName, new ByteArrayInputStream(bytes), meta));
            String url = "https://" + bucketName + "." + endpoint + "/" + objectName;
            log.info("OSS 上传成功：{}", url);
            return url;
        } finally {
            client.shutdown();
        }
    }
}
