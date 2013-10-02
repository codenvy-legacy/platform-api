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

import com.codenvy.dto.shared.DTO;

import java.util.List;
import java.util.Map;

/**
 * Base request.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
@DTO
public interface BaseBuilderRequest {

    /**
     * Location of source code for build. It is required to have {@link com.codenvy.api.core.rest.DownloadPlugin} which supports such type
     * of URL.
     *
     * @see com.codenvy.api.core.rest.DownloadPlugin#download(String, java.io.File, com.codenvy.api.core.rest.DownloadPlugin.Callback)
     */
    String getSourcesUrl();

    void setSourcesUrl(String sourcesUrl);

    /**
     * Name of which should be used for build. Client should use method {@link com.codenvy.api.builder.internal.SlaveBuilderService#availableBuilders()}
     * to get list of available builders.
     */
    String getBuilder();

    void setBuilder(String builder);

    /**
     * Build targets, e.g. "clean", "compile", ... . Supported targets depend on builder implementation. Builder uses default targets if
     * this parameter is not provided by client.
     */
    List<String> getTargets();

    void setTargets(List<String> targets);

    /**
     * Optional parameters for builder. Supported options depend on builder implementation. Builder may provide own set of options. User
     * specified options have preference over builder's default options.
     */
    Map<String, String> getOptions();

    void setOptions(Map<String, String> options);

    /** Name of workspace which the sources are belong. */
    String getWorkspace();

    void setWorkspace(String workspace);

    /** Name of project which represents sources on the ide side. */
    String getProject();

    void setProject(String project);
}
