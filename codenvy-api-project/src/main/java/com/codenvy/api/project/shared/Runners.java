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
public class Runners {
    private String              _default;
    private Map<String, Config> configs;

    public Runners(String _default) {
        this._default = _default;
    }

    public Runners(String _default, Map<String, Config> configs) {
        this._default = _default;
        this.configs = configs;
    }

    public Runners(Runners other) {
        this._default = other._default;
    }

    public String getDefault() {
        return _default;
    }

    public void setDefault(String _default) {
        this._default = _default;
    }

    public Runners withDefault(String _default) {
        this._default = _default;
        return this;
    }

    public Map<String, Config> getConfigs() {
        return configs;
    }

    public void setConfigs(Map<String, Config> configs) {
        this.configs = configs;
    }

    public Runners withConfigs(Map<String, Config> configs) {
        this.configs = configs;
        return this;
    }

    public static class Config {
        private int                 ram;
        private Map<String, String> options;
        private Map<String, String> variables;

        public Config(int ram, Map<String, String> options, Map<String, String> variables) {
            this.ram = ram;
            setOptions(options);
            setVariables(variables);
        }

        public Config(int ram) {
            this.ram = ram;
        }

        public Config(Config other) {
            this.ram = other.ram;
            setOptions(other.options);
            setVariables(other.variables);
        }

        public int getRam() {
            return ram;
        }

        public void setRam(int ram) {
            this.ram = ram;
        }

        public Config withRam(int ram) {
            this.ram = ram;
            return this;
        }

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

        public Config withOptions(Map<String, String> options) {
            final Map<String, String> myOptions = getOptions();
            myOptions.clear();
            if (options != null) {
                myOptions.putAll(options);
            }
            return this;
        }

        public Map<String, String> getVariables() {
            if (variables == null) {
                variables = new LinkedHashMap<>();
            }
            return variables;
        }

        public void setVariables(Map<String, String> variables) {
            final Map<String, String> myVariables = getVariables();
            myVariables.clear();
            if (variables != null) {
                myVariables.putAll(variables);
            }
        }

        public Config withVariables(Map<String, String> variables) {
            final Map<String, String> myVariables = getVariables();
            myVariables.clear();
            if (variables != null) {
                myVariables.putAll(variables);
            }
            return this;
        }
    }
}
