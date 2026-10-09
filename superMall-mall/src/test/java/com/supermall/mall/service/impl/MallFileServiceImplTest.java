package com.supermall.mall.service.impl;

import com.supermall.mall.exception.MallBusinessException;
import com.supermall.properties.AliOssProperties;
import com.supermall.utils.AliOssUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 文件上传：空文件 / 批量上限 / 文件名生成（不真连 OSS）。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MallFileServiceImpl - 图片上传")
class MallFileServiceImplTest {

    @Mock private AliOssUtil aliOssUtil;

    private MallFileServiceImpl service;

    @BeforeEach
    void setUp() {
        AliOssProperties props = new AliOssProperties();
        props.setDir("supermall/mall/");
        service = new MallFileServiceImpl(aliOssUtil, props);
        when(aliOssUtil.upload(any(), anyString(), any())).thenReturn("https://oss/x.png");
    }

    private MockMultipartFile png(String name) {
        return new MockMultipartFile("file", name, "image/png", new byte[]{1, 2, 3});
    }

    @Test
    @DisplayName("上传单张：文件名用 UUID + 原扩展名，目录取自配置")
    void uploadGeneratesName() {
        String url = service.upload(png("a.PNG"), null);

        assertThat(url).isEqualTo("https://oss/x.png");
        var objectName = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(aliOssUtil).upload(any(), objectName.capture(), eq("image/png"));
        assertThat(objectName.getValue()).startsWith("supermall/mall/").endsWith(".png");
    }

    @Test
    @DisplayName("上传单张：传了 name 就用 name")
    void uploadKeepsName() {
        service.upload(png("a.png"), "  custom.png  ");
        verify(aliOssUtil).upload(any(), eq("supermall/mall/custom.png"), any());
    }

    @Test
    @DisplayName("上传单张：空文件 → 拒绝，不调 OSS")
    void uploadEmpty() {
        MockMultipartFile empty = new MockMultipartFile("file", "e.png", "image/png", new byte[0]);
        assertThatThrownBy(() -> service.upload(empty, null))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage("上传的文件为空");
        verify(aliOssUtil, never()).upload(any(), anyString(), any());
    }

    @Test
    @DisplayName("批量：空数组 → 拒绝")
    void batchEmpty() {
        assertThatThrownBy(() -> service.uploadBatch(new MockMultipartFile[0], 9))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage("上传的文件为空");
    }

    @Test
    @DisplayName("批量：超过上限 → 拒绝（买家 9 张）")
    void batchTooMany() {
        MockMultipartFile[] files = new MockMultipartFile[10];
        for (int i = 0; i < files.length; i++) {
            files[i] = png(i + ".png");
        }
        assertThatThrownBy(() -> service.uploadBatch(files, 9))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage("一次最多上传 9 张");
        verify(aliOssUtil, never()).upload(any(), anyString(), any());
    }

    @Test
    @DisplayName("批量：正常返回与入参等长的 URL 列表")
    void batchOk() {
        var urls = service.uploadBatch(new MockMultipartFile[]{png("1.png"), png("2.png")}, 9);
        assertThat(urls).containsExactly("https://oss/x.png", "https://oss/x.png");
    }
}
