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

import com.codenvy.api.core.ConflictException;
import com.codenvy.api.core.ForbiddenException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.project.shared.dto.NewProject;

/**
 * Generates project structure.
 *
 * @author andrew00x
 * @author Artem Zatsarynnyy
 */
public interface ProjectGenerator {
    /** Unique id of project generator. */
    String getId();

    /**
     * Generates project.
     *
     * @param baseFolder
     *         base project folder
     * @param newProjectDescriptor
     *         new project descriptor
     * @throws ForbiddenException
     *         if some operations in {@code baseFolder} are forbidden, e.g. current user doesn't have write permissions to the {@code
     *         baseFolder}
     * @throws ConflictException
     *         if project generation causes any conflicts, e.g. if import operation causes name conflicts in {@code baseFolder}
     * @throws ServerException
     *         if project generation causes some errors that should be treated as internal errors
     */
    void generateProject(FolderEntry baseFolder, NewProject newProjectDescriptor)
            throws ForbiddenException, ConflictException, ServerException;
}
