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

import java.io.IOException;

/**
 * Downloads remote file.
 *
 * @author andrew00x
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

    /**
     * Download file from specified location to local directory {@code downloadTo}
     *
     * @param downloadUrl
     *         download URL
     * @param downloadTo
     *         local directory for download
     * @param fileName
     *         name of local file to save download result
     * @param replaceExisting
     *         replace existed file with the same name
     * @param callback
     *         notified when download is done or an error occurs
     */
    void download(String downloadUrl, java.io.File downloadTo, String fileName, boolean replaceExisting, Callback callback);
}
