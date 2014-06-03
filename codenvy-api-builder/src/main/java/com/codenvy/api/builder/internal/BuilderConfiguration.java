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
package com.codenvy.api.builder.internal;

import com.codenvy.api.builder.dto.BaseBuilderRequest;
import com.codenvy.dto.server.DtoFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builder configuration for particular build process.
 *
 * @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a>
 */
public class BuilderConfiguration {
    private final java.io.File       workDir;
    private final BuilderTaskType    taskType;
    private final BaseBuilderRequest request;

    public BuilderConfiguration(java.io.File workDir, BuilderTaskType taskType, BaseBuilderRequest request) {
        this.workDir = workDir;
        this.taskType = taskType;
        this.request = request;
    }

    public java.io.File getWorkDir() {
        return workDir;
    }

    public BuilderTaskType getTaskType() {
        return taskType;
    }

    public List<String> getTargets() {
        return new ArrayList<>(request.getTargets());
    }

    public Map<String, String> getOptions() {
        return new LinkedHashMap<>(request.getOptions());
    }

    public BaseBuilderRequest getRequest() {
        return DtoFactory.getInstance().clone(request);
    }

    @Override
    public String toString() {
        return "BuilderConfiguration{" +
               "workDir=" + workDir +
               ", taskType=" + taskType +
               ", request=" + request +
               '}';
    }
}
