/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
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
        final java.io.File buildDir = createBuildDir();

        if (request instanceof BuildRequest) {
            return new BuilderConfiguration(buildDir, createWorkDir(buildDir, request), BuilderTaskType.DEFAULT, request);
        } else if (request instanceof DependencyRequest) {
            return new BuilderConfiguration(buildDir, createWorkDir(buildDir, request), BuilderTaskType.COPY_DEPS, request);
        }

        throw new BuilderException("Unsupported type of request");
    }

    protected java.io.File createBuildDir() throws BuilderException {
        try {
            return Files.createTempDirectory(builder.getBuildDirectory().toPath(), "build-").toFile();
        } catch (IOException e) {
            throw new BuilderException(e);
        }
    }

    /**
     * Work directory that will be created matches build-<generated number>/project-name.
     *
     * @param request
     *         the request for this new build
     * @return the folder that will be used as work directory
     * @throws BuilderException
     *         if there is any exception (like creating the directories)
     * @see #createBuildDir()
     */
    protected java.io.File createWorkDir(java.io.File parent, BaseBuilderRequest request) throws BuilderException {
        try {
            return Files.createDirectory(new java.io.File(parent, request.getProjectDescriptor().getName()).toPath()).toFile();
        } catch (IOException e) {
            throw new BuilderException(e);
        }
    }
}
