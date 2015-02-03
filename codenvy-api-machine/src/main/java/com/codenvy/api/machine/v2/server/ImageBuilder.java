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
package com.codenvy.api.machine.v2.server;

import com.codenvy.api.core.ForbiddenException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.core.util.LineConsumer;
import com.codenvy.api.machine.shared.model.Recipe;

import java.io.File;
import java.util.Map;
import java.util.Set;

/**
 * @author andrew00x
 */
public abstract class ImageBuilder {
    private final Recipe recipe;

    private Set<File>           files;
    private Map<String, String> environmentVariables;
    private String              workspaceId;
    private String              displayName;
    private String              createdBy;
    private LineConsumer outputConsumer = LineConsumer.DEV_NULL;

    protected ImageBuilder(Recipe recipe) {
        this.recipe = recipe;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public Set<File> getFiles() {
        return files;
    }

    public void setFiles(Set<File> files) {
        this.files = files;
    }

    public Map<String, String> getEnvironmentVariables() {
        return environmentVariables;
    }

    public void setEnvironmentVariables(Map<String, String> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LineConsumer getOutputConsumer() {
        return outputConsumer;
    }

    public void setOutputConsumer(LineConsumer outputConsumer) {
        this.outputConsumer = outputConsumer;
    }

    public abstract Image build() throws ForbiddenException, ServerException;
}
