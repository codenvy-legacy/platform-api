/*
 * CODENVY CONFIDENTIAL
 * __________________
 * 
 * [2012] - [$today.year] Codenvy, S.A. 
 * All Rights Reserved.
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
package com.codenvy.api.project.server;

import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;

import java.io.IOException;

/**
 * Provide possibility for importing source from some resource e.g. VCS (like Git or SVN) or from ZIP archive
 *
 * @author Vitaly Parfonov
 */
public interface SourceImporter {

    /**
     * @return type of importer e.g git, zip
     */
    String getType();

    /**
     * Imports source from the given {@code location}. Creates project if it doesn't exist.
     *
     * @param workspace
     *         the workspace id
     * @param projectName
     *         the new name of new project if it not exist or import to the given project
     * @param location
     *         location to the import sources
     */
    void importSource(String workspace, String projectName, String location) throws IOException, VirtualFileSystemException;

}
