package com.supermall.takeout.controller.merchant;

import com.supermall.takeout.exception.ProductBusinessException;
import com.supermall.utils.ImageFileValidator;
import com.supermall.properties.AliOssProperties;
import com.supermall.result.Result;
import com.supermall.utils.AliOssUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * 商家端文件上传：商品图片直接进 OSS，返回可公网访问的 URL。
 * 图片不落在仓库里，项目换机器 / 换库后老图片依然能访问。
 */
@RestController
@RequestMapping("/api/merchant/file")
@Tag(name = "商家端-文件上传")
@Slf4j
@RequiredArgsConstructor
public class FileController {

    private final AliOssUtil aliOssUtil;
    private final AliOssProperties ossProperties;

    @PostMapping("/upload")
    @Operation(summary = "上传商品图片到 OSS")
    public Result<String> upload(@RequestParam("file") MultipartFile file,
                                 @RequestParam(value = "name", required = false) String name) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ProductBusinessException("上传的文件为空");
        }
        // 类型白名单 + 大小上限（原来只受 multipart 总大小限制，SVG 这种能带脚本的也能传上来）
        String bad = ImageFileValidator.check(file, ImageFileValidator.MAX_PRODUCT_IMAGE_BYTES);
        if (bad != null) {
            throw new ProductBusinessException(bad);
        }
        String objectName = ossProperties.getDir() + (hasText(name) ? name : UUID.randomUUID() + extOf(file));
        String url = aliOssUtil.upload(file.getBytes(), objectName, file.getContentType());
        log.info("商家上传图片：{} -> {}", file.getOriginalFilename(), url);
        return Result.success(url);
    }

    private boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private String extOf(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original != null) {
            int i = original.lastIndexOf('.');
            if (i > -1) {
                return original.substring(i).toLowerCase();
            }
        }
        String ct = file.getContentType();
        if (ct != null && ct.contains("/")) {
            return "." + ct.substring(ct.indexOf('/') + 1);
        }
        return ".jpg";
    }
}
