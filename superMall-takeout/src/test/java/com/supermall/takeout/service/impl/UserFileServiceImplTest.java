package com.supermall.takeout.service.impl;

import com.supermall.constant.MessageConstant;
import com.supermall.properties.AliOssProperties;
import com.supermall.takeout.exception.ProductBusinessException;
import com.supermall.utils.AliOssUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 外卖买家头像上传：类型 / 大小 / 目录 的校验。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UserFileServiceImpl - 头像上传")
class UserFileServiceImplTest {

    private static final Long USER_ID = 7L;

    @Mock
    private AliOssUtil aliOssUtil;

    private AliOssProperties props;
    private UserFileServiceImpl service;

    @BeforeEach
    void setUp() {
        props = new AliOssProperties();
        props.setAvatarDir("supermall/avatars/");
        service = new UserFileServiceImpl(aliOssUtil, props);
    }

    private MockMultipartFile file(String name, String contentType, int size) {
        return new MockMultipartFile("file", name, contentType, new byte[size]);
    }

    @Test
    @DisplayName("空文件 → 拒绝")
    void emptyFile_isRejected() {
        assertThatThrownBy(() -> service.uploadAvatar(null, USER_ID))
                .isInstanceOf(ProductBusinessException.class)
                .hasMessageContaining(MessageConstant.AVATAR_EMPTY);
        verify(aliOssUtil, never()).upload(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("非图片类型 → 拒绝")
    void nonImage_isRejected() {
        assertThatThrownBy(() -> service.uploadAvatar(file("a.txt", "text/plain", 10), USER_ID))
                .isInstanceOf(ProductBusinessException.class)
                .hasMessageContaining(MessageConstant.AVATAR_TYPE_INVALID);
        verify(aliOssUtil, never()).upload(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("超过 2MB → 拒绝")
    void tooLarge_isRejected() {
        MockMultipartFile big = file("big.png", "image/png", (int) (2 * 1024 * 1024 + 1));
        assertThatThrownBy(() -> service.uploadAvatar(big, USER_ID))
                .isInstanceOf(ProductBusinessException.class)
                .hasMessageContaining(MessageConstant.AVATAR_TOO_LARGE);
        verify(aliOssUtil, never()).upload(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("扩展名不在白名单（伪装成 image/png 的 .exe）→ 拒绝")
    void badExtension_isRejected() {
        assertThatThrownBy(() -> service.uploadAvatar(file("evil.exe", "image/png", 10), USER_ID))
                .isInstanceOf(ProductBusinessException.class)
                .hasMessageContaining(MessageConstant.AVATAR_TYPE_INVALID);
    }

    @Test
    @DisplayName("合法图片 → 按 userId 分目录上传，返回 URL")
    void validImage_uploadsToUserDir() {
        when(aliOssUtil.upload(any(), anyString(), anyString()))
                .thenReturn("https://oss.example.com/supermall/avatars/7/x.png");

        String url = service.uploadAvatar(file("me.png", "image/png", 1024), USER_ID);

        ArgumentCaptor<String> objectName = ArgumentCaptor.forClass(String.class);
        verify(aliOssUtil).upload(any(), objectName.capture(), anyString());
        assertThat(objectName.getValue()).startsWith("supermall/avatars/7/");
        assertThat(objectName.getValue()).endsWith(".png");
        assertThat(url).isEqualTo("https://oss.example.com/supermall/avatars/7/x.png");
    }
}
