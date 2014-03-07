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

import com.codenvy.api.builder.BuilderException;
import com.codenvy.api.builder.internal.dto.BaseBuilderRequest;
import com.codenvy.api.builder.internal.dto.BuildRequest;
import com.codenvy.api.builder.internal.dto.DependencyRequest;

import java.io.IOException;
import java.nio.file.Files;

/**
 * Base implementation of BuilderConfigurationFactory.
 *
 * @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a>
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

    protected java.io.File createWorkDir(BaseBuilderRequest request) throws BuilderException {
        try {
            return Files.createTempDirectory(builder.getBuildDirectory().toPath(), "build-").toFile();
        } catch (IOException e) {
            throw new BuilderException(e);
        }
    }
}
