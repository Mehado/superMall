package com.supermall.mall.service.impl;

import com.supermall.constant.MessageConstant;
import com.supermall.mall.exception.MallBusinessException;
import com.supermall.properties.AliOssProperties;
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
 * 电商买家头像上传：类型 / 大小 / 目录 的校验。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MallFileServiceImpl - 头像上传")
class MallFileAvatarTest {

    private static final Long USER_ID = 12L;

    @Mock
    private AliOssUtil aliOssUtil;

    private AliOssProperties props;
    private MallFileServiceImpl service;

    @BeforeEach
    void setUp() {
        props = new AliOssProperties();
        props.setDir("supermall/mall/");
        props.setAvatarDir("supermall/avatars/");
        service = new MallFileServiceImpl(aliOssUtil, props);
    }

    private MockMultipartFile file(String name, String contentType, int size) {
        return new MockMultipartFile("file", name, contentType, new byte[size]);
    }

    @Test
    @DisplayName("空文件 → 拒绝")
    void emptyFile_isRejected() {
        assertThatThrownBy(() -> service.uploadAvatar(null, USER_ID))
                .isInstanceOf(MallBusinessException.class)
                .hasMessageContaining(MessageConstant.AVATAR_EMPTY);
        verify(aliOssUtil, never()).upload(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("非图片类型 → 拒绝")
    void nonImage_isRejected() {
        assertThatThrownBy(() -> service.uploadAvatar(file("a.txt", "text/plain", 10), USER_ID))
                .isInstanceOf(MallBusinessException.class)
                .hasMessageContaining(MessageConstant.AVATAR_TYPE_INVALID);
    }

    @Test
    @DisplayName("超过 2MB → 拒绝")
    void tooLarge_isRejected() {
        MockMultipartFile big = file("big.jpg", "image/jpeg", (int) (2 * 1024 * 1024 + 1));
        assertThatThrownBy(() -> service.uploadAvatar(big, USER_ID))
                .isInstanceOf(MallBusinessException.class)
                .hasMessageContaining(MessageConstant.AVATAR_TOO_LARGE);
    }

    @Test
    @DisplayName("合法图片 → 进头像目录（不是商品目录），扩展名只带一个点")
    void validImage_usesAvatarDir() {
        when(aliOssUtil.upload(any(), anyString(), anyString()))
                .thenReturn("https://oss.example.com/supermall/avatars/12/x.jpg");

        String url = service.uploadAvatar(file("me.JPG", "image/jpeg", 2048), USER_ID);

        ArgumentCaptor<String> objectName = ArgumentCaptor.forClass(String.class);
        verify(aliOssUtil).upload(any(), objectName.capture(), anyString());
        String key = objectName.getValue();
        assertThat(key).startsWith("supermall/avatars/12/");
        assertThat(key).doesNotContain("supermall/mall/");
        assertThat(key).doesNotContain("..");
        assertThat(key).endsWith(".jpg");
        assertThat(url).endsWith("x.jpg");
    }
}
