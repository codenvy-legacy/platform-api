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

import com.codenvy.api.builder.dto.BuildOptions;
import com.codenvy.dto.shared.DTO;

import java.util.Map;

/**
 * Options to configure run process.
 *
 * @author Eugene Voevodin
 */
@DTO
public interface RunOptions {
    /** Get memory size (in megabytes) that is required for starting application. */
    int getMemorySize();

    void setMemorySize(int mem);

    RunOptions withMemorySize(int mem);

    /** Optional parameter which may be specified by user if need to run application under debug. */
    DebugMode getDebugMode();

    void setDebugMode(DebugMode debugMode);

    RunOptions withDebugMode(DebugMode debugMode);

    /**
     * Optional parameters for runner. Supported options depend on runner implementation. Runner may have own set of options. Caller
     * specified options have preference over runner's default options.
     */
    Map<String, String> getOptions();

    RunOptions withOptions(Map<String, String> options);

    void setOptions(Map<String, String> options);

    /** Force skip build before run. Build stage is skipped even project has configuration for builder. */
    boolean getSkipBuild();

    void setSkipBuild(boolean skipBuild);

    RunOptions withSkipBuild(boolean skipBuild);

    /**
     * Get id of environment that should be used for running an application. If this parameter is omitted then runner will use default
     * environment.
     *
     * @see RunnerDescriptor#getEnvironments()
     * @see com.codenvy.api.runner.dto.RunnerEnvironment
     */
    String getEnvironmentId();

    void setEnvironmentId(String environmentId);

    RunOptions withEnvironmentId(String environmentId);

    /**
     * Get builder options. Make sense only for application that requires build before run. This parameter has preference over builder
     * options that is configured in properties of project.
     *
     * @see com.codenvy.api.builder.dto.BuildOptions
     */
    BuildOptions getBuildOptions();

    void setBuildOptions(BuildOptions options);

    RunOptions withBuildOptions(BuildOptions options);

    /**
     * Runner may provide shell console to the instance with running application. Map that is returned by this method contains
     * configuration parameters for shell console. Supporting of shell console is optional feature and not all runner's implementation may
     * support this feature.
     */
    Map<String, String> getShellOptions();

    RunOptions withShellOptions(Map<String, String> options);

    void setShellOptions(Map<String, String> options);
}
