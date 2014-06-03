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
