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
package com.codenvy.api.builder.dto;

import com.codenvy.dto.shared.DTO;

import java.util.List;
import java.util.Map;

/**
 * Options to configure build process from client
 *
 * @author Eugene Voevodin
 */
@DTO
public interface BuildOptions {
    /** Get name of builder. This parameter has preference over builder name that is configured in properties of project. */
    String getBuilderName();

    void setBuilderName(String builderName);

    BuildOptions withBuilderName(String builderName);

    /**
     * Build targets, e.g. "clean", "compile", ... . Supported targets depend on builder implementation. Builder uses default targets if
     * this parameter is not provided by client.
     */
    List<String> getTargets();

    BuildOptions withTargets(List<String> targets);

    void setTargets(List<String> targets);

    /**
     * Optional parameters for builder. Supported options depend on builder implementation. Builder may provide own set of options. User
     * specified options have preference over builder's default options.
     */
    Map<String, String> getOptions();

    BuildOptions withOptions(Map<String, String> options);

    void setOptions(Map<String, String> options);

    boolean isIncludeDependencies();

    void setIncludeDependencies(boolean includeDependencies);

    BuildOptions withIncludeDependencies(boolean includeDependencies);
}
