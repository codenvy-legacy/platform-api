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

/**
 * @author andrew00x
 */
public class RunnerEnvironmentConfiguration {
    private final String name;

    private int requiredMemorySize    = -1;
    private int recommendedMemorySize = -1;

    public RunnerEnvironmentConfiguration(String name) {
        this.name = name;
    }

    public String getEnvName() {
        return null;
    }

    public int getRequiredMemorySize() {
        return requiredMemorySize;
    }

    public void setRequiredMemorySize(int requiredMemorySize) {
        this.requiredMemorySize = requiredMemorySize;
    }

    public int getRecommendedMemorySize() {
        return recommendedMemorySize;
    }

    public void setRecommendedMemorySize(int recommendedMemorySize) {
        this.recommendedMemorySize = recommendedMemorySize;
    }
}
