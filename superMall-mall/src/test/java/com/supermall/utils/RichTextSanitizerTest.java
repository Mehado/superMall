package com.supermall.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 富文本 / 纯文本 XSS 清洗（{@link RichTextSanitizer}）。
 *
 * <p>这里卡的是「需求条目：对用户输入的富文本做 XSS 过滤」——
 * 重点是**别过滤过头**：正常文字一个字符都不能动（否则前端再转义一次会露出 `&lt;`）。
 */
@DisplayName("RichTextSanitizer - 富文本 XSS 清洗")
class RichTextSanitizerTest {

    /* ---------------- 富文本：白名单保留 + 危险标签丢弃 ---------------- */

    @Test
    @DisplayName("保留安全的排版标签（p / br / strong / ul-li / h1）")
    void richText_keepsSafeTags() {
        String out = RichTextSanitizer.richText(
                "<h1>纯棉T恤</h1><p>100% 精梳棉<br>亲肤透气</p><ul><li>不起球</li></ul><strong>限时</strong>");
        assertThat(out).contains("<h1>", "<p>", "<br>", "<ul>", "<li>", "<strong>");
    }

    @Test
    @DisplayName("script / iframe / style 一律丢弃（连标签带内容）")
    void richText_dropsDangerousTags() {
        String out = RichTextSanitizer.richText(
                "<p>安全内容</p><script>alert(1)</script><iframe src=\"//evil\"></iframe><style>body{}</style>");
        assertThat(out).contains("安全内容");
        assertThat(out).doesNotContain("script", "iframe", "style", "alert(1)");
    }

    @Test
    @DisplayName("事件属性与 style/class/target 全部去掉（只有 href 留下）")
    void richText_dropsDangerousAttributes() {
        String out = RichTextSanitizer.richText(
                "<a href=\"https://example.com\" onclick=\"evil()\" target=\"_blank\" style=\"position:fixed\">链接</a>");
        assertThat(out).contains("href=\"https://example.com\"");
        assertThat(out).doesNotContain("onclick", "target", "style");
    }

    @Test
    @DisplayName("javascript: 协议的 href 会被拒掉")
    void richText_rejectsJavascriptUrl() {
        String out = RichTextSanitizer.richText("<a href=\"javascript:alert(1)\">点我</a>");
        assertThat(out).doesNotContain("javascript:");
    }

    @Test
    @DisplayName("图片地址：http(s) 与相对路径都保留，危险协议摘掉")
    void richText_imgProtocol() {
        assertThat(RichTextSanitizer.richText("<img src=\"https://a.com/x.png\" alt=\"a\">"))
                .contains("https://a.com/x.png");
        // 相对路径必须保留（本项目图片惯例 /seed/xxx.jpg）
        assertThat(RichTextSanitizer.richText("<img src=\"/seed/c-001.jpg\">"))
                .contains("/seed/c-001.jpg");
        assertThat(RichTextSanitizer.richText("<img src=\"photo/marquee/01.jpg\">"))
                .contains("photo/marquee/01.jpg");
        // 危险协议一律摘掉
        assertThat(RichTextSanitizer.richText("<img src=\"javascript:alert(1)\">")).doesNotContain("javascript:");
        assertThat(RichTextSanitizer.richText("<img src=\"data:image/svg+xml;base64,PHN2Zz4=\">"))
                .doesNotContain("data:");
        // 混淆写法（中间塞空白/实体）也要挡住
        assertThat(RichTextSanitizer.richText("<a href=\"java&#9;script:alert(1)\">x</a>"))
                .doesNotContain("script:");
    }

    @Test
    @DisplayName("null / 空白原样返回（保持「没填」语义）")
    void richText_nullSafe() {
        assertThat(RichTextSanitizer.richText(null)).isNull();
        assertThat(RichTextSanitizer.richText("")).isEmpty();
        assertThat(RichTextSanitizer.richText("   ")).isEqualTo("   ");
    }

    @Test
    @DisplayName("幂等：清洗过的再清洗一次结果不变")
    void richText_idempotent() {
        String once = RichTextSanitizer.richText("<p>a</p><script>x</script>");
        assertThat(RichTextSanitizer.richText(once)).isEqualTo(once);
    }

    /* ---------------- 纯文本：不许多转义 ---------------- */

    @Test
    @DisplayName("纯文本没有标签时一个字符都不动（含 < 的正常文字）")
    void plainText_untouchedWhenNoTags() {
        assertThat(RichTextSanitizer.plainText("全场满 100 < 200 减 30")).isEqualTo("全场满 100 < 200 减 30");
        assertThat(RichTextSanitizer.plainText("小明说：好评！")).isEqualTo("小明说：好评！");
        // 上面的关键点：不能被 Jsoup 转义成 &lt;
        assertThat(RichTextSanitizer.plainText("a < b")).isEqualTo("a < b");
    }

    @Test
    @DisplayName("纯文本里的标签会被剥掉（内容保留）")
    void plainText_stripsTags() {
        assertThat(RichTextSanitizer.plainText("<b>很</b>好用")).isEqualTo("很好用");
        assertThat(RichTextSanitizer.plainText("<img src=x onerror=alert(1)>")).isEmpty();
    }

    @Test
    @DisplayName("纯文本里的 script 被整段丢弃（不留 alert(1) 文本）")
    void plainText_dropsScript() {
        String out = RichTextSanitizer.plainText("<script>alert(1)</script>");
        assertThat(out).doesNotContain("<script>", "alert(1)");
    }

    @Test
    @DisplayName("纯文本 null / 空白原样返回")
    void plainText_nullSafe() {
        assertThat(RichTextSanitizer.plainText(null)).isNull();
        assertThat(RichTextSanitizer.plainText("")).isEmpty();
    }
}
