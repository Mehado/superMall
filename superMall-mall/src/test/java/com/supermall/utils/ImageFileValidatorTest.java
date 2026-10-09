package com.supermall.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 图片上传校验（{@link ImageFileValidator}）。
 *
 * <p>背景：商品图上传原来**只受 multipart 总大小限制**，实测能把带 `onload=alert(1)` 的
 * `.svg` 传上 OSS 并拿到公网 URL —— 这里把"类型白名单 + 大小上限"卡死。
 */
@DisplayName("ImageFileValidator - 上传白名单与大小")
class ImageFileValidatorTest {

    private static MockMultipartFile file(String name, String contentType, int size) {
        return new MockMultipartFile("file", name, contentType, new byte[size]);
    }

    @Test
    @DisplayName("正常图片通过（jpg / png / webp 都行）")
    void acceptsNormalImages() {
        assertThat(ImageFileValidator.check(file("a.jpg", "image/jpeg", 1024), 5 * 1024 * 1024)).isNull();
        assertThat(ImageFileValidator.check(file("b.PNG", "image/png", 1024), 5 * 1024 * 1024)).isNull();
        assertThat(ImageFileValidator.check(file("c.webp", "image/webp", 1024), 5 * 1024 * 1024)).isNull();
    }

    @Test
    @DisplayName("SVG 被拒（可带脚本，是存储型 XSS 的经典入口）")
    void rejectsSvg() {
        String msg = ImageFileValidator.check(file("evil.svg", "image/svg+xml", 512), 5 * 1024 * 1024);
        assertThat(msg).contains("格式不支持");
    }

    @Test
    @DisplayName("content-type 不是 image/* 的被拒（挡住 .html/.jsp 伪装）")
    void rejectsNonImageContentType() {
        assertThat(ImageFileValidator.check(file("a.png", "text/html", 512), 5 * 1024 * 1024))
                .contains("只支持图片文件");
        assertThat(ImageFileValidator.check(file("a.png", null, 512), 5 * 1024 * 1024))
                .contains("只支持图片文件");
    }

    @Test
    @DisplayName("没有扩展名的被拒")
    void rejectsNoExtension() {
        assertThat(ImageFileValidator.check(file("noext", "image/png", 512), 5 * 1024 * 1024))
                .contains("格式不支持");
    }

    @Test
    @DisplayName("超过上限被拒，提示里带 MB 数")
    void rejectsTooLarge() {
        String msg = ImageFileValidator.check(file("big.png", "image/png", 6 * 1024 * 1024), 5 * 1024 * 1024);
        assertThat(msg).contains("5MB");
        // 头像上限更紧：2MB
        assertThat(ImageFileValidator.check(file("a.png", "image/png", 3 * 1024 * 1024),
                ImageFileValidator.MAX_AVATAR_BYTES)).contains("2MB");
    }

    @Test
    @DisplayName("空文件被拒")
    void rejectsEmpty() {
        assertThat(ImageFileValidator.check(null, 5 * 1024 * 1024)).contains("为空");
        assertThat(ImageFileValidator.check(file("a.png", "image/png", 0), 5 * 1024 * 1024)).contains("为空");
    }

    @Test
    @DisplayName("extName：取小写扩展名，取不到返回空串")
    void extName() {
        assertThat(ImageFileValidator.extName(file("A.JPEG", "image/jpeg", 1))).isEqualTo("jpeg");
        assertThat(ImageFileValidator.extName(file("noext", "image/png", 1))).isEmpty();
        assertThat(ImageFileValidator.extName(file("trailingdot.", "image/png", 1))).isEmpty();
    }
}
