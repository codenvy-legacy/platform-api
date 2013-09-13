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

import com.codenvy.api.core.util.ComponentLoader;

/**
 * Represents content of remote file.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
public final class RemoteContent {
    private static final DownloadPlugin CUSTOM_DOWNLOAD_PLUGIN = ComponentLoader.one(DownloadPlugin.class);

    public static RemoteContent of(final java.io.File downloadDirectory, final String downloadUrl) {
        return new RemoteContent(downloadDirectory,
                                 downloadUrl,
                                 CUSTOM_DOWNLOAD_PLUGIN);
    }

    private final FileAdapter    downloadDirectory;
    private final String         downloadUrl;
    private final DownloadPlugin downloadPlugin;

    private boolean downloaded;

    private RemoteContent(java.io.File downloadDirectory, String downloadUrl, DownloadPlugin downloadPlugin) {
        this.downloadDirectory = new FileAdapter(downloadDirectory, "");
        this.downloadUrl = downloadUrl;
        this.downloadPlugin = downloadPlugin;
    }

    public void download(DownloadPlugin.Callback callback) {
        if (downloaded) {
            return;
        }
        downloadPlugin.download(downloadUrl, downloadDirectory.getIoFile(), callback);
        downloaded = true;
    }

    public boolean isDownloaded() {
        return downloaded;
    }

    public FileAdapter getDirectory() {
        return downloadDirectory;
    }
}
