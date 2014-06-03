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
package com.codenvy.api.runner.dto;

import com.codenvy.api.project.shared.dto.ProjectDescriptor;
import com.codenvy.dto.shared.DTO;

import java.util.List;
import java.util.Map;

/**
 * Run application request.
 *
 * @author andrew00x
 */
@DTO
public interface RunRequest {
    long getId();

    void setId(long id);

    RunRequest withId(long id);

    /** Location of deployment sources. Deployment sources typically points to zip bundle of application that is ready to run. */
    String getDeploymentSourcesUrl();

    void setDeploymentSourcesUrl(String url);

    RunRequest withDeploymentSourcesUrl(String url);

    ProjectDescriptor getProjectDescriptor();

    void setProjectDescriptor(ProjectDescriptor project);

    RunRequest withProjectDescriptor(ProjectDescriptor project);

    /** Name of {@link com.codenvy.api.runner.internal.Runner} which should be used for running this application. */
    String getRunner();

    void setRunner(String runner);

    RunRequest withRunner(String runner);

    /**
     * Get id of environment that should be used for running an application. If this parameter is omitted then runner will use default
     * environment.
     *
     * @see RunnerDescriptor#getEnvironments()
     * @see com.codenvy.api.runner.dto.RunnerEnvironment
     * @see com.codenvy.api.runner.dto.RunOptions
     */
    String getEnvironmentId();

    void setEnvironmentId(String environmentId);

    RunRequest withEnvironmentId(String environmentId);

    /** Location of files that contains run scripts. */
    List<String> getRunnerScriptUrls();

    void setRunnerScriptUrls(List<String> scripts);

    RunRequest withRunnerScriptUrls(List<String> scripts);

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


    String getUserName();

    RunRequest withUserName(String userName);

    void setUserName(String userName);
}
