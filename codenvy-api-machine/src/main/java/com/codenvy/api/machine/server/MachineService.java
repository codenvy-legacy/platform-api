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
import com.codenvy.api.machine.shared.dto.NewSnapshot;
import com.codenvy.api.machine.shared.dto.SnapshotDescriptor;
import com.codenvy.api.machine.shared.dto.StartMachineRequest;
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
    private final Machines        machines;
    private final MemberDao       memberDao;

    @Inject
    public MachineService(Machines machines, MemberDao memberDao) {
        this.machines = machines;
        this.memberDao = memberDao;
        this.executorService = Executors.newCachedThreadPool(new NamedThreadFactory("MachineService-", true));
    }

    // TODO Save errors somewhere to send to client if client wasn't subscribed to build output

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public MachineDescriptor startMachineFromRecipe(final CreateMachineRequest createMachineRequest)
            throws ServerException, ForbiddenException, NotFoundException {
        requiredNotNull(createMachineRequest.getType(), "Machine type");
        requiredNotNull(createMachineRequest.getRecipe(), "Machine recipe");
        requiredNotNull(createMachineRequest.getWorkspace(), "Workspace parameter");
        checkCurrentUserPermissionsForWorkspace(createMachineRequest.getWorkspace());

        // TODO
        // client have to remember output channel
        // what if we put logs to this channel at the beginning. When machine will have an id we starts logging to channel based on machine id
        // and put administrative message with url of the new channel to old channel
        final LineConsumer lineConsumer;
        if (createMachineRequest.getOutputChannel() != null) {
            lineConsumer = new WebsocketLineConsumer(createMachineRequest.getOutputChannel());
        } else {
            lineConsumer = LineConsumer.DEV_NULL;
        }

        final String userId = EnvironmentContext.getCurrent().getUser().getId();

        final MachineBuilder machineBuilder = machines.newMachineOfType(createMachineRequest.getType())
                                                      .setRecipe(new BaseMachineRecipe() {
                                                          @Override
                                                          public String asString() {
                                                              return createMachineRequest.getRecipe();
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
                            null,
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
                            machine.getProjects(),
                            machine.getSnapshots());
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public List<MachineDescriptor> getMachines(@QueryParam("workspaceId") String workspaceId,
                                               @QueryParam("project") String project)
            throws ServerException, ForbiddenException {
        requiredNotNull(workspaceId, "Workspace parameter");
        checkCurrentUserPermissionsForWorkspace(workspaceId);

        final String userId = EnvironmentContext.getCurrent().getUser().getId();

        final List<MachineDescriptor> machinesDescriptors = new LinkedList<>();
        final List<Machine> existingMachines = machines.getMachines(userId, workspaceId, project);
        for (Machine machine : existingMachines) {
            machinesDescriptors.add(toDescriptor(machine.getId(),
                                                 machine.getType(),
                                                 machine.getCreatedBy(),
                                                 workspaceId,
                                                 machine.getDisplayName(),
                                                 machine.getState(),
                                                 machine.getProjects(),
                                                 machine.getSnapshots()));
        }

        return machinesDescriptors;
    }

    @Path("/{machineId}/stop")
    @POST
    @RolesAllowed("user")
    public void stopMachine(@PathParam("machineId") String machineId) throws NotFoundException, ServerException, ForbiddenException {
        final Machine machine = machines.getMachine(machineId);

        checkCurrentUserPermissionsForMachine(machine.getCreatedBy());

        machine.saveSnapshot("latest");
        machine.stop();
    }

    @Path("/{machineId}/resume")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public void startMachineFromSnapshot(@PathParam("machineId") String machineId, final StartMachineRequest startMachineRequest)
            throws ForbiddenException, NotFoundException, ServerException {
        final Machine machine = machines.getMachine(machineId);
        final String machineCreator = machine.getCreatedBy();

        checkCurrentUserPermissionsForMachine(machineCreator);

        // TODO
        // client have to remember output channel
        // what if we put logs to channel based on machine id or put log channel to links
        final LineConsumer lineConsumer;
        if (startMachineRequest.getOutputChannel() != null) {
            lineConsumer = new WebsocketLineConsumer(startMachineRequest.getOutputChannel());
        } else {
            lineConsumer = LineConsumer.DEV_NULL;
        }

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    machine.setOutputConsumer(lineConsumer);
                    machine.restoreToSnapshot(startMachineRequest.getSnapshot() == null ? "latest" : startMachineRequest.getSnapshot());
                } catch (ServerException e) {
                    try {
                        lineConsumer.writeLine(e.getLocalizedMessage());
                    } catch (IOException e1) {
                        LOG.error(e.getLocalizedMessage(), e);
                    }
                }
            }
        });
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
    }

    @Path("/{machineId}/run")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public void executeCommandInMachine(@PathParam("machineId") String machineId, final Command command)
            throws NotFoundException, ServerException, ForbiddenException {
        final Machine machine = machines.getMachine(machineId);
        checkCurrentUserPermissionsForMachine(machine.getCreatedBy());

        final LineConsumer lineConsumer;
        if (command.getOutputChannel() != null) {
            lineConsumer = new WebsocketLineConsumer(command.getOutputChannel());
        } else {
            lineConsumer = LineConsumer.DEV_NULL;
        }

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                final CommandProcess commandProcess = machine.newCommandProcess(command.getCommandLine());
                try {
                    commandProcess.start(lineConsumer);
                } catch (ConflictException | ServerException e) {
                    try {
                        lineConsumer.writeLine(e.getLocalizedMessage());
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
            processesDescriptors.add(toDescriptor(process.getPid(), process.getCommandLine()));
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
                // E.g. if soft kill won't stop the process, request will be finished by timeout
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
        // TODO check user's permissions for project

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

    @Path("/{machineId}/snapshot")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public void saveSnapshot(@PathParam("machineId") String machineId,
                             NewSnapshot newSnapshot) throws NotFoundException, ServerException, ForbiddenException {
        final Machine machine = machines.getMachine(machineId);
        checkCurrentUserPermissionsForMachine(machine.getCreatedBy());

        machine.saveSnapshot(newSnapshot.getDescription());
    }

    @Path("/{machineId}/snapshot/{snapshotId}")
    @DELETE
    @RolesAllowed("user")
    public void removeSnapshot(@PathParam("machineId") String machineId,
                               @PathParam("snapshotId") String snapshotId) throws NotFoundException, ServerException, ForbiddenException {
        final Machine machine = machines.getMachine(machineId);
        checkCurrentUserPermissionsForMachine(machine.getCreatedBy());

        machine.removeSnapshot(snapshotId);
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

    private void checkCurrentUserPermissionsForWorkspace(String workspaceId) throws ServerException, ForbiddenException {
        final String userId = EnvironmentContext.getCurrent().getUser().getId();
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
                                           String machineCreator,
                                           String workspaceId,
                                           String displayName,
                                           Machine.State machineState,
                                           List<String> projects,
                                           List<Snapshot> snapshots)
            throws ServerException {
        List<SnapshotDescriptor> snapshotDescriptors = new LinkedList<>();
        if (snapshots != null) {
            for (Snapshot snapshot : snapshots) {
                snapshotDescriptors.add(DtoFactory.getInstance().createDto(SnapshotDescriptor.class)
                                                  .withId(snapshot.getId())
                                                  .withDate(snapshot.getDate())
                                                  .withDescription(snapshot.getDescription())
                                                  .withLinks(null)); // TODO
            }
        }

        return DtoFactory.getInstance().createDto(MachineDescriptor.class)
                         .withId(id)
                         .withType(machineType)
                         .withCreatedBy(machineCreator)
                         .withState(machineState)
                         .withWorkspaceId(workspaceId)
                         .withDisplayName(displayName)
                         .withSnapshots(snapshotDescriptors)
                         .withProjects(projects)
                         // TODO
                         .withLinks(null);
    }


    private CommandProcessDescriptor toDescriptor(int processId, String commandLine) throws ServerException {
        return DtoFactory.getInstance().createDto(CommandProcessDescriptor.class)
                .withId(processId)
                .withCommandLine(commandLine)
                        // TODO
                .withLinks(null);
    }

    @PreDestroy
    private void cleanUp() {
        executorService.shutdownNow();
    }
}