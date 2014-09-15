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
    public RunnerEnvironmentConfiguration(RunnerEnvironmentConfiguration origin) {
        this.recommendedMemorySize = origin.getRecommendedMemorySize();
        this.requiredMemorySize = origin.getRequiredMemorySize();
        this.defaultMemorySize = origin.defaultMemorySize;
    }

    public RunnerEnvironmentConfiguration(int requiredMemorySize, int recommendedMemorySize, int defaultMemorySize) {
        this.requiredMemorySize = requiredMemorySize;
        this.recommendedMemorySize = recommendedMemorySize;
        this.defaultMemorySize = defaultMemorySize;
    }

    public RunnerEnvironmentConfiguration() {
    }

    private int requiredMemorySize    = -1;
    private int recommendedMemorySize = -1;
    private int defaultMemorySize     = -1;

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

    public int getDefaultMemorySize() {
        return defaultMemorySize;
    }

    public void setDefaultMemorySize(int defaultMemorySize) {
        this.defaultMemorySize = defaultMemorySize;
    }
}
