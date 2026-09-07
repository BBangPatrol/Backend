package com.bbangpatrol.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageResizerTest {

    @Test
    @DisplayName("큰 이미지는 긴 변이 maxDimension 으로 줄고 용량도 줄어든다")
    void shrinksLargeImage() throws IOException {
        byte[] source = png(3000, 2000);

        byte[] resized = ImageResizer.toJpeg(source, 1080, 0.8f);
        BufferedImage result = read(resized);

        assertEquals(1080, result.getWidth());
        assertEquals(720, result.getHeight());
        assertTrue(resized.length < source.length,
                "리사이즈 결과가 원본보다 커졌다: " + resized.length + " >= " + source.length);
    }

    @Test
    @DisplayName("원본이 maxDimension 보다 작으면 확대하지 않는다")
    void doesNotEnlargeSmallImage() throws IOException {
        byte[] resized = ImageResizer.toJpeg(png(200, 100), 1080, 0.8f);
        BufferedImage result = read(resized);

        assertEquals(200, result.getWidth());
        assertEquals(100, result.getHeight());
    }

    @Test
    @DisplayName("썸네일은 긴 변이 400px 이고 원본보다 작다")
    void createsThumbnail() throws IOException {
        byte[] source = png(3000, 2000);

        byte[] origin = ImageResizer.toJpeg(source, 1080, 0.8f);
        byte[] thumbnail = ImageResizer.toJpeg(source, 400, 0.75f);
        BufferedImage result = read(thumbnail);

        assertEquals(400, result.getWidth());
        assertTrue(thumbnail.length < origin.length,
                "썸네일이 원본보다 크다: " + thumbnail.length + " >= " + origin.length);
    }

    @Test
    @DisplayName("알파 채널이 있는 PNG 도 JPEG 로 변환된다")
    void handlesImageWithAlphaChannel() throws IOException {
        BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(255, 0, 0, 128));
        g.fillRect(0, 0, 800, 600);
        g.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);

        BufferedImage result = read(ImageResizer.toJpeg(out.toByteArray(), 400, 0.75f));

        assertEquals(400, result.getWidth());
        // JPEG 는 알파를 지원하지 않으므로 불투명 이미지로 나와야 한다
        assertEquals(BufferedImage.TYPE_3BYTE_BGR, result.getType());
    }

    @Test
    @DisplayName("toPng 은 알파 채널을 유지하며 축소한다")
    void pngKeepsAlpha() throws IOException {
        // 아이템 아이콘과 같은 조건: 2048x2048 투명 배경 PNG
        BufferedImage image = new BufferedImage(2048, 2048, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(220, 160, 90, 255));
        g.fillOval(200, 200, 1648, 1648);
        g.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        byte[] source = out.toByteArray();

        byte[] resized = ImageResizer.toPng(source, 300);
        BufferedImage result = read(resized);

        assertEquals(300, result.getWidth());
        assertEquals(300, result.getHeight());
        assertTrue(result.getColorModel().hasAlpha(), "알파 채널이 사라졌다");
        // 모서리는 투명하게 남아야 한다
        assertEquals(0, result.getRGB(2, 2) >>> 24, "투명 영역이 불투명해졌다");
        assertTrue(resized.length < source.length,
                "축소 결과가 원본보다 크다: " + resized.length + " >= " + source.length);
    }

    @Test
    @DisplayName("toPng 도 원본보다 크게 확대하지 않는다")
    void pngDoesNotEnlarge() throws IOException {
        BufferedImage result = read(ImageResizer.toPng(png(200, 100), 300));

        assertEquals(200, result.getWidth());
        assertEquals(100, result.getHeight());
    }

    // JPEG 압축이 잘 먹지 않도록 노이즈가 섞인 PNG 를 만든다 (실제 사진에 가까운 조건)
    private byte[] png(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, (x * 7919 + y * 104729) & 0xFFFFFF);
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private BufferedImage read(byte[] bytes) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(bytes));
    }
}
