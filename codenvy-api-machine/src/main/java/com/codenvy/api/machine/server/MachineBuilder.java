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
package com.codenvy.api.machine.server;

import com.codenvy.api.core.ForbiddenException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.core.util.LineConsumer;
import com.codenvy.api.machine.server.dto.MachineMetaInfo;
import com.codenvy.dto.server.DtoFactory;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author andrew00x
 */
public abstract class MachineBuilder {
    private String              machineId;
    private String              machineType;
    private MachineMetaInfoDao  machineMetaInfoDao;
    private MachineRecipe       recipe;
    private Set<File>           files;
    private Map<String, String> machineEnvironmentVariables;
    private Map<String, Object> buildOptions;
    private String              workspaceId;
    private String              displayName;
    private String              createdBy;
    private LineConsumer outputConsumer = LineConsumer.DEV_NULL;

    /**
     * Builds machine using supplied configuration. Puts build logs to given line consumer.
     *
     * @throws ForbiddenException
     *         if machine can't be built due to misconfiguration
     * @throws ServerException
     *         if internal error occurs
     */
    public Machine build() throws ServerException, ForbiddenException {
        if (machineId == null) {
            throw new ForbiddenException("Machine id is required");
        }
        if (workspaceId == null) {
            throw new ForbiddenException("Workspace id is required");
        }
        final Machine machine = doBuild();
        if (machineMetaInfoDao != null) {
            final DtoFactory dtoFactory = DtoFactory.getInstance();
            machineMetaInfoDao.add(dtoFactory.createDto(MachineMetaInfo.class)
                                             .withId(machineId)
                                             .withUserId(createdBy)
                                             .withWorkspaceId(workspaceId)
                                             .withDisplayName(displayName)
                                             .withType(machineType));
            machine.setMachineMetaInfoDao(machineMetaInfoDao);
        }
        machine.setOutputConsumer(outputConsumer);
        return machine;
    }

    protected abstract Machine doBuild() throws ServerException, ForbiddenException;

    //

    public MachineBuilder setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
        return this;
    }

    public MachineBuilder setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public MachineBuilder setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
        return this;
    }

    /** Sets output consumer for machine output, including build machine output. */
    public MachineBuilder setOutputConsumer(LineConsumer outputConsumer) throws IllegalArgumentException {
        if (outputConsumer == null) {
            throw new IllegalArgumentException("Output consumer can't be null");
        }
        this.outputConsumer = outputConsumer;
        return this;
    }

    public MachineBuilder setRecipe(MachineRecipe recipe) {
        this.recipe = recipe;
        return this;
    }

    public MachineBuilder addFile(File file) {
        getFiles().add(file);
        return this;
    }

    public MachineBuilder setMachineEnvironmentVariables(Map<String, String> machineEnvironmentVariables) {
        getMachineEnvironmentVariables().putAll(machineEnvironmentVariables);
        return this;
    }

    public MachineBuilder setMachineEnvironmentVariables(String name, String value) {
        getMachineEnvironmentVariables().put(name, value);
        return this;
    }

    public MachineBuilder setBuildOptions(Map<String, Object> parameters) {
        getBuildOptions().putAll(parameters);
        return this;
    }

    public MachineBuilder setBuildOption(String name, Object value) {
        getBuildOptions().put(name, value);
        return this;
    }

    //

    protected LineConsumer getOutputConsumer() {
        return outputConsumer;
    }

    protected MachineRecipe getRecipe() {
        return recipe;
    }

    protected Set<File> getFiles() {
        if (files == null) {
            files = new LinkedHashSet<>();
        }
        return this.files;
    }

    protected Map<String, String> getMachineEnvironmentVariables() {
        if (this.machineEnvironmentVariables == null) {
            this.machineEnvironmentVariables = new HashMap<>();
        }
        return machineEnvironmentVariables;
    }

    protected Map<String, Object> getBuildOptions() {
        if (buildOptions == null) {
            buildOptions = new HashMap<>();
        }
        return buildOptions;
    }

    protected String getMachineId() {
        return machineId;
    }

    //

    MachineBuilder setMachineId(String machineId) {
        this.machineId = machineId;
        return this;
    }

    MachineBuilder setMachineType(String machineType) {
        this.machineType = machineType;
        return this;
    }

    MachineBuilder setMachineMetaInfoDao(MachineMetaInfoDao machineMetaInfoDao) {
        this.machineMetaInfoDao = machineMetaInfoDao;
        return this;
    }
}
