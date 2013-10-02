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
package com.codenvy.api.core.rest;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
public final class HttpDownloadPlugin implements DownloadPlugin {
    @Override
    public void download(String downloadUri, java.io.File downloadTo, Callback callback) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection)new URL(downloadUri).openConnection();
            conn.setConnectTimeout(30 * 1000);
            conn.setConnectTimeout(30 * 1000);
            final int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                throw new IOException(String.format("Invalid response status %d from remote server. ", responseCode));
            }
            final java.io.File tempFile = new java.io.File(downloadTo, "downloaded.file");
            try (InputStream in = conn.getInputStream()) {
                Files.copy(in, tempFile.toPath());
            }
            callback.done(tempFile);
        } catch (IOException e) {
            callback.error(e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
