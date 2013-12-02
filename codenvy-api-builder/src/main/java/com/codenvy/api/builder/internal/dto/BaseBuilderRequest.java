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
     * Location of source code for build. It is required to have {@link com.codenvy.api.core.util.DownloadPlugin} which supports such type
     * of URL.
     *
     * @see com.codenvy.api.core.util.DownloadPlugin#download(String, java.io.File, com.codenvy.api.core.util.DownloadPlugin.Callback)
     */
    String getSourcesUrl();

    BaseBuilderRequest withSourcesUrl(String sourcesUrl);

    void setSourcesUrl(String sourcesUrl);

    /**
     * Name of which should be used for build. Client should use method {@link com.codenvy.api.builder.internal.SlaveBuilderService#availableBuilders()}
     * to get list of available builders.
     */
    String getBuilder();

    BaseBuilderRequest withBuilder(String builder);

    void setBuilder(String builder);

    /**
     * Get build timeout in seconds. If build is running longer then this time {@link com.codenvy.api.builder.internal.Builder} must
     * terminate the build.
     */
    long getTimeout();

    void setTimeout(long time);

    BaseBuilderRequest withTimeout(long time);

    /**
     * Build targets, e.g. "clean", "compile", ... . Supported targets depend on builder implementation. Builder uses default targets if
     * this parameter is not provided by client.
     */
    List<String> getTargets();

    BaseBuilderRequest withTargets(List<String> targets);

    void setTargets(List<String> targets);

    /**
     * Optional parameters for builder. Supported options depend on builder implementation. Builder may provide own set of options. User
     * specified options have preference over builder's default options.
     */
    Map<String, String> getOptions();

    BaseBuilderRequest withOptions(Map<String, String> options);

    void setOptions(Map<String, String> options);

    /** Name of workspace which the sources are belong. */
    String getWorkspace();

    BaseBuilderRequest withWorkspace(String workspace);

    void setWorkspace(String workspace);

    /** Name of project which represents sources on the ide side. */
    String getProject();

    BaseBuilderRequest withProject(String project);

    void setProject(String project);

    /** URL that will be notified when build is done: successfully, failed or cancelled. */
    String getWebHookUrl();

    BaseBuilderRequest withWebHookUrl(String webHookUrl);

    void setWebHookUrl(String webHookUrl);
}
