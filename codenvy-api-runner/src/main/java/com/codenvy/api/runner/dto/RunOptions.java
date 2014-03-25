/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 * [2012] - [2013] Codenvy, S.A.
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
package com.codenvy.api.runner.dto;

import com.codenvy.api.builder.dto.BuildOptions;
import com.codenvy.api.runner.internal.dto.DebugMode;
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

    /**
     * Get builder options. Make sense only for application that requires build before run.This parameter has preference over builder
     * options that is configured in properties of project.
     *
     * @see com.codenvy.api.builder.dto.BuildOptions
     */
    BuildOptions getBuildOptions();

    void setBuildOptions(BuildOptions options);

    RunOptions withBuildOptions(BuildOptions options);
}
