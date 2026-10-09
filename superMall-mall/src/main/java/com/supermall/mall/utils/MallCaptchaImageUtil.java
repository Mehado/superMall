package com.supermall.mall.utils;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 图形验证码绘制：纯 JDK（AWT），不引第三方验证码库。
 *
 * <p>风格上刻意做得"脏"一点：随机旋转、随机基线偏移、干扰曲线、噪点 ——
 * 目的是让简单 OCR 不好过，同时对人眼保持可读。
 *
 * <p>注意：Spring Boot 默认 headless=true，BufferedImage + Graphics2D + ImageIO
 * 在 headless 下可正常工作；字体用逻辑字体 SansSerif，避免容器里缺字体导致报错。
 */
public final class MallCaptchaImageUtil {

    private MallCaptchaImageUtil() {
    }

    /** 去掉了 0/O、1/I/L 这些容易看混的字符 */
    private static final char[] CHARS = "23456789ABCDEFGHJKMNPQRSTUVWXYZ".toCharArray();

    private static final SecureRandom RANDOM = new SecureRandom();

    /** 生成随机验证码文本 */
    public static String randomCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS[RANDOM.nextInt(CHARS.length)]);
        }
        return sb.toString();
    }

    /**
     * 把验证码文本画成 PNG，返回 base64（不含 data: 前缀）。
     */
    public static String renderBase64(String code, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            applyQualityHints(g);
            paintBackground(g, width, height);
            paintNoise(g, width, height);
            paintChars(g, code, width, height);
            paintLines(g, width, height);
        } finally {
            g.dispose();
        }
        return toBase64Png(image);
    }

    private static void applyQualityHints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    }

    private static void paintBackground(Graphics2D g, int w, int h) {
        // 与前端卡片同色系（#fafbfc ~ #f2f3f6）的浅灰底，避免在界面上太突兀
        g.setColor(new Color(250, 251, 252));
        g.fillRect(0, 0, w, h);
        g.setColor(new Color(242, 243, 246));
        for (int i = 0; i < 6; i++) {
            int x = RANDOM.nextInt(w);
            int y = RANDOM.nextInt(h);
            int r = 10 + RANDOM.nextInt(22);
            g.fillOval(x - r, y - r, r * 2, r * 2);
        }
    }

    private static void paintNoise(Graphics2D g, int w, int h) {
        for (int i = 0; i < w * h / 26; i++) {
            g.setColor(randomColor(150, 210));
            g.fillRect(RANDOM.nextInt(w), RANDOM.nextInt(h), 1, 1);
        }
    }

    private static void paintChars(Graphics2D g, String code, int w, int h) {
        int n = code.length();
        // 字符区留边距，避免旋转后被裁掉
        double slot = (double) (w - 16) / n;
        for (int i = 0; i < n; i++) {
            AffineTransform saved = g.getTransform();
            double fontSize = h * 0.62 + RANDOM.nextInt(6) - 3;
            g.setFont(new Font("SansSerif", Font.BOLD, (int) fontSize));
            g.setColor(randomColor(20, 120));

            double cx = 8 + slot * i + slot / 2;
            double cy = h * 0.5 + (RANDOM.nextInt(7) - 3);
            // -28° ~ +28° 随机旋转
            double angle = Math.toRadians(RANDOM.nextInt(57) - 28);
            g.rotate(angle, cx, cy);

            String ch = String.valueOf(code.charAt(i));
            int cw = g.getFontMetrics().charWidth(code.charAt(i));
            g.drawString(ch, (float) (cx - cw / 2.0), (float) (cy + fontSize * 0.34));
            g.setTransform(saved);
        }
    }

    private static void paintLines(Graphics2D g, int w, int h) {
        // 三条正弦干扰线，比直线更难被简单算法抹掉
        for (int k = 0; k < 3; k++) {
            g.setColor(randomColor(120, 200));
            g.setStroke(new BasicStroke(1.1f + RANDOM.nextFloat(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            double amp = 3 + RANDOM.nextDouble() * 6;
            double phase = RANDOM.nextDouble() * Math.PI * 2;
            double freq = 0.6 + RANDOM.nextDouble() * 1.4;
            int yBase = 6 + RANDOM.nextInt(Math.max(1, h - 12));
            int prevX = 0;
            int prevY = yBase;
            for (int x = 1; x <= w; x++) {
                int y = (int) (yBase + Math.sin(phase + x * freq / 10.0) * amp);
                g.drawLine(prevX, prevY, x, y);
                prevX = x;
                prevY = y;
            }
        }
    }

    private static Color randomColor(int min, int max) {
        int range = Math.max(1, max - min);
        return new Color(
                min + RANDOM.nextInt(range),
                min + RANDOM.nextInt(range),
                min + RANDOM.nextInt(range));
    }

    private static String toBase64Png(BufferedImage image) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(4096)) {
            ImageIO.write(image, "png", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("验证码图片生成失败", e);
        }
    }
}
