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
import com.codenvy.api.core.util.WebsocketLineConsumer;
import com.codenvy.api.machine.server.dto.MachineMetaInfo;
import com.codenvy.api.machine.server.dto.Snapshot;
import com.codenvy.api.machine.shared.dto.CommandProcessDescriptor;
import com.codenvy.api.machine.shared.dto.CreateMachineRequest;
import com.codenvy.api.machine.shared.dto.MachineDescriptor;
import com.codenvy.api.machine.shared.dto.SnapshotDescriptor;
import com.codenvy.api.workspace.server.dao.Member;
import com.codenvy.api.workspace.server.dao.MemberDao;
import com.codenvy.commons.env.EnvironmentContext;
import com.codenvy.commons.lang.NamedThreadFactory;
import com.codenvy.dto.server.DtoFactory;

import org.slf4j.Logger;

import javax.annotation.PreDestroy;
import javax.annotation.security.RolesAllowed;
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
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Machine API
 *
 * @author Alexander Garagatyi
 */
@Singleton
@Path("/machine")
public class MachineService {
    private static final Logger LOG = getLogger(MachineService.class);

    private final MachineRegistry machineRegistry;

    private final ExecutorService executorService;

    private final MachineFactories machineFactories;

    private final MemberDao memberDao;

    @Inject
    public MachineService(MachineRegistry machineRegistry,
                          MachineFactories machineFactories,
                          MemberDao memberDao) {
        this.machineRegistry = machineRegistry;
        this.machineFactories = machineFactories;
        this.memberDao = memberDao;
        this.executorService = Executors.newCachedThreadPool(new NamedThreadFactory("MachineRegistry-", true));
    }

    // TODO Save errors somewhere to send to client if client wasn't subscribed to build output

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public MachineDescriptor createMachine(final CreateMachineRequest createMachineRequest)
            throws ServerException, NotFoundException, ForbiddenException {
        if (createMachineRequest.getType() == null) {
            throw new ForbiddenException("Machine type is missing");
        }
        if (createMachineRequest.getReceipt() == null) {
            throw new ForbiddenException("Machine recipe is missing");
        }
        if (createMachineRequest.getWorkspace() == null) {
            throw new ForbiddenException("Workspace is missing");
        }

        final String userId = EnvironmentContext.getCurrent().getUser().getId();
        final Member workspaceMember = memberDao.getWorkspaceMember(createMachineRequest.getWorkspace(), userId);
        if (!workspaceMember.getRoles().contains("workspace/developer") && !workspaceMember.getRoles().contains("workspace/admin")) {
            throw new ForbiddenException("You don't have access to create machine in specified workspace");
        }

        final LineConsumer lineConsumer;
        if (createMachineRequest.getOutputChannel() != null) {
            lineConsumer = new WebsocketLineConsumer(createMachineRequest.getOutputChannel());
        } else {
            lineConsumer = LineConsumer.DEV_NULL;
        }

        final MachineBuilder machineBuilder = machineFactories.get(createMachineRequest.getType()).newMachineBuilder()
                                                              .setRecipe(new BaseMachineRecipe() {
                                                                  @Override
                                                                  public String asString() {
                                                                      return createMachineRequest.getReceipt();
                                                                  }
                                                              })
                                                              .setOutputConsumer(lineConsumer);

        final MachineMetaInfo machineMetaInfo = DtoFactory.getInstance().createDto(MachineMetaInfo.class)
                                                      .withId(machineBuilder.getMachineId())
                                                      .withUserId(userId)
                                                      .withWorkspaceId(createMachineRequest.getWorkspace())
                                                      .withDisplayName(createMachineRequest.getDisplayName())
                                                      .withType(createMachineRequest.getType());

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    machineBuilder.build();

                    machineRegistry.addMachine(machineMetaInfo);

                } catch (ServerException | ForbiddenException e) {
                    try {
                        lineConsumer.writeLine(e.getLocalizedMessage());
                    } catch (IOException e1) {
                        LOG.error(e1.getLocalizedMessage(), e1);
                    }
                }
            }
        });

        return toDescriptor(machineMetaInfo.getId(),
                            machineMetaInfo.getUserId(),
                            machineMetaInfo.getWorkspaceId(),
                            machineMetaInfo.getDisplayName(),
                            Machine.State.CREATING,
                            null);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public List<MachineDescriptor> getMachines(@QueryParam("workspaceId") String workspaceId,
                                               @QueryParam("project") String project,
                                               @Context SecurityContext context)
            throws ServerException, ForbiddenException {
        if (workspaceId == null) {
            throw new ForbiddenException("Workspace parameter is missing");
        }

        final String userId = EnvironmentContext.getCurrent().getUser().getId();

        final List<Machine> machines = machineRegistry.getMachines(workspaceId, project, userId);
        final List<MachineDescriptor> machinesDescriptors = new LinkedList<>();
        for (Machine machine : machines) {
            machinesDescriptors.add(toDescriptor(machine, userId, workspaceId));
        }
        return machinesDescriptors;
    }

    @Path("/{machineId}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public void destroyMachine(@PathParam("machineId") String machineId) throws NotFoundException, ServerException {
        final Machine machine = machineRegistry.getMachine(machineId);

        machine.destroy();
    }

    @Path("/{machineId}/suspend")
    @POST
    @RolesAllowed("user")
    public void suspendMachine(@PathParam("machineId") String machineId) throws NotFoundException, ServerException {
        final Machine machine = machineRegistry.getMachine(machineId);

//        machine.suspend();
    }

    @Path("/{machineId}/resume")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @RolesAllowed("user")
    public void resumeMachine(@PathParam("machineId") String machineId) throws NotFoundException, ServerException {
        final Machine machine = machineRegistry.getMachine(machineId);

        executorService.execute(new Runnable() {
            @Override
            public void run() {
//                try {
//                    machine.resume();
//                } catch (ServerException e) {
//                    TODO put to websocket
//                }
            }
        });
    }

    // TODO add channels for process output

    @Path("/{machineId}/run")
    @PUT
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    @RolesAllowed("user")
    public CommandProcessDescriptor executeCommandInMachine(@PathParam("machineId") String machineId,
                                                            @FormParam("command") final String command,
                                                            @Context SecurityContext context)
            throws NotFoundException, ServerException {
        // returns channel id for process output listening
        final Machine machine = machineRegistry.getMachine(machineId);

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

        // TODO how to read process output on client? Should we generate pid here?
        return toDescriptor(42);
    }

    @Path("/{machineId}/processes")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public List<CommandProcessDescriptor> getProcesses(@PathParam("machineId") String machineId,
                                                       @Context SecurityContext context)
            throws NotFoundException, ServerException {
        final Machine machine = machineRegistry.getMachine(machineId);
        final List<CommandProcess> processes = machine.getRunningProcesses();
        final List<CommandProcessDescriptor> processesDescriptors = new LinkedList<>();
        for (CommandProcess process : processes) {
            // TODO how can user recognize process?
            processesDescriptors.add(toDescriptor(process.getPid()));
        }
        return processesDescriptors;
    }

    @Path("/{machineId}/kill/{processId}")
    @DELETE
    @RolesAllowed("user")
    public void killProcess(@PathParam("machineId") String machineId,
                            @PathParam("processId") int processId)
            throws NotFoundException, ForbiddenException, ServerException {
        final Machine machine = machineRegistry.getMachine(machineId);

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

    @Path("/{machineId}/bind/{workspaceId}/{project}")
    @POST
    @RolesAllowed("user")
    public void bindProject(@PathParam("machineId") String machineId,
                            @PathParam("workspaceId") String workspace,
                            @PathParam("project") String project) throws NotFoundException, ServerException {
        final Machine machine = machineRegistry.getMachine(machineId);

        machine.bind(workspace, project);
    }

    @Path("/{machineId}/unbind/{workspaceId}/{project}")
    @POST
    @RolesAllowed("user")
    public void unbindProject(@PathParam("machineId") String machineId,
                              @PathParam("workspaceId") String workspace,
                              @PathParam("project") String project) throws NotFoundException, ServerException {
        final Machine machine = machineRegistry.getMachine(machineId);

        machine.unbind(workspace, project);
    }

    private MachineDescriptor toDescriptor(String id,
                                           String userId,
                                           String workspaceId,
                                           String displayName,
                                           Machine.State machineState,
                                           List<Snapshot> snapshots)
            throws ServerException {
        List<SnapshotDescriptor> snapshotDescriptors = new LinkedList<>();
        if (snapshots != null) {
            for (Snapshot snapshot : snapshots) {
                snapshotDescriptors.add(DtoFactory.getInstance().createDto(SnapshotDescriptor.class)
                                                  .withId(snapshot.getId())
                                                  .withDescription(snapshot.getDescription())
                                                  .withLinks(null)); // TODO
            }
        }

        return DtoFactory.getInstance().createDto(MachineDescriptor.class)
                         .withId(id)
                         .withUserId(userId)
                         .withState(machineState)
                         .withWorkspaceId(workspaceId)
                         .withDisplayName(displayName)
                         .withSnapshots(snapshotDescriptors)
                         // TODO
                         .withLinks(null);
    }

    private MachineDescriptor toDescriptor(Machine machine,
                                           String user,
                                           String workspace) throws ServerException {
        return toDescriptor(machine.getId(), user, workspace, machine.getDisplayName(), machine.getState(), machine.getSnapshots());

    }

    private CommandProcessDescriptor toDescriptor(int processId) throws ServerException {
        return DtoFactory.getInstance().createDto(CommandProcessDescriptor.class)
                .withId(processId)
                        // TODO
                .withLinks(null);
    }

    @PreDestroy
    private void cleanUp() {
        executorService.shutdownNow();
    }
}