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
package com.codenvy.api.builder.internal;

import java.io.File;

/**
 * @author andrew00x
 */
public class SourceManagerEvent {
    private final String    workspace;
    private final String    project;
    private final String    sourcesUrl;
    private final File      workDir;

    public SourceManagerEvent(String workspace, String project, String sourcesUrl, File workDir) {
        this.workspace = workspace;
        this.project = project;
        this.sourcesUrl = sourcesUrl;
        this.workDir = workDir;
    }

    public String getWorkspace() {
        return workspace;
    }

    public String getProject() {
        return project;
    }

    public String getSourcesUrl() {
        return sourcesUrl;
    }

    public File getWorkDir() {
        return workDir;
    }
}
