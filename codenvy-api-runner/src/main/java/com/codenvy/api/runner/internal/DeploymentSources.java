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
package com.codenvy.api.runner.internal;

import com.codenvy.commons.lang.ZipUtils;

import java.io.IOException;

/**
 * An application bundle that contains all needed binaries, configurations, etc for running application with {@link
 * com.codenvy.api.runner.internal.Runner}. May be represented with single (archive) file or directory.
 *
 * @author andrew00x
 */
public class DeploymentSources {
    private final java.io.File file;

    public DeploymentSources(java.io.File file) {
        this.file = file;
    }

    /** Get application file or directory. */
    public java.io.File getFile() {
        return file;
    }

    /**
     * Checks is application bundle is zip archive or not.
     *
     * @return is application bundle is zip archive or not
     */
    public boolean isArchive() {
        try {
            return file != null && ZipUtils.isZipFile(file);
        } catch (IOException e) {
            return false;
        }
    }
}
