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

import com.codenvy.api.core.ApiException;

import java.io.IOException;

/**
 * Provide possibility for importing source from some resource e.g. VCS (like Git or SVN) or from ZIP archive
 *
 * @author Vitaly Parfonov
 */
public interface ProjectImporter {
    /**
     * @return unique id of importer e.g git, zip
     */
    String getId();


    /**
     * @return human readable description about this importer
     */
    String getDescription();

    /**
     * Imports source from the given {@code location} to the specified folder.
     *
     * @param baseFolder
     *         base project folder
     * @param location
     *         location to the import sources
     */
    void importSources(FolderEntry baseFolder, String location) throws IOException, ApiException;
}
