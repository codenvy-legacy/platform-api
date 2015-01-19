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
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.machine.shared.dto.ApplicationProcessDescriptor;
import com.codenvy.api.machine.shared.dto.MachineDescriptor;
import com.codenvy.dto.server.DtoFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.InputStream;
import java.util.List;

/**
 * Machine API
 *
 * @author Alexander Garagatyi
 */
@Path("/machine")
public class MachineService {
    @Inject
    private MachineBuilder machineBuilder;

    @Inject
    private MachineRegistry machineRegistry;

    @PUT
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public MachineDescriptor createMachine(final InputStream recipeStream) throws BuildMachineException {
        final Machine machine = machineBuilder.setRecipe(new BaseMachineRecipe() {
            @Override
            public InputStream asStream() {
                return recipeStream;
            }
        }).buildMachine();

        machineRegistry.putMachine(machine);

        return DtoFactory.getInstance().createDto(MachineDescriptor.class).withId(machine.getId());
    }

    @Path("/{machineId}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public void destroyMachine(@PathParam("machineId") String id) throws NotFoundException {
        final Machine machine = machineRegistry.getMachine(id);
        machine.destroy();
    }

    @Path("/{machineId}/run")
    @PUT
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public ApplicationProcessDescriptor executeCommandInMachine(@PathParam("machineId") String id,
                                                                @FormParam("command") String command) throws NotFoundException {
        final Machine machine = machineRegistry.getMachine(id);
        final CommandProcess commandProcess = machine.newCommandProcess(command);

        return DtoFactory.getInstance().createDto(ApplicationProcessDescriptor.class).withId(commandProcess.getId());
    }

    @Path("/{machineId}/kill/{processId}")
    @DELETE
    public void killProcess(@PathParam("machineId") String machineId,
                            @PathParam("processId") long processId) throws NotFoundException, ForbiddenException {
        final Machine machine = machineRegistry.getMachine(machineId);
        for (CommandProcess commandProcess : machine.getRunningProcesses()) {
            if (commandProcess.getId() == processId) {
                if (!process.isAlive()) {
                    throw new ForbiddenException("Process finished already");
                }
                process.kill();
                return;
            }
        }
        throw new NotFoundException(String.format("Process with id %s not found in machine %s.", processId, machineId));
    }

    @Path("/{machineId}/suspend")
    @POST
    public void suspendMachine(@PathParam("machineId") String machineId) throws NotFoundException {
        final Machine machine = machineRegistry.getMachine(machineId);

        machine.suspend();
    }

    @Path("/{machineId}/resume")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public MachineDescriptor resumeMachine(@PathParam("machineId") String machineId) throws NotFoundException {
        final Machine machine = machineRegistry.getMachine(machineId);

        machine.resume();

        return DtoFactory.getInstance().createDto(MachineDescriptor.class).withId(machineId);
    }

    @Path("/{machineId}/processes")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<CommandProcess> getProcesses(@PathParam("machineId") String machineId) throws NotFoundException {
        final Machine machine = machineRegistry.getMachine(machineId);
        return machine.getRunningProcesses();
    }
}