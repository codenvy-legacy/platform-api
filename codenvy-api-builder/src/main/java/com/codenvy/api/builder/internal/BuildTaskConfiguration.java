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
import com.codenvy.api.builder.internal.dto.BuildRequest;
import com.codenvy.api.builder.internal.dto.DependencyRequest;
import com.codenvy.api.core.rest.RemoteContent;

import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * factory for configuration of BuildTask.
 *
 * @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a>
 */
public class BuildTaskConfiguration {
    public static BuildTaskConfiguration newBuildConfiguration(Builder builder, BuildRequest request) throws IOException {
        final RemoteContent sources = RemoteContent.of(createSrcDirectory(builder.getRepository(), request), request.getSourcesUrl());
        return new BuildTaskConfiguration(sources, BuilderTaskType.DEFAULT, request);
    }

    public static BuildTaskConfiguration newDependencyAnalysisConfiguration(Builder builder, DependencyRequest request)
            throws IOException, BuilderException {
        final RemoteContent sources = RemoteContent.of(createSrcDirectory(builder.getRepository(), request), request.getSourcesUrl());
        String type = request.getType();
        if (type == null) {
            type = "list";
        }
        final BuilderTaskType taskType;
        switch (type) {
            case "copy":
                taskType = BuilderTaskType.COPY_DEPS;
                break;
            case "list":
                taskType = BuilderTaskType.LIST_DEPS;
                break;
            default:
                throw new BuilderException(
                        String.format("Unsupported type of an analysis task: %s. Should be either 'list' or 'copy'", type));
        }
        return new BuildTaskConfiguration(sources, taskType, request);
    }

    private static java.io.File createSrcDirectory(java.io.File parentDirectory, BaseBuilderRequest request) throws IOException {
        final String workspace = request.getWorkspace();
        final String project = request.getProject();
        final java.io.File srcDirectory;
        if (workspace == null || project == null) {
            srcDirectory = Files.createTempDirectory(parentDirectory.toPath(), "build-").toFile();
        } else {
            final java.io.File workspaceDirectory = new java.io.File(parentDirectory, workspace);
            if (!(workspaceDirectory.exists() || workspaceDirectory.mkdir())) {
                throw new IOException(String.format("Unable create %s", workspaceDirectory));
            }
            java.io.File tmp = new java.io.File(workspaceDirectory, project);
            if (!tmp.mkdir()) {
                int suffix = workspaceDirectory.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(java.io.File path) {
                        return path.isDirectory() && path.getName().startsWith(project);
                    }
                }).length;
                for (; ; ) {
                    tmp = new java.io.File(workspaceDirectory, project + '(' + suffix + ')');
                    if (tmp.mkdir()) {
                        break;
                    }
                    suffix++;
                }
            }
            srcDirectory = tmp;
        }
        return srcDirectory;
    }

    private final RemoteContent      sources;
    private final BuilderTaskType    taskType;
    private final BaseBuilderRequest request;

    private BuildTaskConfiguration(RemoteContent sources, BuilderTaskType taskType, BaseBuilderRequest request) {
        this.sources = sources;
        this.taskType = taskType;
        this.request = request;
    }

    public RemoteContent getSources() {
        return sources;
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
        return request;
    }

    @Override
    public String toString() {
        return "BuildTaskConfiguration{" +
               "sources=" + sources +
               ", taskType=" + taskType +
               ", request=" + request +
               '}';
    }
}
