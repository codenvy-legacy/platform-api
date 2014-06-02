/*******************************************************************************
* Copyright (c) 2012-2014 Codenvy, S.A.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
* Codenvy, S.A. - initial API and implementation
*******************************************************************************/
package com.codenvy.api.builder.dto;

import com.codenvy.dto.shared.DTO;

import java.util.List;
import java.util.Map;

/**
 * Options to configure build process.
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

    boolean isSkipTest();

    void setSkipTest(boolean skip);

    BuildOptions withSkipTest(boolean skip);

    boolean isIncludeDependencies();

    void setIncludeDependencies(boolean includeDependencies);

    BuildOptions withIncludeDependencies(boolean includeDependencies);
}
