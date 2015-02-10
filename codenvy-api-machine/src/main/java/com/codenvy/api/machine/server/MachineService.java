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
import com.codenvy.api.core.util.LineConsumer;
import com.codenvy.api.core.util.WebsocketLineConsumer;
import com.codenvy.api.machine.shared.*;
import com.codenvy.api.machine.shared.dto.CreateMachineFromRecipe;
import com.codenvy.api.machine.shared.dto.CreateMachineFromSnapshot;
import com.codenvy.api.machine.shared.dto.MachineDescriptor;
import com.codenvy.api.machine.shared.dto.NewCommand;
import com.codenvy.api.machine.shared.dto.ProcessDescriptor;
import com.codenvy.api.workspace.server.dao.Member;
import com.codenvy.api.workspace.server.dao.MemberDao;
import com.codenvy.commons.env.EnvironmentContext;
import com.codenvy.dto.server.DtoFactory;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Machine API
 *
 * @author Alexander Garagatyi
 */
@Path("/machine")
public class MachineService {
    @Inject
    private MachineManager machineManager;

    @Inject
    private MemberDao memberDao;

    // TODO Save errors somewhere to send to client if client wasn't subscribed to build output

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public MachineDescriptor createMachineFromRecipe(final CreateMachineFromRecipe createMachineRequest)
            throws ServerException, ForbiddenException, NotFoundException {
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
                                                           // TODO
        final MachineImpl machine = machineManager.create(new RecipeId(), EnvironmentContext.getCurrent().getUser().getId(), lineConsumer);

        return toDescriptor(machine.getId(),
                            machine.getType(),
                            machine.getOwner(),
                            Collections.<ProjectBinding>emptySet(),
                            machine.getState());
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public void createMachineFromSnapshot(CreateMachineFromSnapshot createMachineRequest)
            throws ForbiddenException, NotFoundException, ServerException {

        // todo how to check access rights?

        final LineConsumer lineConsumer;
        if (createMachineRequest.getOutputChannel() != null) {
            lineConsumer = new WebsocketLineConsumer(createMachineRequest.getOutputChannel());
        } else {
            lineConsumer = LineConsumer.DEV_NULL;
        }
        machineManager.create(createMachineRequest.getSnapshotId(), EnvironmentContext.getCurrent().getUser().getId(), lineConsumer);
    }

    @Path("/{machineId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public MachineDescriptor getMachineById(@PathParam("machineId") String machineId)
            throws ServerException, ForbiddenException, NotFoundException {
        final MachineImpl machine = machineManager.getMachine(machineId);

        checkCurrentUserPermissionsForMachine(machine.getOwner());

        return toDescriptor(machineId,
                            machine.getType(),
                            machine.getOwner(),
                            machine.getProjectBindings(),
//                            machine.getDisplayName(),
                            machine.getState());
//                            machine.getProjects(),
//                            machine.getSnapshots());
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public List<MachineDescriptor> getMachines(@QueryParam("workspaceId") String workspaceId,
                                               @QueryParam("path") String path)
            throws ServerException, ForbiddenException {
        requiredNotNull(workspaceId, "Workspace parameter");
        checkCurrentUserPermissionsForWorkspace(workspaceId);

        final String userId = EnvironmentContext.getCurrent().getUser().getId();
        final List<MachineImpl> machines = machineManager.getMachines(userId, DtoFactory.getInstance().createDto(ProjectBinding.class)
                                                                                        .withWorkspaceId(workspaceId)
                                                                                        .withPath(path));


        final List<MachineDescriptor> machinesDescriptors = new LinkedList<>();
        for (MachineImpl machine : machines) {
            machinesDescriptors.add(toDescriptor(machine.getId(),
                                                 machine.getType(),
                                                 machine.getOwner(),
                                                 machine.getProjectBindings(),
//                                                 workspaceId,
//                                                 machine.getDisplayName(),
                                                 machine.getState()
//                                                 machine.getProjects(),
//                                                 machine.getSnapshots()
                                                ));
        }

        return machinesDescriptors;
    }

    @Path("/{machineId}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public void destroyMachine(@PathParam("machineId") String machineId) throws NotFoundException, ServerException, ForbiddenException {
        // todo how to check access rights?
//        checkCurrentUserPermissionsForMachine(machine.getCreatedBy());
        machineManager.destroy(machineId);
    }

    @Path("/{machineId}/snapshot")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")                                               // fixme move to body
    public void saveSnapshot(@PathParam("machineId") String machineId, @QueryParam("description") String description)
            throws NotFoundException, ServerException, ForbiddenException {
        final MachineImpl machine = machineManager.getMachine(machineId);
        if (!EnvironmentContext.getCurrent().getUser().getId().equals(machine.getOwner())) {
            throw new ForbiddenException("Operation is not permitted");
        }

        // todo should we send output to client?
        machineManager.save(machineId, EnvironmentContext.getCurrent().getUser().getId(), description);
    }

    @Path("/snapshot/{snapshotId}")
    @DELETE
    @RolesAllowed("user")
    public void removeSnapshot(@PathParam("snapshotId") String snapshotId) throws ForbiddenException, NotFoundException {
        final Snapshot snapshot = machineManager.getSnapshot(snapshotId);
        if (!EnvironmentContext.getCurrent().getUser().getId().equals(snapshot.getOwner())) {
            throw new ForbiddenException("Operation is not permitted");
        }

        machineManager.removeSnapshot(snapshotId);
    }

    @Path("/{machineId}/run")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public void executeCommandInMachine(@PathParam("machineId") String machineId, final NewCommand command)
            throws NotFoundException, ServerException, ForbiddenException {
        final MachineImpl machine = machineManager.getMachine(machineId);

        checkCurrentUserPermissionsForMachine(machine.getOwner());

        final LineConsumer lineConsumer;
        if (command.getOutputChannel() != null) {
            lineConsumer = new WebsocketLineConsumer(command.getOutputChannel());
        } else {
            lineConsumer = LineConsumer.DEV_NULL;
        }

        machineManager.exec(machineId, command, lineConsumer);
    }

    @Path("/{machineId}/processes")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public List<ProcessDescriptor> getProcesses(@PathParam("machineId") String machineId)
            throws NotFoundException, ServerException, ForbiddenException {
        final MachineImpl machine = machineManager.getMachine(machineId);

        checkCurrentUserPermissionsForMachine(machine.getOwner());

        final List<ProcessDescriptor> processesDescriptors = new LinkedList<>();
        for (ProcessImpl process : machine.getProcesses()) {
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
        final MachineImpl machine = machineManager.getMachine(machineId);
        checkCurrentUserPermissionsForMachine(machine.getOwner());

        for (ProcessImpl process : machine.getProcesses()) {
            if (process.getPid() == processId) {
                if (!process.isAlive()) {
                    throw new ForbiddenException("Process finished already");
                }
                // TODO should we do that in separate thread. E.g. if process is zombie request will stop by timeout
                process.kill();
                return;
            }
        }
        throw new NotFoundException(String.format("Process with id %s not found in machine %s.", processId, machineId));
    }

    @Path("/{machineId}/bind/{workspaceId}/{path:.*}")
    @POST
    @RolesAllowed("user")
    public void bindProject(@PathParam("machineId") String machineId,
                            @PathParam("workspaceId") String workspace,
                            @PathParam("path") String path) throws NotFoundException, ServerException, ForbiddenException {
        checkCurrentUserPermissionsForMachine(machineManager.getMachine(machineId).getOwner());
        // TODO check user's permissions for project

        machineManager.bindProject(machineId, DtoFactory.getInstance().createDto(ProjectBinding.class)
                                                        .withWorkspaceId(workspace)
                                                        .withPath(path));
    }

    @Path("/{machineId}/unbind/{workspaceId}/{path:.*}")
    @POST
    @RolesAllowed("user")
    public void unbindProject(@PathParam("machineId") String machineId,
                              @PathParam("workspaceId") String workspace,
                              @PathParam("path") String path) throws NotFoundException, ServerException, ForbiddenException {
        checkCurrentUserPermissionsForMachine(machineManager.getMachine(machineId).getOwner());

        machineManager.unbindProject(machineId, DtoFactory.getInstance().createDto(ProjectBinding.class)
                                                          .withWorkspaceId(workspace)
                                                          .withPath(path));
    }

    private void checkCurrentUserPermissionsForMachine(String machineOwner) throws ForbiddenException {
        final String userId = EnvironmentContext.getCurrent().getUser().getId();
        if (!userId.equals(machineOwner)) {
            throw new ForbiddenException("Operation is not permitted");
        }
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
                                           String machineOwner,
                                           Set<ProjectBinding> projectBindings,
//                                           String workspaceId,
//                                           String displayName,
                                           MachineState machineState
//                                           List<String> projects,
//                                           List<Snapshot> snapshots
                                          )
            throws ServerException {
//        List<SnapshotDescriptor> snapshotDescriptors = new LinkedList<>();
//        if (snapshots != null) {
//            for (Snapshot snapshot : snapshots) {
//                snapshotDescriptors.add(DtoFactory.getInstance().createDto(SnapshotDescriptor.class)
//                                                  .withId(snapshot.getId())
//                                                  .withDate(snapshot.getDate())
//                                                  .withDescription(snapshot.getDescription())
//                                                  .withLinks(null)); // TODO
//            }
//        }

        return DtoFactory.getInstance().createDto(MachineDescriptor.class)
                         .withId(id)
                         .withType(machineType)
                         .withOwner(machineOwner)
//                         .withState(machineState)
//                         .withWorkspaceId(workspaceId)
//                         .withDisplayName(displayName)
//                         .withSnapshots(snapshotDescriptors)
//                         .withProjects(projects)
//                        TODO
                         .withLinks(null);
    }

    private ProcessDescriptor toDescriptor(int processId, String commandLine) throws ServerException {
        return DtoFactory.getInstance().createDto(ProcessDescriptor.class)
                         .withPid(processId)
                         .withCommandLine(commandLine)
//                        TODO
                         .withLinks(null);
    }
}
