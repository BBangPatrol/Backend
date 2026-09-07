package com.bbangpatrol.common.util;

import net.coobird.thumbnailator.Thumbnails;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 * 업로드된 이미지를 R2 에 올리기 전에 축소/재인코딩한다.
 * 폰 카메라 원본(장당 3~8MB)을 그대로 내려주면 목록 로딩이 느려지므로 업로드 시점에 한 번만 줄여둔다.
 */
public final class ImageResizer {

    private ImageResizer() {
    }

    /**
     * 긴 변이 maxDimension 을 넘지 않도록 비율을 유지하며 축소한 뒤 JPEG 로 인코딩한다.
     * 원본이 이미 더 작으면 확대하지 않고 재인코딩만 한다.
     *
     * <p>투명 영역은 검게 채워진다. 알파를 살려야 하면 {@link #toPng(byte[], int)} 를 쓸 것.
     *
     * @param quality 0.0 ~ 1.0 의 JPEG 품질
     */
    public static byte[] toJpeg(byte[] source, int maxDimension, float quality) throws IOException {
        // 알파 채널이 있는 이미지를 JPEG 로 쓸 때 색이 깨지는 것을 막는다
        return resize(source, maxDimension, "jpg", quality, BufferedImage.TYPE_INT_RGB);
    }

    /**
     * 알파 채널을 유지하며 PNG 로 축소한다.
     * 투명 배경 아이콘처럼 JPEG 로 바꿀 수 없는 이미지에 쓴다.
     */
    public static byte[] toPng(byte[] source, int maxDimension) throws IOException {
        // PNG 는 무손실이라 품질 파라미터가 없다 (Thumbnailator 는 png 에 outputQuality 를 주면 예외를 던진다)
        return resize(source, maxDimension, "png", null, BufferedImage.TYPE_INT_ARGB);
    }

    private static byte[] resize(byte[] source, int maxDimension, String format, Float quality, int imageType)
            throws IOException {
        int longestEdge = readLongestEdge(source);
        // Thumbnailator 의 size() 는 원본보다 크게 지정하면 확대해버린다. 축소만 하도록 목표 크기를 제한한다
        int target = longestEdge > 0 ? Math.min(maxDimension, longestEdge) : maxDimension;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        var builder = Thumbnails.of(new ByteArrayInputStream(source))
                .size(target, target)
                .keepAspectRatio(true)
                .imageType(imageType)
                .outputFormat(format);
        if (quality != null) {
            builder.outputQuality(quality);
        }
        builder.toOutputStream(out);
        return out.toByteArray();
    }

    /**
     * 픽셀을 디코드하지 않고 헤더에서 크기만 읽는다.
     * 90/270 도 회전된 사진이라 가로세로가 바뀌어도 긴 변의 길이는 같으므로 EXIF 를 볼 필요가 없다.
     *
     * @return 긴 변의 픽셀 수. 읽지 못하면 0 (호출측은 maxDimension 을 그대로 쓴다)
     */
    private static int readLongestEdge(byte[] source) {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(source))) {
            if (in == null) return 0;
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) return 0;

            ImageReader reader = readers.next();
            try {
                reader.setInput(in);
                return Math.max(reader.getWidth(0), reader.getHeight(0));
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            return 0;
        }
    }
}
