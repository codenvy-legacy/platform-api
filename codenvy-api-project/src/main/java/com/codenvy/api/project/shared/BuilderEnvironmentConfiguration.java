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
package com.codenvy.api.project.shared;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author andrew00x
 */
@Deprecated
public class BuilderEnvironmentConfiguration {
    public BuilderEnvironmentConfiguration(BuilderEnvironmentConfiguration origin) {
        setOptions(origin.getOptions());
    }

    public BuilderEnvironmentConfiguration(Map<String, String> options) {
        setOptions(options);
    }

    public BuilderEnvironmentConfiguration() {
    }

    private Map<String, String> options;

    public Map<String, String> getOptions() {
        if (options == null) {
            options = new LinkedHashMap<>();
        }
        return options;
    }

    public void setOptions(Map<String, String> options) {
        final Map<String, String> myOptions = getOptions();
        myOptions.clear();
        if (options != null) {
            myOptions.putAll(options);
        }
    }
}
