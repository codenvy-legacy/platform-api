/*******************************************************************************
 * Copyright (c) 2012-2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.api.core.util;

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
