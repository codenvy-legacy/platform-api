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
package com.codenvy.api.project.server;

import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;

import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * @author Vitaly Parfonov
 */
@Singleton
public class ZipProjectImporter implements ProjectImporter {
    @Override
    public String getId() {
        return "zip";
    }

    @Override
    public String getDescription() {
        return "Add possibility to import project from zip archive located by public URL";
    }

    @Override
    public void importSources(FolderEntry baseFolder, String location) throws IOException {
        URL url;
        if (location.startsWith("http://") || location.startsWith("https://")) {
            url = new URL(location);
        } else {
            url = Thread.currentThread().getContextClassLoader().getResource(location);
            if (url == null) {
                final java.io.File file = new java.io.File(location);
                if (file.exists()) {
                    url = file.toURI().toURL();
                }
            }
        }
        if (url == null) {
            throw new IOException(String.format("Can't find %s", location));
        }
        try (InputStream zip = url.openStream()) {
            baseFolder.getVirtualFile().unzip(zip, true);
        } catch (VirtualFileSystemException e) {
            throw new IOException(e.getMessage(), e);
        }
    }
}
