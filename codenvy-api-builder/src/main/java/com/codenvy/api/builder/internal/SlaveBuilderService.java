/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2013] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.api.builder.internal;

import com.codenvy.api.builder.BuildStatus;
import com.codenvy.api.builder.BuilderException;
import com.codenvy.api.builder.dto.BuildTaskDescriptor;
import com.codenvy.api.builder.internal.dto.BuildRequest;
import com.codenvy.api.builder.internal.dto.BuilderDescriptor;
import com.codenvy.api.builder.internal.dto.BuilderList;
import com.codenvy.api.builder.internal.dto.BuilderState;
import com.codenvy.api.builder.internal.dto.DependencyRequest;
import com.codenvy.api.builder.internal.dto.ServerState;
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.rest.Service;
import com.codenvy.api.core.rest.annotations.Description;
import com.codenvy.api.core.rest.annotations.GenerateLink;
import com.codenvy.api.core.rest.annotations.Required;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.core.util.ContentTypeGuesser;
import com.codenvy.api.core.util.SystemInfo;
import com.codenvy.dto.server.DtoFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * RESTful interface for Builder.
 *
 * @author andrew00x
 */
@Path("internal/builder")
public final class SlaveBuilderService extends Service {
    @Inject
    private BuilderRegistry builders;

    /** Get list of available Builders which can be accessible over this SlaveBuilderService. */
    @GenerateLink(rel = Constants.LINK_REL_AVAILABLE_BUILDERS)
    @GET
    @Path("available")
    @Produces(MediaType.APPLICATION_JSON)
    public BuilderList availableBuilders() {
        final Set<Builder> all = builders.getAll();
        final List<BuilderDescriptor> list = new ArrayList<>(all.size());
        for (Builder builder : all) {
            list.add(DtoFactory.getInstance().createDto(BuilderDescriptor.class)
                               .withName(builder.getName())
                               .withDescription(builder.getDescription()));
        }
        return DtoFactory.getInstance().createDto(BuilderList.class).withBuilders(list);
    }

    @GenerateLink(rel = Constants.LINK_REL_BUILDER_STATE)
    @GET
    @Path("state")
    @Produces(MediaType.APPLICATION_JSON)
    public BuilderState getBuilderState(@Required
                                        @Description("Name of the builder")
                                        @QueryParam("builder") String builder) throws Exception {
        final Builder myBuilder = getBuilder(builder);
        return DtoFactory.getInstance().createDto(BuilderState.class)
                         .withName(myBuilder.getName())
                         .withNumberOfWorkers(myBuilder.getNumberOfWorkers())
                         .withNumberOfActiveWorkers(myBuilder.getNumberOfActiveWorkers())
                         .withInternalQueueSize(myBuilder.getInternalQueueSize())
                         .withMaxInternalQueueSize(myBuilder.getMaxInternalQueueSize())
                         .withServerState(getServerState());
    }

    @GenerateLink(rel = Constants.LINK_REL_SERVER_STATE)
    @GET
    @Path("server-state")
    @Produces(MediaType.APPLICATION_JSON)
    public ServerState getServerState() {
        return DtoFactory.getInstance().createDto(ServerState.class)
                         .withCpuPercentUsage(SystemInfo.cpu())
                         .withTotalMemory(SystemInfo.totalMemory())
                         .withFreeMemory(SystemInfo.freeMemory());
    }

    @GenerateLink(rel = Constants.LINK_REL_BUILD)
    @POST
    @Path("build")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public BuildTaskDescriptor build(@Description("Parameters for build task in JSON format") BuildRequest request) throws Exception {
        final BuildTask task = getBuilder(request.getBuilder()).perform(request);
        return getDescriptor(task, getServiceContext().getServiceUriBuilder());
    }

    @GenerateLink(rel = Constants.LINK_REL_DEPENDENCIES_ANALYSIS)
    @POST
    @Path("dependencies")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public BuildTaskDescriptor dependencies(@Description("Parameters for analyze dependencies in JSON format") DependencyRequest request)
            throws Exception {
        final BuildTask task = getBuilder(request.getBuilder()).perform(request);
        return getDescriptor(task, getServiceContext().getServiceUriBuilder());
    }

    @GET
    @Path("status/{builder}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public BuildTaskDescriptor getStatus(@PathParam("builder") String builder, @PathParam("id") Long id) throws Exception {
        final BuildTask task = getBuilder(builder).getBuildTask(id);
        return getDescriptor(task, getServiceContext().getServiceUriBuilder());
    }

    @GET
    @Path("logs/{builder}/{id}")
    public Response getLogs(@PathParam("builder") String builder, @PathParam("id") Long id) throws Exception {
        final BuildLogger logger = getBuilder(builder).getBuildTask(id).getBuildLogger();
        return Response.ok(logger.getReader(), logger.getContentType()).build();
    }

    @POST
    @Path("cancel/{builder}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public BuildTaskDescriptor cancel(@PathParam("builder") String builder, @PathParam("id") Long id) throws Exception {
        final BuildTask task = getBuilder(builder).getBuildTask(id);
        task.cancel();
        return getDescriptor(task, getServiceContext().getServiceUriBuilder());
    }

    @GET
    @Path("browse/{builder}/{id}")
    public Response browse(@PathParam("builder") String builder,
                           @PathParam("id") Long id,
                           @Required @QueryParam("path") String path) throws Exception {
        final Builder myBuilder = getBuilder(builder);
        final BuildTask task = myBuilder.getBuildTask(id);
        final java.io.File workDir = task.getConfiguration().getWorkDir();
        final java.io.File target = new java.io.File(workDir, path);
        if (target.isDirectory()) {
            final StringWriter buff = new StringWriter();
            buff.write("<div class='file-browser'>");
            final UriBuilder serviceUriBuilder = getServiceContext().getServiceUriBuilder();
            final java.io.File[] list = target.listFiles();
            if (list != null) {
                for (java.io.File file : list) {
                    final String name = file.getName();
                    buff.write("<div class='file-browser-item'>");
                    buff.write("<span class='file-browser-name'>");
                    buff.write(name);
                    buff.write("</span>");
                    buff.write("&nbsp;");
                    if (file.isFile()) {
                        buff.write("<span class='file-browser-file-open'>");
                        buff.write(String.format("<a target='_blank' href='%s'>open</a>",
                                                 serviceUriBuilder.clone().path(getClass(), "view")
                                                                  .replaceQueryParam("path", path + '/' + name)
                                                                  .build(task.getBuilder(), task.getId()).toString()
                                                ));
                        buff.write("</span>");
                        buff.write("&nbsp;");
                        buff.write("<span class='file-browser-file-download'>");
                        buff.write(String.format("<a href='%s'>download</a>",
                                                 serviceUriBuilder.clone().path(getClass(), "download")
                                                                  .replaceQueryParam("path", path + '/' + name)
                                                                  .build(task.getBuilder(), task.getId()).toString()
                                                ));
                        buff.write("</span>");
                    } else if (file.isDirectory()) {
                        buff.write("<span class='file-browser-directory-open'>");
                        buff.write(String.format("<a href='%s'>open</a>",
                                                 serviceUriBuilder.clone().path(getClass(), "browse")
                                                                  .replaceQueryParam("path", path + '/' + name)
                                                                  .build(task.getBuilder(), task.getId()).toString()
                                                ));
                        buff.write("</span>");
                        buff.write("&nbsp;");
                    }
                    buff.write("</div>");
                }
            }
            buff.write("</div>");
            return Response.status(200).entity(buff.toString()).type(MediaType.TEXT_HTML).build();
        }
        throw new NotFoundException(String.format("%s does not exist or is not a folder", path));
    }

    @GET
    @Path("download/{builder}/{id}")
    public Response download(@PathParam("builder") String builder,
                             @PathParam("id") Long id,
                             @Required @QueryParam("path") String path) throws Exception {
        final java.io.File workDir = getBuilder(builder).getBuildTask(id).getConfiguration().getWorkDir();
        final java.io.File target = new java.io.File(workDir, path);
        if (target.isFile()) {
            return Response.status(200)
                           .header("Content-Disposition", String.format("attachment; filename=\"%s\"", target.getName()))
                           .type(ContentTypeGuesser.guessContentType(target))
                           .entity(target)
                           .build();
        }
        throw new NotFoundException(String.format("%s does not exist or is not a file", path));
    }

    @GET
    @Path("view/{builder}/{id}")
    public Response view(@PathParam("builder") String builder,
                         @PathParam("id") Long id,
                         @Required @QueryParam("path") String path) throws Exception {
        final java.io.File workDir = getBuilder(builder).getBuildTask(id).getConfiguration().getWorkDir();
        final java.io.File target = new java.io.File(workDir, path);
        if (target.isFile()) {
            return Response.status(200).type(ContentTypeGuesser.guessContentType(target)).entity(target).build();
        }
        throw new NotFoundException(String.format("%s does not exist or is not a file", path));
    }

    private Builder getBuilder(String name) throws NotFoundException {
        final Builder myBuilder = builders.get(name);
        if (myBuilder == null) {
            throw new NotFoundException(String.format("Unknown builder %s", name));
        }
        return myBuilder;
    }

    private BuildTaskDescriptor getDescriptor(BuildTask task, UriBuilder uriBuilder) throws BuilderException {
        final String builder = task.getBuilder();
        final Long taskId = task.getId();
        final BuildResult result = task.getResult();
        final java.nio.file.Path workDirPath = task.getConfiguration().getWorkDir().toPath();
        final BuildStatus status = task.isDone()
                                   ? (task.isCancelled() ? BuildStatus.CANCELLED
                                                         : (result.isSuccessful() ? BuildStatus.SUCCESSFUL : BuildStatus.FAILED))
                                   : (task.isStarted() ? BuildStatus.IN_PROGRESS : BuildStatus.IN_QUEUE);
        final List<Link> links = new LinkedList<>();
        links.add(DtoFactory.getInstance().createDto(Link.class)
                            .withRel(Constants.LINK_REL_GET_STATUS)
                            .withHref(uriBuilder.clone().path(SlaveBuilderService.class, "getStatus").build(builder, taskId).toString())
                            .withMethod("GET")
                            .withProduces(MediaType.APPLICATION_JSON));

        if (status == BuildStatus.IN_QUEUE || status == BuildStatus.IN_PROGRESS) {
            links.add(DtoFactory.getInstance().createDto(Link.class)
                                .withRel(Constants.LINK_REL_CANCEL)
                                .withHref(uriBuilder.clone().path(SlaveBuilderService.class, "cancel").build(builder, taskId).toString())
                                .withMethod("POST")
                                .withProduces(MediaType.APPLICATION_JSON));
        }

        if (status != BuildStatus.IN_QUEUE) {
            links.add(DtoFactory.getInstance().createDto(Link.class)
                                .withRel(Constants.LINK_REL_VIEW_LOG)
                                .withHref(uriBuilder.clone().path(SlaveBuilderService.class, "getLogs").build(builder, taskId).toString())
                                .withMethod("GET")
                                .withProduces(task.getBuildLogger().getContentType()));
            links.add(DtoFactory.getInstance().createDto(Link.class)
                                .withRel(Constants.LINK_REL_BROWSE)
                                .withHref(uriBuilder.clone().path(SlaveBuilderService.class, "browse").queryParam("path", "/")
                                                    .build(builder, taskId).toString())
                                .withMethod("GET")
                                .withProduces(MediaType.TEXT_HTML));
        }

        if (status == BuildStatus.SUCCESSFUL) {
            for (java.io.File ru : result.getResults()) {
                links.add(DtoFactory.getInstance().createDto(Link.class)
                                    .withRel(Constants.LINK_REL_DOWNLOAD_RESULT)
                                    .withHref(uriBuilder.clone().path(SlaveBuilderService.class, "download")
                                                        .queryParam("path", workDirPath.relativize(ru.toPath()))
                                                        .build(builder, taskId).toString())
                                    .withMethod("GET")
                                    .withProduces(ContentTypeGuesser.guessContentType(ru)));
            }
        }

        if ((status == BuildStatus.SUCCESSFUL || status == BuildStatus.FAILED) && result.hasBuildReport()) {
            final java.io.File br = result.getBuildReport();
            if (br.isDirectory()) {
                links.add(DtoFactory.getInstance().createDto(Link.class)
                                    .withRel(Constants.LINK_REL_VIEW_REPORT)
                                    .withHref(uriBuilder.clone().path(SlaveBuilderService.class, "browse")
                                                        .queryParam("path", workDirPath.relativize(br.toPath()))
                                                        .build(builder, taskId).toString())
                                    .withMethod("GET")
                                    .withProduces(MediaType.TEXT_HTML));
            } else {
                links.add(DtoFactory.getInstance().createDto(Link.class)
                                    .withRel(Constants.LINK_REL_VIEW_REPORT)
                                    .withHref(uriBuilder.clone().path(SlaveBuilderService.class, "view")
                                                        .queryParam("path", workDirPath.relativize(br.toPath()))
                                                        .build(builder, taskId).toString())
                                    .withMethod("GET")
                                    .withProduces(ContentTypeGuesser.guessContentType(br)));
            }
        }

        return DtoFactory.getInstance().createDto(BuildTaskDescriptor.class)
                         .withTaskId(taskId)
                         .withStatus(status)
                         .withLinks(links)
                         .withStartTime(task.getStartTime())
                         .withEndTime(task.getEndTime());
    }
}
