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
package com.codenvy.api.project.server;

import java.io.IOException;
import java.util.Map;

/**
 * Generates project structure.
 *
 * @author andrew00x
 */
public interface ProjectGenerator {
    /** Unique id of project generator. */
    String getId();

    /**
     * Generates project.
     *
     * @param baseFolder
     *         base project folder
     * @param options
     *         generator options
     * @throws IOException
     *         if i/o error occurs
     */
    void generateProject(FolderEntry baseFolder, Map<String, String> options) throws IOException;
}
