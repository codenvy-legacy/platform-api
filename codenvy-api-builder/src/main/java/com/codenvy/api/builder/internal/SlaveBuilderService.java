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

import com.codenvy.api.builder.internal.dto.BuildRequest;
import com.codenvy.api.builder.internal.dto.BuildTaskDescriptor;
import com.codenvy.api.builder.internal.dto.BuilderDescriptor;
import com.codenvy.api.builder.internal.dto.BuilderList;
import com.codenvy.api.builder.internal.dto.DependencyRequest;
import com.codenvy.api.builder.internal.dto.InstanceState;
import com.codenvy.api.builder.internal.dto.SlaveBuilderState;
import com.codenvy.api.core.rest.FileAdapter;
import com.codenvy.api.core.rest.Service;
import com.codenvy.api.core.rest.annotations.Description;
import com.codenvy.api.core.rest.annotations.GenerateLink;
import com.codenvy.api.core.rest.annotations.Required;
import com.codenvy.api.core.util.SystemInfo;
import com.codenvy.api.vfs.server.exceptions.InvalidArgumentException;
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
import java.util.List;
import java.util.Set;

/**
 * RESTful interface for Builder.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
@Path("internal/builder")
public final class SlaveBuilderService extends Service {

    @Inject
    private BuilderRegistry builders;

    /** Get list of available Builders which can be accessible over this InternalBuilderService. */
    @GenerateLink(rel = Constants.LINK_REL_AVAILABLE_BUILDERS)
    @GET
    @Path("available")
    @Produces(MediaType.APPLICATION_JSON)
    public BuilderList availableBuilders() {
        final Set<Builder> all = builders.getAll();
        final List<BuilderDescriptor> list = new ArrayList<>(all.size());
        for (Builder builder : all) {
            list.add(builder.getDescriptor());
        }
        final BuilderList result = DtoFactory.getInstance().createDto(BuilderList.class);
        result.setBuilders(list);
        return result;
    }

    @GenerateLink(rel = Constants.LINK_REL_BUILDER_STATE)
    @GET
    @Path("state")
    @Produces(MediaType.APPLICATION_JSON)
    public SlaveBuilderState getBuilderState(@Required
                                             @Description("Name of the builder")
                                             @QueryParam("builder") String builder) throws Exception {
        final Builder myBuilder = getBuilder(builder);
        final InstanceState instanceState = DtoFactory.getInstance().createDto(InstanceState.class);
        instanceState.setCpuPercentUsage(SystemInfo.cpu());
        instanceState.setTotalMemory(SystemInfo.totalMemory());
        instanceState.setFreeMemory(SystemInfo.freeMemory());
        final SlaveBuilderState builderState = DtoFactory.getInstance().createDto(SlaveBuilderState.class);
        builderState.setName(myBuilder.getName());
        builderState.setNumberOfWorkers(myBuilder.getNumberOfWorkers());
        builderState.setNumberOfActiveWorkers(myBuilder.getNumberOfActiveWorkers());
        builderState.setInternalQueueSize(myBuilder.getInternalQueueSize());
        builderState.setMaxInternalQueueSize(myBuilder.getMaxInternalQueueSize());
        builderState.setInstanceState(instanceState);
        return builderState;
    }

    @GenerateLink(rel = Constants.LINK_REL_BUILD)
    @POST
    @Path("build")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public BuildTaskDescriptor build(@Description("Parameters for build task in JSON format") BuildRequest request) throws Exception {
        return getBuilder(request.getBuilder()).perform(request).getDescriptor(getServiceContext());
    }

    @GenerateLink(rel = Constants.LINK_REL_DEPENDENCIES_ANALYSIS)
    @POST
    @Path("dependencies")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public BuildTaskDescriptor dependencies(@Description("Parameters for analyze dependencies in JSON format") DependencyRequest request)
            throws Exception {
        return getBuilder(request.getBuilder()).perform(request).getDescriptor(getServiceContext());
    }

    @GET
    @Path("status/{builder}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public BuildTaskDescriptor getStatus(@PathParam("builder") String builder, @PathParam("id") Long id) throws Exception {
        return getBuilder(builder).getBuildTask(id).getDescriptor(getServiceContext());
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
        return task.getDescriptor(getServiceContext());
    }

    @GET
    @Path("browse/{builder}/{id}")
    public Response browse(@PathParam("builder") String builder,
                           @PathParam("id") Long id,
                           @Required @QueryParam("path") String path) throws Exception {
        final Builder myBuilder = getBuilder(builder);
        final BuildTask task = myBuilder.getBuildTask(id);
        final FileAdapter srcDir = task.getSources().getDirectory();
        final FileAdapter target = srcDir.getChild(path);
        if (target.isDirectory()) {
            final StringWriter buff = new StringWriter();
            buff.write("<div class='file-browser'>");
            final UriBuilder serviceUriBuilder = getServiceContext().getServiceUriBuilder();
            for (java.io.File file : target.getIoFile().listFiles()) {
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
                                                              .build(task.getBuilder(), task.getId()).toString()));
                    buff.write("</span>");
                    buff.write("&nbsp;");
                    buff.write("<span class='file-browser-file-download'>");
                    buff.write(String.format("<a href='%s'>download</a>",
                                             serviceUriBuilder.clone().path(getClass(), "download")
                                                              .replaceQueryParam("path", path + '/' + name)
                                                              .build(task.getBuilder(), task.getId()).toString()));
                    buff.write("</span>");
                } else if (file.isDirectory()) {
                    buff.write("<span class='file-browser-directory-open'>");
                    buff.write(String.format("<a href='%s'>open</a>",
                                             serviceUriBuilder.clone().path(getClass(), "browse")
                                                              .replaceQueryParam("path", path + '/' + name)
                                                              .build(task.getBuilder(), task.getId()).toString()));
                    buff.write("</span>");
                    buff.write("&nbsp;");
                }
                buff.write("</div>");
            }
            buff.write("</div>");
            return Response.status(200).entity(buff.toString()).type(MediaType.TEXT_HTML).build();
        }
        throw new InvalidArgumentException(String.format("%s does not exist or is not a folder", path));
    }

    @GET
    @Path("download/{builder}/{id}")
    public Response download(@PathParam("builder") String builder,
                             @PathParam("id") Long id,
                             @Required @QueryParam("path") String path) throws Exception {
        final FileAdapter srcDir = getBuilder(builder).getBuildTask(id).getSources().getDirectory();
        final FileAdapter target = srcDir.getChild(path);
        if (target.isFile()) {
            return Response.status(200)
                           .header("Content-Disposition", String.format("attachment; filename=\"%s\"", target.getName()))
                           .type(target.getContentType())
                           .entity(target.getIoFile())
                           .build();
        }
        throw new InvalidArgumentException(String.format("%s does not exist or is not a file", path));
    }

    @GET
    @Path("view/{builder}/{id}")
    public Response view(@PathParam("builder") String builder,
                         @PathParam("id") Long id,
                         @Required @QueryParam("path") String path) throws Exception {
        final FileAdapter srcDir = getBuilder(builder).getBuildTask(id).getSources().getDirectory();
        final FileAdapter target = srcDir.getChild(path);
        if (target.isFile()) {
            return Response.status(200).type(target.getContentType()).entity(target.getIoFile()).build();
        }
        throw new InvalidArgumentException(String.format("%s does not exist or is not a file", path));
    }

    private Builder getBuilder(String name) throws NoSuchBuilderException {
        final Builder myBuilder = builders.get(name);
        if (myBuilder == null) {
            throw new NoSuchBuilderException(name);
        }
        return myBuilder;
    }
}
