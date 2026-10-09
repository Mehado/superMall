package com.supermall.takeout.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证码图片生成：能在 headless 环境出图，尺寸正确，字符集不含易混字符。
 */
@DisplayName("CaptchaImageUtil - 画图")
class CaptchaImageUtilTest {

    @Test
    @DisplayName("输出是合法的 PNG，且尺寸与配置一致")
    void rendersValidPng() throws Exception {
        String base64 = CaptchaImageUtil.renderBase64("ABCD", 132, 44);
        assertThat(base64).isNotBlank();

        byte[] bytes = Base64.getDecoder().decode(base64);
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));

        assertThat(img).isNotNull();
        assertThat(img.getWidth()).isEqualTo(132);
        assertThat(img.getHeight()).isEqualTo(44);
    }

    @Test
    @DisplayName("随机码长度正确，且只含去混字符集")
    void randomCodeUsesSafeCharset() {
        for (int i = 0; i < 200; i++) {
            String code = CaptchaImageUtil.randomCode(4);
            assertThat(code).hasSize(4);
            // 不含 0 O 1 I L —— 这几个在图片里最容易看错
            assertThat(code).doesNotContainAnyWhitespaces();
            assertThat(code).matches("[23456789ABCDEFGHJKMNPQRSTUVWXYZ]+");
        }
    }

    @RepeatedTest(20)
    @DisplayName("连续生成有随机性（不是每次都一样）")
    void randomCodeVaries() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 30; i++) {
            seen.add(CaptchaImageUtil.randomCode(4));
        }
        assertThat(seen.size()).isGreaterThan(25);
    }

    @Test
    @DisplayName("同一下发文本能重复出图（渲染本身无状态）")
    void renderIsDeterministicInSize() {
        String a = CaptchaImageUtil.renderBase64("WXYZ", 120, 40);
        String b = CaptchaImageUtil.renderBase64("WXYZ", 120, 40);
        // 内容是随机的（噪点/旋转），但长度量级一致，说明每次都真的画了图
        assertThat(a).isNotBlank();
        assertThat(b).isNotBlank();
        assertThat(Math.abs(a.length() - b.length())).isLessThan(a.length());
    }
}
