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
import com.codenvy.api.machine.server.dto.Snapshot;
import com.codenvy.api.machine.shared.dto.Command;
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
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
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

    private final ExecutorService executorService;

    private final Machines machines;

    private final MemberDao memberDao;

    private final MachineMetaInfoDao machineMetaInfoDao;

    @Inject
    public MachineService(Machines machines,
                          MemberDao memberDao,
                          MachineMetaInfoDao machineMetaInfoDao) {
        this.machines = machines;
        this.memberDao = memberDao;
        this.machineMetaInfoDao = machineMetaInfoDao;
        this.executorService = Executors.newCachedThreadPool(new NamedThreadFactory("MachineRegistry-", true));
    }

    // TODO Save errors somewhere to send to client if client wasn't subscribed to build output

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public MachineDescriptor startMachine(final CreateMachineRequest createMachineRequest)
            throws ServerException, ForbiddenException, NotFoundException {
        requiredNotNull(createMachineRequest.getType(), "Machine type");
        requiredNotNull(createMachineRequest.getReceipt(), "Machine recipe");
        requiredNotNull(createMachineRequest.getWorkspace(), "Workspace parameter");

        final String userId = EnvironmentContext.getCurrent().getUser().getId();
        checkPermissions(userId, createMachineRequest.getWorkspace());

        final LineConsumer lineConsumer;
        if (createMachineRequest.getOutputChannel() != null) {
            lineConsumer = new WebsocketLineConsumer(createMachineRequest.getOutputChannel());
        } else {
            lineConsumer = LineConsumer.DEV_NULL;
        }

        final MachineBuilder machineBuilder = machines.newMachineOfType(createMachineRequest.getType())
                                                      .setRecipe(new BaseMachineRecipe() {
                                                          @Override
                                                          public String asString() {
                                                              return createMachineRequest.getReceipt();
                                                          }
                                                      })
                                                      .setCreatedBy(userId)
                                                      .setDisplayName(createMachineRequest.getDisplayName())
                                                      .setWorkspaceId(createMachineRequest.getWorkspace())
                                                      .setOutputConsumer(lineConsumer);

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final Machine machine = machineBuilder.build();

                    machine.start();

                } catch (ServerException | ForbiddenException e) {
                    try {
                        lineConsumer.writeLine(e.getLocalizedMessage());
                    } catch (IOException e1) {
                        LOG.error(e1.getLocalizedMessage(), e1);
                    }
                }
            }
        });

        return toDescriptor(machineBuilder.getMachineId(),
                            createMachineRequest.getType(),
                            userId,
                            createMachineRequest.getWorkspace(),
                            createMachineRequest.getDisplayName(),
                            Machine.State.CREATING,
                            null);
    }

    @Path("/{machineId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public MachineDescriptor getMachineById(@PathParam("machineId") String machineId)
            throws ServerException, ForbiddenException, NotFoundException {
        final Machine machine = machines.getMachine(machineId);
        final String machineCreator = machine.getCreatedBy();

        checkCurrentUserPermissionsForMachine(machineCreator);

        return toDescriptor(machineId,
                            machine.getType(),
                            machineCreator,
                            machine.getWorkspaceId(),
                            machine.getDisplayName(),
                            machine.getState(),
                            machine.getSnapshots());
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public List<MachineDescriptor> getMachines(@QueryParam("workspaceId") String workspaceId,
                                               @QueryParam("project") String project)
            throws ServerException, ForbiddenException {
        requiredNotNull(workspaceId, "Workspace parameter");

        final String userId = EnvironmentContext.getCurrent().getUser().getId();
        checkPermissions(userId, workspaceId);

        final List<MachineDescriptor> machinesDescriptors = new LinkedList<>();
        final List<Machine> existingMachines = machines.getMachines(userId, workspaceId, project);
        for (Machine machine : existingMachines) {
            machinesDescriptors.add(toDescriptor(machine.getId(),
                                                 machine.getType(),
                                                 userId,
                                                 workspaceId,
                                                 machine.getDisplayName(),
                                                 machine.getState(),
                                                 machine.getSnapshots()));
        }

        return machinesDescriptors;
    }

    @Path("/{machineId}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public void destroyMachine(@PathParam("machineId") String machineId) throws NotFoundException, ServerException, ForbiddenException {
        final Machine machine = machines.getMachine(machineId);

        checkCurrentUserPermissionsForMachine(machine.getCreatedBy());

        for (Snapshot snapshot : machine.getSnapshots()) {
            machine.removeSnapshot(snapshot.getId());
        }

        machine.destroy();

        machineMetaInfoDao.remove(machineId);
    }

    @Path("/{machineId}/run")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public void executeCommandInMachine(@PathParam("machineId") String machineId,
                                                            final Command command)
            throws NotFoundException, ServerException, ForbiddenException {
        // TODO how to use command name?

        final Machine machine = machines.getMachine(machineId);
        checkCurrentUserPermissionsForMachine(machine.getCreatedBy());

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                final WebsocketLineConsumer websocketLineConsumer = new WebsocketLineConsumer(command.getOutputChannel());
                final CommandProcess commandProcess = machine.newCommandProcess(command.getCommandLine());
                try {
                    commandProcess.start(websocketLineConsumer);
                } catch (ConflictException | ServerException e) {
                    try {
                        websocketLineConsumer.writeLine(e.getLocalizedMessage());
                    } catch (IOException e1) {
                        LOG.error(e.getLocalizedMessage(), e);
                    }
                }
            }
        });
    }

    @Path("/{machineId}/processes")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public List<CommandProcessDescriptor> getProcesses(@PathParam("machineId") String machineId)
            throws NotFoundException, ServerException, ForbiddenException {
        final Machine machine = machines.getMachine(machineId);
        checkCurrentUserPermissionsForMachine(machine.getCreatedBy());

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
        final Machine machine = machines.getMachine(machineId);
        checkCurrentUserPermissionsForMachine(machine.getCreatedBy());

        for (CommandProcess commandProcess : machine.getRunningProcesses()) {
            if (commandProcess.getPid() == processId) {
                if (!commandProcess.isAlive()) {
                    throw new ForbiddenException("Process finished already");
                }
                // TODO should we do that in separate thread?
                // E.g. if soft kill won't stop the process request will be finished by timeout
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
                            @PathParam("project") String project) throws NotFoundException, ServerException, ForbiddenException {
        final Machine machine = machines.getMachine(machineId);
        checkCurrentUserPermissionsForMachine(machine.getCreatedBy());
        // TODO check user's permissions fot project

        machine.bind(workspace, project);
    }

    @Path("/{machineId}/unbind/{workspaceId}/{project}")
    @POST
    @RolesAllowed("user")
    public void unbindProject(@PathParam("machineId") String machineId,
                              @PathParam("workspaceId") String workspace,
                              @PathParam("project") String project) throws NotFoundException, ServerException, ForbiddenException {
        final Machine machine = machines.getMachine(machineId);
        checkCurrentUserPermissionsForMachine(machine.getCreatedBy());

        machine.unbind(workspace, project);
    }

    /**
     * Checks object reference is not {@code null}
     *
     * @param object
     *         object reference to check
     * @param subject
     *         used as subject of exception message "{subject} required"
     * @throws ForbiddenException
     *         when object reference is {@code null}
     */
    private void requiredNotNull(Object object, String subject) throws ForbiddenException {
        if (object == null) {
            throw new ForbiddenException(subject + " required");
        }
    }

    private void checkCurrentUserPermissionsForMachine(String machineCreator) throws ForbiddenException {
        final String userId = EnvironmentContext.getCurrent().getUser().getId();
        if (!userId.equals(machineCreator)) {
            throw new ForbiddenException("Operation is not permitted");
        }
    }

    private void checkPermissions(String userId, String workspaceId) throws ServerException, ForbiddenException {
        final Member workspaceMember;
        try {
            workspaceMember = memberDao.getWorkspaceMember(workspaceId, userId);
        } catch (NotFoundException e) {
            throw new ForbiddenException("Operation is not permitted");
        }
        if (!workspaceMember.getRoles().contains("workspace/developer") && !workspaceMember.getRoles().contains("workspace/admin")) {
            throw new ForbiddenException("Operation is not permitted");
        }
    }

    private MachineDescriptor toDescriptor(String id,
                                           String machineType,
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
                         .withType(machineType)
                         .withUserId(userId)
                         .withState(machineState)
                         .withWorkspaceId(workspaceId)
                         .withDisplayName(displayName)
                         .withSnapshots(snapshotDescriptors)
                         // TODO
                         .withLinks(null);
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