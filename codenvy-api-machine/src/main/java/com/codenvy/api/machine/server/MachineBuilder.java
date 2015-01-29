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

import javax.annotation.Nonnull;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author andrew00x
 */
public abstract class MachineBuilder {
    private final String machineId;

    private MachineRecipe       recipe;
    private Set<File>           files;
    private Map<String, String> machineEnvironmentVariables;
    private Map<String, Object> buildOptions;
    private LineConsumer        outputConsumer = LineConsumer.DEV_NULL;

    protected MachineBuilder(String machineId) {
        this.machineId = machineId;
    }

    /**
     * Sets output consumer for machine output, including build machine output.
     * Specified {@link com.codenvy.api.core.util.LineConsumer} should be passed to machine instance after successful build
     */
    public MachineBuilder setOutputConsumer(LineConsumer lineConsumer) throws ForbiddenException {
        if (lineConsumer == null) {
            throw new ForbiddenException("Output consumer can't be null");
        }
        outputConsumer = lineConsumer;
        return this;
    }

    protected LineConsumer getOutputConsumer() {
        return outputConsumer;
    }

    /**
     * Builds machine using supplied configuration. Puts build logs to given line consumer.
     *
     * @throws ForbiddenException
     *         if machine can't be built due to misconfiguration
     * @throws ServerException
     *         if internal error occurs
     */
    public Machine buildMachine() throws ServerException, ForbiddenException {
        if (machineId == null) {
            throw new ForbiddenException("Machine id is required");
        }
        return doBuildMachine();
    }

    protected abstract Machine doBuildMachine() throws ServerException, ForbiddenException;

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
}
