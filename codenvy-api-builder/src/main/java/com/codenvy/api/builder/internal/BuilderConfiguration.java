/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2013] Codenvy, S.A.
 *  All Rights Reserved.
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
package com.codenvy.api.builder.internal;

import com.codenvy.api.builder.internal.dto.BaseBuilderRequest;
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
