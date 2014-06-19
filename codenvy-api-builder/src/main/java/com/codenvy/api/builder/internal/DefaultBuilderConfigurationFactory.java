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

import com.codenvy.api.builder.BuilderException;
import com.codenvy.api.builder.dto.BaseBuilderRequest;
import com.codenvy.api.builder.dto.BuildRequest;
import com.codenvy.api.builder.dto.DependencyRequest;
import com.codenvy.api.project.shared.dto.ProjectDescriptor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Base implementation of BuilderConfigurationFactory.
 *
 * @author andrew00x
 */
public class DefaultBuilderConfigurationFactory implements BuilderConfigurationFactory {
    private final Builder builder;

    public DefaultBuilderConfigurationFactory(Builder builder) {
        this.builder = builder;
    }

    @Override
    public BuilderConfiguration createBuilderConfiguration(BaseBuilderRequest request) throws BuilderException {
        if (request instanceof BuildRequest) {
            return new BuilderConfiguration(createWorkDir(request), BuilderTaskType.DEFAULT, request);
        } else if (request instanceof DependencyRequest) {
            final DependencyRequest myRequest = (DependencyRequest)request;
            String type = myRequest.getType();
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
            return new BuilderConfiguration(createWorkDir(request), taskType, myRequest);

        }
        throw new BuilderException("Unsupported type of request");
    }

    /**
     * Work directory that will be created will match build-<generated number>/project-name .
     * @param request the request for this new build
     * @return the folder that will be used as work directory
     * @throws BuilderException if there is any exception (like creating the directories)
     */
    protected java.io.File createWorkDir(BaseBuilderRequest request) throws BuilderException {

        // Get project name through project descriptor
        String projectName = request.getProjectDescriptor().getName();

        // Create a temp directory
        File tempDirectory;
        try {
            tempDirectory = Files.createTempDirectory(builder.getBuildDirectory().toPath(), "build-").toFile();
        } catch (IOException e) {
            throw new BuilderException(e);
        }
        // Now create subdirectory to match the project name. If project is relying on its name it is kept.
        try {
            return Files.createDirectory(new File(tempDirectory, projectName).toPath()).toFile();
        } catch (IOException e) {
            throw new BuilderException(e);
        }

    }
}
