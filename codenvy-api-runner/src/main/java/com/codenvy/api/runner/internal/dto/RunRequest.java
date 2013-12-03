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
package com.codenvy.api.runner.internal.dto;

import com.codenvy.dto.shared.DTO;

import java.util.Map;

/**
 * Run application request.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
@DTO
public interface RunRequest {
    /** Location of deployment sources. Deployment sources typically points to zip bundle of application that is ready to run. */
    String getDeploymentSourcesUrl();

    void setDeploymentSourcesUrl(String url);

    RunRequest withDeploymentSourcesUrl(String url);

    /** Name of {@link com.codenvy.api.runner.internal.Runner} which should be used for running this application. */
    String getRunner();

    void setRunner(String runner);

    RunRequest withRunner(String runner);

    /** Optional parameter which may be specified by user if need to run application under debug. */
    DebugMode getDebugMode();

    void setDebugMode(DebugMode debugMode);

    RunRequest withDebugMode(DebugMode debugMode);

    /** Get memory size (in megabytes) that is required for starting application. */
    int getMemorySize();

    void setMemorySize(int mem);

    RunRequest withMemorySize(int mem);

    /** Optional parameters for Runner. Supported options depend on Runner implementation. */
    Map<String, String> getOptions();

    void setOptions(Map<String, String> options);

    RunRequest withOptions(Map<String, String> options);

    /**
     * Get application lifetime in seconds. If application is running longer then this time {@link com.codenvy.api.runner.internal.Runner}
     * must terminate the application.
     */
    long getLifetime();

    void setLifetime(long time);

    RunRequest withLifetime(long time);

    /** Name of workspace which the sources are belong. */
    String getWorkspace();

    void setWorkspace(String workspace);

    RunRequest withWorkspace(String workspace);

    /** Name of project which represents sources on the ide side. */
    String getProject();

    void setProject(String project);

    RunRequest withProject(String project);

    String getWebHookUrl();

    RunRequest withWebHookUrl(String url);

    void setWebHookUrl(String url);
}
