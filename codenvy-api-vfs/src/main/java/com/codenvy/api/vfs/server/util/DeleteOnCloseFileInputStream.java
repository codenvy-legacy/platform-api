/*******************************************************************************
* Copyright (c) 2012-2014 Codenvy, S.A.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
* Codenvy, S.A. - initial API and implementation
*******************************************************************************/
package com.codenvy.api.vfs.server.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Delete java.io.File after closing.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
public final class DeleteOnCloseFileInputStream extends FileInputStream {
    private final java.io.File file;
    private boolean deleted = false;

    public DeleteOnCloseFileInputStream(java.io.File file) throws FileNotFoundException {
        super(file);
        this.file = file;
    }

    /** @see java.io.FileInputStream#close() */
    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            if (!deleted) {
                deleted = file.delete();
            }
        }
        //System.out.println("---> " + file.getAbsolutePath() + ", exists : " + file.exists());
    }
}