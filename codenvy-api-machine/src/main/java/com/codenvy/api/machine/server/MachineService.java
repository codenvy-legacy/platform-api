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
import com.codenvy.api.core.ServerException;
import com.codenvy.api.machine.shared.dto.RuntimeMachine;
import com.codenvy.api.machine.shared.dto.ApplicationProcessDescription;
import com.codenvy.api.machine.shared.dto.RuntimeMachineDescription;
import com.codenvy.dto.server.DtoFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

/**
 * Machine API
 *
 * @author Alexander Garagatyi
 */
@Singleton
@Path("/machine")
public class MachineService {
    @Inject
    private MachineRegistry machineRegistry;

    @Path("/{ws-id}")
    @PUT
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public RuntimeMachineDescription createMachine(InputStream recipeStream,
                                @Nullable @QueryParam("type") String machineType,
                                @PathParam("ws-id") String workspace,
                                @QueryParam("project") String project, // TODO make it mandatory
                                @QueryParam("user") String user,
                                @Context SecurityContext context)
            throws ServerException, NotFoundException, ForbiddenException {
        return toDescription(DtoFactory.getInstance().createDto(RuntimeMachine.class)
                                       .withId(machineRegistry.createMachine(recipeStream, machineType, workspace, project, user))
                                       .withWorkspaceId(workspace)
                                       .withProject(project)
                                       .withUser(user)
                                       .withState("creating"),
                             context);
    }

    @Path("/{ws-id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<RuntimeMachineDescription> getMachines(@PathParam("ws-id") String wsId,
                                                @QueryParam("project") String project,
                                                @QueryParam("user") String user,
                                                @Context SecurityContext context)
            throws ServerException {
        final List<RuntimeMachine> machines = machineRegistry.getMachines(wsId, project, user);
        final List<RuntimeMachineDescription> machinesDescriptions = new LinkedList<>();
        for (RuntimeMachine machine : machines) {
            machinesDescriptions.add(toDescription(machine, context));
        }
        return machinesDescriptions;
    }

    //  TODO update image, e.g. remove bad slices

    // TODO add method for image removing from registry

    @Path("/{machineId}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public void destroyMachine(@PathParam("machineId") String id) throws NotFoundException, ServerException {
        machineRegistry.destroy(id);
    }

    @Path("/{machineId}/suspend")
    @POST
    public void suspendMachine(@PathParam("machineId") String machineId) throws NotFoundException, ServerException {
        machineRegistry.suspend(machineId);
    }

    @Path("/{machineId}/resume")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public void resumeMachine(@PathParam("machineId") String machineId) throws NotFoundException, ServerException {
        machineRegistry.resumeMachine(machineId);
    }

    // TODO add channels for process output

    @Path("/{machineId}/run")
    @PUT
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public String executeCommandInMachine(@PathParam("machineId") String id,
                                          @FormParam("command") String command)
            throws NotFoundException, ServerException {
        // returns channel id for process output listening
        return machineRegistry.execute(id, command);
    }

    @Path("/{machineId}/processes")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<ApplicationProcessDescription> getProcesses(@PathParam("machineId") String machineId,
                                                        @Context SecurityContext context)
            throws NotFoundException, ServerException {
        final List<CommandProcess> processes = machineRegistry.getProcesses(machineId);
        final List<ApplicationProcessDescription> processesDescriptions = new LinkedList<>();
        for (CommandProcess process : processes) {
            processesDescriptions.add(toDescription(process, context));
        }
        return processesDescriptions;
    }

    @Path("/{machineId}/kill/{processId}")
    @DELETE
    public void killProcess(@PathParam("machineId") String machineId,
                            @PathParam("processId") int processId)
            throws NotFoundException, ForbiddenException, ServerException {
        machineRegistry.killProcess(machineId, processId);
    }

    private RuntimeMachineDescription toDescription(RuntimeMachine machines, SecurityContext context) {
        return DtoFactory.getInstance().createDto(RuntimeMachineDescription.class)
                         .withId(machines.getId())
                         .withUser(machines.getUser())
                         .withProject(machines.getProject())
                         .withState(machines.getState())
                         .withWorkspaceId(machines.getWorkspaceId())
                         // TODO
                         .withLinks(null);
    }

    private ApplicationProcessDescription toDescription(CommandProcess process, SecurityContext context) throws ServerException {
        return DtoFactory.getInstance().createDto(ApplicationProcessDescription.class)
                         .withId(process.getPid())
                         // TODO
                         .withLinks(null);
    }
}