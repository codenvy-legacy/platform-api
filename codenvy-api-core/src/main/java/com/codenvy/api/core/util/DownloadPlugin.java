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
package com.codenvy.api.core.util;

import java.io.IOException;

/**
 * Downloads remote file.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
public interface DownloadPlugin {

    interface Callback {
        /**
         * Notified when file downloaded.
         *
         * @param downloaded
         *         downloaded file
         */
        void done(java.io.File downloaded);

        /**
         * Notified when error occurs.
         *
         * @param e
         *         error
         */
        void error(IOException e);
    }

    /**
     * Download file from specified location to local directory {@code downloadTo}
     *
     * @param downloadUrl
     *         download URL
     * @param downloadTo
     *         local directory for download
     * @param callback
     *         notified when download is done or an error occurs
     */
    void download(String downloadUrl, java.io.File downloadTo, Callback callback);
}
