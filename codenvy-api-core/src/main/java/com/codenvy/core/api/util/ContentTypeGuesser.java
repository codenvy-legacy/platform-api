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
package com.codenvy.core.api.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.Properties;

/**
 * Helps getting content type of file.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
public class ContentTypeGuesser {
    private static final Logger LOG = LoggerFactory.getLogger(ContentTypeGuesser.class);

    private static String defaultContentType = "application/octet-stream";

    public synchronized static void setDefaultContentType(String myDefaultContentType) {
        defaultContentType = myDefaultContentType;
    }

    public synchronized static String getDefaultContentType() {
        return defaultContentType;
    }

    private static Properties properties = new Properties();

    static {
        final String filePath = System.getProperty("com.codenvy.content-types");
        URL resource = null;
        if (filePath != null) {
            resource = Thread.currentThread().getContextClassLoader().getResource(filePath);
        }
        if (resource == null) {
            resource = Thread.currentThread().getContextClassLoader().getResource("content-types.properties");
        }
        if (resource != null) {
            try (InputStream stream = resource.openStream()) {
                properties.load(stream);
                LOG.info("Successfully load content types from {}", resource);
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    public static String guessContentType(java.io.File file) {
        String contentType = null;
        final String name = file.getName();
        final int dot = name.lastIndexOf('.');
        if (dot > 0) {
            final String ext = name.substring(dot + 1);
            if (!ext.isEmpty()) {
                // by file extensions
                contentType = properties.getProperty(ext);
            }
        }
        if (contentType == null) {
            // by full file name.
            contentType = properties.getProperty(name);
        }
        if (contentType == null) {
            try {
                // if content type is null use JDK.
                contentType = Files.probeContentType(file.toPath());
            } catch (IOException e) {
                LOG.warn(e.getMessage(), e);
            }
        }
        return contentType == null ? defaultContentType : contentType;
    }

    private ContentTypeGuesser() {
    }
}
