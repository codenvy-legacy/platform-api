/*
 * CODENVY CONFIDENTIAL
 * __________________
 * 
 *  [2012] - [2013] Codenvy, S.A. 
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.api.factory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/** Class to hold image information such as data, name, media type */
public class FactoryImage {
    private byte[] imageData;
    private String mediaType;
    private String name;

    public FactoryImage() {
    }

    public FactoryImage(byte[] data, String mediaType, String name) throws IOException {
        setMediaType(mediaType);
        this.name = name;
        setImageData(data);
    }

    public byte[] getImageData() {
        return imageData;
    }

    public void setImageData(byte[] imageData) throws IOException {
        BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageData));
        if (bufferedImage == null) {
            throw new IOException("Can't read image content.");
        }
        if (bufferedImage.getWidth() != 100 || bufferedImage.getHeight() != 100) {
            throw new IOException(
                    "Uploaded image has a wrong size. We only support image with a resolution 100 x 100. Please update your image and " +
                    "upload it again.");
        }

        this.imageData = imageData;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) throws IOException {
        switch (mediaType) {
            case "image/jpeg":
            case "image/png":
            case "image/gif":
                this.mediaType = mediaType;
                break;
            default:
                throw new IOException(mediaType + " is unsupported media type.");
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean hasContent() {
        if (imageData != null && imageData.length > 0) {
            return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FactoryImage)) return false;

        FactoryImage that = (FactoryImage)o;

        if (!Arrays.equals(imageData, that.imageData)) return false;
        if (mediaType != null ? !mediaType.equals(that.mediaType) : that.mediaType != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = imageData != null ? Arrays.hashCode(imageData) : 0;
        result = 31 * result + (mediaType != null ? mediaType.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    /**
     * Creates {@code FactoryImage}.
     * InputStream should be closed manually.
     *
     * @param is
     *         - input stream with image data
     * @param mediaType
     *         - media type of image
     * @param name
     *         - image name
     * @return - {@code FactoryImage} if {@code FactoryImage} was created, null if input stream has no content
     * @throws FactoryUrlException
     */
    public static FactoryImage createImage(InputStream is, String mediaType, String name) throws FactoryUrlException {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int read;
            while ((read = is.read(buffer, 0, buffer.length)) != -1) {
                baos.write(buffer, 0, read);
                if (baos.size() > 1024 * 1024) {
                    throw new FactoryUrlException(413, "Maximum upload size exceeded.");
                }
            }

            if (baos.size() == 0) {
                return new FactoryImage();
            }
            baos.flush();

            return new FactoryImage(baos.toByteArray(), mediaType, name);
        } catch (IOException e) {
            throw new FactoryUrlException(e.getLocalizedMessage(), e);
        }
    }
}
