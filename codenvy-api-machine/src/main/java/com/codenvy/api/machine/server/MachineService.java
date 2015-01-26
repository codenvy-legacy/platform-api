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

import com.codenvy.api.core.ConflictException;
import com.codenvy.api.core.ForbiddenException;
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.core.util.LineConsumer;
import com.codenvy.api.machine.server.dto.StoredMachine;
import com.codenvy.api.machine.shared.dto.ApplicationProcessDescriptor;
import com.codenvy.api.machine.shared.dto.MachineDescriptor;
import com.codenvy.commons.lang.NameGenerator;
import com.codenvy.commons.lang.NamedThreadFactory;
import com.codenvy.dto.server.DtoFactory;

import org.everrest.core.impl.provider.json.JsonUtils;
import org.everrest.websockets.WSConnectionContext;
import org.everrest.websockets.message.ChannelBroadcastMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.annotation.PreDestroy;
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
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Machine API
 *
 * @author Alexander Garagatyi
 */
@Singleton
@Path("/machine")
public class MachineService {
    private final MachineRegistry machineRegistry;

    private final ExecutorService executorService;

    private final MachineBuilderFactoryRegistry machineBuilderFactoryRegistry;

    @Inject
    public MachineService(MachineRegistry machineRegistry,
                          MachineBuilderFactoryRegistry machineBuilderFactoryRegistry) {
        this.machineRegistry = machineRegistry;
        this.machineBuilderFactoryRegistry = machineBuilderFactoryRegistry;
        this.executorService = Executors.newCachedThreadPool(new NamedThreadFactory("MachineRegistry-", true));
    }

    // TODO add ability to check recipe before machine build execution to throw exception synchronously if recipe is not found or valid
    // will help in situations when build fails before client will be able to subscribe to output of build

    // TODO add ability to name machines

    // TODO Save errors somewhere to send to client if client wasn't subscribed to build output

    @Path("/{ws-id}")
    @PUT
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public MachineDescriptor createMachine(final InputStream recipeStream,
                                           @Nullable @QueryParam("type") String machineType,
                                           @PathParam("ws-id") final String workspace,
                                           @QueryParam("project") final String project, // TODO make it mandatory
                                           @QueryParam("user") final String user,
                                           @Context SecurityContext context)
            throws ServerException, NotFoundException, ForbiddenException {
        final MachineBuilder machineBuilder = machineBuilderFactoryRegistry.get(machineType).newMachineBuilder()
                                                                           .setRecipe(new BaseMachineRecipe() {
                                                                               @Override
                                                                               public InputStream asStream() {
                                                                                   return recipeStream;
                                                                               }
                                                                           });
        final MachineOutputWsLineConsumer machineOutputWsLineConsumer = new MachineOutputWsLineConsumer(machineBuilder.getMachineId());

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final Machine machine = machineBuilder.buildMachine(machineOutputWsLineConsumer);

                    // TODO get snapshots from machine and put them to Dao

                    machineRegistry.addMachine(DtoFactory.getInstance().createDto(StoredMachine.class)
                                                         .withId(machineBuilder.getMachineId())
                                                         .withUser(user)
                                                         .withWorkspaceId(workspace)
                                                         .withProject(project), machine);

                } catch (ServerException | ForbiddenException e) {
                    // TODO put to websocket
                }
            }
        });

        return toDescriptor(DtoFactory.getInstance().createDto(MachineDescriptor.class)
                                      .withId(machineBuilder.getMachineId())
                                      .withWorkspaceId(workspace)
                                      .withProject(project)
                                      .withUser(user)
                                      .withState(Machine.Type.CREATING),
                            context);
    }

    @Path("/{ws-id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<MachineDescriptor> getMachines(@PathParam("ws-id") String wsId,
                                               @QueryParam("project") String project,
                                               @QueryParam("user") String user,
                                               @Context SecurityContext context)
            throws ServerException {


        final List<MachineDescriptor> machines = machineRegistry.getMachines(wsId, project, user);
        final List<MachineDescriptor> machinesDescriptions = new LinkedList<>();
        for (MachineDescriptor machine : machines) {
            machinesDescriptions.add(toDescriptor(machine, context));
        }
        return machinesDescriptions;
    }

    //  TODO update image, e.g. remove bad slices

    // TODO add method for image removing from registry

    @Path("/{machineId}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public void destroyMachine(@PathParam("machineId") String machineId) throws NotFoundException, ServerException {
        final Machine machine = machineRegistry.getActiveMachine(machineId);

        machine.destroy();

        machineRegistry.removeActiveMachine(machineId);
    }

    @Path("/{machineId}/suspend")
    @POST
    public void suspendMachine(@PathParam("machineId") String machineId) throws NotFoundException, ServerException {
        final Machine machine = machineRegistry.getActiveMachine(machineId);

        machine.suspend();

        machineRegistry.removeActiveMachine(machineId);
    }

    @Path("/{machineId}/resume")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public void resumeMachine(@PathParam("machineId") String machineId) throws NotFoundException, ServerException {
//        final Machine machine = machineRegistry.getActiveMachine(machineId);
//
//        machine.resume();
    }

    // TODO add channels for process output

    @Path("/{machineId}/run")
    @PUT
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public String executeCommandInMachine(@PathParam("machineId") String id,
                                          @FormParam("command") final String command)
            throws NotFoundException, ServerException {
        // returns channel id for process output listening
        final Machine machine = machineRegistry.getActiveMachine(id);

        final String outputChannelId = NameGenerator.generate("", 16);
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                final CommandProcess commandProcess = machine.newCommandProcess(command);
                try {
                    commandProcess.start();
                } catch (ConflictException | ServerException e) {
                    // TODO put to websocket
                }
            }
        });

        return outputChannelId;
    }

    @Path("/{machineId}/processes")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<ApplicationProcessDescriptor> getProcesses(@PathParam("machineId") String machineId,
                                                           @Context SecurityContext context)
            throws NotFoundException, ServerException {
        final Machine machine = machineRegistry.getActiveMachine(machineId);
        final List<CommandProcess> processes = machine.getRunningProcesses();
        final List<ApplicationProcessDescriptor> processesDescriptors = new LinkedList<>();
        for (CommandProcess process : processes) {
            processesDescriptors.add(toDescriptor(process, context));
        }
        return processesDescriptors;
    }

    @Path("/{machineId}/kill/{processId}")
    @DELETE
    public void killProcess(@PathParam("machineId") String machineId,
                            @PathParam("processId") int processId)
            throws NotFoundException, ForbiddenException, ServerException {
        final Machine machine = machineRegistry.getActiveMachine(machineId);

        for (CommandProcess commandProcess : machine.getRunningProcesses()) {
            if (commandProcess.getPid() == processId) {
                if (!commandProcess.isAlive()) {
                    throw new ForbiddenException("Process finished already");
                }
                // TODO should we do that in separate thread?
                commandProcess.kill();
                return;
            }
        }
        throw new NotFoundException(String.format("Process with id %s not found in machine %s.", processId, machineId));
    }

    private MachineDescriptor toDescriptor(MachineDescriptor machines, SecurityContext context) {
        return DtoFactory.getInstance().createDto(MachineDescriptor.class)
                         .withId(machines.getId())
                         .withUser(machines.getUser())
                         .withProject(machines.getProject())
                         .withState(machines.getState())
                .withWorkspaceId(machines.getWorkspaceId())
                        // TODO
                .withLinks(null);
    }

    private ApplicationProcessDescriptor toDescriptor(CommandProcess process, SecurityContext context) throws ServerException {
        return DtoFactory.getInstance().createDto(ApplicationProcessDescriptor.class)
                .withId(process.getPid())
                        // TODO
                .withLinks(null);
    }

    private static class MachineOutputWsLineConsumer implements LineConsumer {
        private static final Logger LOG = LoggerFactory.getLogger(MachineOutputWsLineConsumer.class);

        private final String machineId;

        public MachineOutputWsLineConsumer(String machineId) {
            this.machineId = machineId;
        }

        @Override
        public void writeLine(String line) throws IOException {
            final ChannelBroadcastMessage bm = new ChannelBroadcastMessage();
            bm.setChannel("machineLogs:output:" + machineId);
            bm.setBody(JsonUtils.getJsonString(line));
            try {
                WSConnectionContext.sendMessage(bm);
            } catch (Exception e) {
                LOG.error("A problem occurred while sending websocket message", e);
            }
        }

        @Override
        public void close() throws IOException {

        }
    }

    @PreDestroy
    private void cleanUp() {
        executorService.shutdownNow();
    }
}