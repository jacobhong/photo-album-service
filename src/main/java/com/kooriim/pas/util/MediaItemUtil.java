package com.kooriim.pas.util;

import com.kooriim.pas.domain.MediaItem;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.codec.binary.StringUtils;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public class MediaItemUtil {

  public static String getMediaType(String contentType) {
    return contentType.toLowerCase().endsWith("jpeg") ||
             contentType.toLowerCase().endsWith("jpg") ||
             contentType.toLowerCase().endsWith("png") ? "photo" : "video";
  }

  public static String generateBase64Image(MediaItem mediaItem, byte[] bytes) {
    final var contentType = mediaItem.getMediaType().equals("photo") ? mediaItem.getContentType() : "png";
    return "data:image/"
             + contentType
             + ";base64,"
             + StringUtils.newStringUtf8(Base64.getEncoder().encode(bytes));
  }

  public static byte[] compressPhoto(String contentType, BufferedImage image, float aspectWidth, float aspectHeight) throws
    IOException {
    final var widthRatio = aspectWidth / image.getWidth();
    final var heightRatio = aspectHeight / image.getHeight();
    final var ratio = Math.min(widthRatio, heightRatio);
    final var width = image.getWidth() * ratio;
    final var height = image.getHeight() * ratio;
    image = Thumbnails.of(image)
              .size(Math.round(width), Math.round(height))
              .asBufferedImage();
    var byteOutputStream = new ByteArrayOutputStream();
    ImageIO.setUseCache(false);
    var writers = ImageIO.getImageWritersByFormatName(contentType);
    var writer = writers.next();
    var ios = ImageIO.createImageOutputStream(byteOutputStream);
    writer.setOutput(ios);
    final var param = writer.getDefaultWriteParam();

    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
    param.setCompressionQuality(0.75f);  // Change the quality value you prefer
    writer.write(null, new IIOImage(image, null, null), param);
    final var compressedImageResult = byteOutputStream.toByteArray();
    writer.dispose();
    byteOutputStream.close();
    ios.close();
    writer.dispose();
    return compressedImageResult;
  }
}
