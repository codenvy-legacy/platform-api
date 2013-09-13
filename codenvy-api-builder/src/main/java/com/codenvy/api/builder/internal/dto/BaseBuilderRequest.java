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
package com.codenvy.api.builder.internal.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
public abstract class BaseBuilderRequest {
    /**
     * Location of source code for build. May be location to zip archive, local directory or location to a version control system. It is
     * required to have {@link com.codenvy.api.core.rest.DownloadPlugin} which supports such type of URL.
     *
     * @see com.codenvy.api.core.rest.DownloadPlugin#download(String, java.io.File, com.codenvy.api.core.rest.DownloadPlugin.Callback)
     */
    protected String              sourcesUrl;
    /**
     * Name of {@link com.codenvy.api.builder.internal.Builder} which should be used for build. Client should use method {@link
     * com.codenvy.api.builder.internal.SlaveBuilderService#availableBuilders()} to get list of available builders.
     */
    protected String              builder;
    /**
     * Build targets, e.g. "clean", "compile", ... . Supported targets depend on builder implementation. Builder uses default targets if
     * this parameter is not provided by client.
     */
    protected List<String>        targets;
    /**
     * Optional parameters for builder. Supported options depend on builder implementation. Builder may provide own set of options. User
     * specified options have preference over builder's default options.
     */
    protected Map<String, String> options;
    /** Name of workspace which the sources are belong. */
    protected String              workspace;
    /** Name of project which represents sources on thr ide side. */
    protected String              project;
    /** Name of user who requests the build. */
    protected String              username;

    public BaseBuilderRequest(String sourcesUrl,
                              String builder,
                              List<String> targets,
                              Map<String, String> options,
                              String workspace,
                              String project,
                              String username) {
        this.sourcesUrl = sourcesUrl;
        this.builder = builder;
        this.workspace = workspace;
        this.project = project;
        this.username = username;
        if (targets != null) {
            this.targets = new ArrayList<>(targets);
        }
        if (options != null) {
            this.options = new LinkedHashMap<>(options);
        }
    }

    public BaseBuilderRequest() {
    }

    public String getSourcesUrl() {
        return sourcesUrl;
    }

    public void setSourcesUrl(String sourcesUrl) {
        this.sourcesUrl = sourcesUrl;
    }

    public String getBuilder() {
        return builder;
    }

    public void setBuilder(String builder) {
        this.builder = builder;
    }

    public List<String> getTargets() {
        if (targets == null) {
            targets = new ArrayList<>();
        }
        return targets;
    }

    public void setTargets(List<String> targets) {
        if (targets == null) {
            this.targets = null;
        } else {
            this.targets = new ArrayList<>(targets);
        }
    }

    public Map<String, String> getOptions() {
        if (options == null) {
            options = new LinkedHashMap<>();
        }
        return options;
    }

    public void setOptions(Map<String, String> options) {
        if (options == null) {
            this.options = null;
        } else {
            this.options = new LinkedHashMap<>(options);
        }
    }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
