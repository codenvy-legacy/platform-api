/*
 * CODENVY CONFIDENTIAL
 * __________________
 * 
 *  [2012] - [2014] Codenvy, S.A. 
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
package com.codenvy.api.project.server;

import java.io.IOException;
import java.util.Map;

/**
 * Generates project structure.
 *
 * @author andrew00x
 */
public interface ProjectGenerator {
    /** Unique name of project generator. */
    String getName();

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
