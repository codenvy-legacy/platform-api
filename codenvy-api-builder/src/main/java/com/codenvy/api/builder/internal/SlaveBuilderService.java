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
import com.codenvy.api.core.rest.ServiceContext;
import com.codenvy.api.core.rest.annotations.Description;
import com.codenvy.api.core.rest.annotations.GenerateLink;
import com.codenvy.api.core.rest.annotations.Required;
import com.codenvy.api.core.rest.dto.JsonDto;
import com.codenvy.api.core.util.SystemInfo;

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

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
@Path("api/builder")
@Description("Builder API")
public final class SlaveBuilderService extends Service {

    @Inject
    private BuilderRegistry builders;

    /** Get list of available Builders which can be accessible over this InternalBuilderService. */
    @GenerateLink(rel = Constants.LINK_REL_AVAILABLE_BUILDERS)
    @GET
    @Path("available")
    @Produces(MediaType.APPLICATION_JSON)
    public Response availableBuilders() {
        final Set<Builder> all = builders.getAll();
        final List<BuilderDescriptor> result = new ArrayList<>(all.size());
        for (Builder builder : all) {
            result.add(builder.getDescriptor());
        }
        return Response.status(Response.Status.OK).entity(JsonDto.toJson(new BuilderList(result))).build();
    }

    @GenerateLink(rel = Constants.LINK_REL_BUILDER_STATE)
    @GET
    @Path("state")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBuilderState(@Required
                                    @Description("Name of the builder")
                                    @QueryParam("builder") String builder) throws Exception {
        final Builder myBuilder = getBuilder(builder);
        final InstanceState instanceState = new InstanceState(SystemInfo.cpu(), SystemInfo.totalMemory(), SystemInfo.freeMemory());
        final SlaveBuilderState builderState = new SlaveBuilderState(myBuilder.getName(),
                                                                           myBuilder.getNumberOfWorkers(),
                                                                           myBuilder.getNumberOfActiveWorkers(),
                                                                           myBuilder.getInternalQueueSize(),
                                                                           myBuilder.getMaxInternalQueueSize(),
                                                                           instanceState);
        return Response.status(Response.Status.OK).entity(JsonDto.toJson(builderState)).build();
    }

    @GenerateLink(rel = Constants.LINK_REL_BUILD)
    @POST
    @Path("build")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response build(@Description("Parameters for build task in JSON format") String str) throws Exception {
        final BuildRequest request = JsonDto.fromJson(str).cast();
        final Builder myBuilder = getBuilder(request.getBuilder());
        final ServiceContext serviceContext = getServiceContext();
        final BuildTaskDescriptor descriptor = myBuilder.perform(request, getServiceContext()).getDescriptor(serviceContext);
        return Response.status(Response.Status.OK).entity(JsonDto.toJson(descriptor)).build();
    }

    @GenerateLink(rel = Constants.LINK_REL_DEPENDENCIES_ANALYSIS)
    @POST
    @Path("dependencies")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response dependencies(@Description("Parameters for analyze dependencies in JSON format") String str) throws Exception {
        final DependencyRequest request = JsonDto.fromJson(str).cast();
        final Builder myBuilder = getBuilder(request.getBuilder());
        final ServiceContext serviceContext = getServiceContext();
        final BuildTaskDescriptor descriptor = myBuilder.perform(request, serviceContext).getDescriptor(serviceContext);
        return Response.status(Response.Status.OK).entity(JsonDto.toJson(descriptor)).build();
    }

    @GET
    @Path("status/{builder}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBuildDescriptor(@PathParam("builder") String builder, @PathParam("id") Long id) throws Exception {
        final Builder myBuilder = getBuilder(builder);
        final BuildTaskDescriptor descriptor = myBuilder.getBuildTask(id).getDescriptor(getServiceContext());
        return Response.status(Response.Status.OK).entity(JsonDto.toJson(descriptor)).build();
    }

    @GET
    @Path("log/{builder}/{id}")
    public Response getBuildLog(@PathParam("builder") String builder, @PathParam("id") Long id) throws Exception {
        final Builder myBuilder = getBuilder(builder);
        final BuildTask task = myBuilder.getBuildTask(id);
        final BuildLogger logger = task.getBuildLogger();
        return Response.ok(logger.getReader(), logger.getContentType()).build();
    }

    @POST
    @Path("cancel/{builder}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response cancel(@PathParam("builder") String builder, @PathParam("id") Long id) throws Exception {
        final Builder myBuilder = getBuilder(builder);
        final BuildTask task = myBuilder.getBuildTask(id);
        task.cancel();
        return Response.status(Response.Status.OK).entity(JsonDto.toJson(task.getDescriptor(getServiceContext()))).build();
    }

    @GET
    @Path("browse/{builder}/{id}")
    public Response browse(@PathParam("builder") String builder,
                           @PathParam("id") Long id,
                           @Required @QueryParam("path") String path) throws Exception {
        final Builder myBuilder = getBuilder(builder);
        final BuildTask task = myBuilder.getBuildTask(id);
        final FileAdapter srcDir = task.getSources().getDirectory();
        final FileAdapter target;
        try {
            target = srcDir.getChild(path);
        } catch (IllegalArgumentException e) {
            return Response.status(200).type(MediaType.TEXT_HTML)
                           .entity(String.format("<div><span class='file-access-error'>%s</span></div>", e.getMessage())).build();
        }
        if (target.isDirectory()) {
            final java.io.File[] files = target.getIoFile().listFiles();
            if (files == null) {
                return Response.status(200).entity(String.format("<div><span class='file-access-error'>Not found %s</span></div>", path))
                               .type(MediaType.TEXT_HTML).build();
            }
            final StringWriter buff = new StringWriter();
            buff.write("<div class='file-browser'>");
            final UriBuilder serviceUriBuilder = getServiceContext().getServicePathBuilder();
            for (java.io.File file : files) {
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
        } else {
            return Response.status(200).type(MediaType.TEXT_HTML)
                           .entity(String.format("<div><span class='file-access-error'>Not a folder %s</span></div>", path)).build();
        }
    }

    @GET
    @Path("download/{builder}/{id}")
    public Response download(@PathParam("builder") String builder,
                             @PathParam("id") Long id,
                             @Required @QueryParam("path") String path) throws Exception {
        return getFile(builder, id, path, true);
    }

    @GET
    @Path("view/{builder}/{id}")
    public Response view(@PathParam("builder") String builder,
                         @PathParam("id") Long id,
                         @Required @QueryParam("path") String path) throws Exception {
        return getFile(builder, id, path, false);
    }

    private Response getFile(String builder, Long id, String path, boolean download) throws Exception {
        final Builder myBuilder = getBuilder(builder);
        final BuildTask task = myBuilder.getBuildTask(id);
        final FileAdapter srcDir = task.getSources().getDirectory();
        final FileAdapter target;
        try {
            target = srcDir.getChild(path);
        } catch (IllegalArgumentException e) {
            return Response.status(200).type(MediaType.TEXT_HTML)
                           .entity(String.format("<div><span class='file-access-error'>%s</span></div>", e.getMessage())).build();
        }
        if (!target.exists()) {
            return Response.status(200).type(MediaType.TEXT_HTML)
                           .entity(String.format("<div><span class='file-access-error'>Not found %s</span></div>", path)).build();
        }
        if (!target.isFile()) {
            return Response.status(200).type(MediaType.TEXT_HTML)
                           .entity(String.format("<div><span class='file-access-error'>Not a file %s</span></div>", path)).build();
        }
        if (download) {
            return Response.status(200)
                           .header("Content-Disposition", String.format("attachment; filename=\"%s\"", target.getName()))
                           .type(target.getContentType())
                           .entity(target.getIoFile())
                           .build();
        }
        return Response.status(200).type(target.getContentType()).entity(target.getIoFile()).build();
    }

    private Builder getBuilder(String name) throws NoSuchBuilderException {
        final Builder myBuilder = builders.get(name);
        if (myBuilder == null) {
            throw new NoSuchBuilderException(name);
        }
        return myBuilder;
    }
}
