/*******************************************************************************
 * Copyright (c) 2012-2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.api.analytics;


import com.codenvy.api.analytics.logger.EventLogger;
import com.codenvy.api.analytics.shared.dto.*;
import com.codenvy.api.core.rest.Service;
import com.codenvy.api.core.rest.annotations.GenerateLink;
import com.codenvy.dto.server.JsonArrayImpl;
import com.codenvy.dto.server.JsonStringMapImpl;
import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Service is responsible for processing REST requests for analytics data.
 *
 * @author Anatoliy Bazko
 */
@Path("analytics")
@Singleton
public class AnalyticsService extends Service {

    private static final Logger LOG = LoggerFactory.getLogger(AnalyticsService.class);

    private final MetricHandler metricHandler;
    private final EventLogger   eventLogger;

    @Inject
    public AnalyticsService(MetricHandler metricHandler, EventLogger eventLogger) {
        this.metricHandler = metricHandler;
        this.eventLogger = eventLogger;
    }

    @GenerateLink(rel = "metric value")
    @GET
    @Path("metric/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    public Response getValueByQueryParams(@PathParam("name") String metricName,
                                          @QueryParam("page") String page,
                                          @QueryParam("per_page") String perPage,
                                          @Context UriInfo uriInfo) {
        try {
            Map<String, String> metricContext = extractContext(uriInfo,
                                                               page,
                                                               perPage);
            MetricValueDTO value = metricHandler.getValueByQueryParams(metricName, metricContext, uriInfo);
            return Response.status(Response.Status.OK).entity(value).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("Unexpected error occurred. Can't get value for metric " + metricName).build();
        }
    }

    @GenerateLink(rel = "metric value")
    @POST
    @Path("metric/{name}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    public Response getValueByJson(Map<String, String> parameters,
                                   @PathParam("name") String metricName,
                                   @QueryParam("page") String page,
                                   @QueryParam("per_page") String perPage,
                                   @Context UriInfo uriInfo) {
        try {
            Map<String, String> metricContext = extractContext(uriInfo,
                                                               page,
                                                               perPage);
            MetricValueDTO value = metricHandler.getValueByJson(metricName,
                                                                new JsonStringMapImpl<>(parameters),
                                                                metricContext,
                                                                uriInfo);
            return Response.status(Response.Status.OK).entity(value).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("Unexpected error occurred. Can't get value for metric " + metricName).build();
        }
    }

    @GenerateLink(rel = "metric value")
    @GET
    @Path("public-metric/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPublicValue(@PathParam("name") String metricName,
                                   @QueryParam("page") String page,
                                   @QueryParam("per_page") String perPage,
                                   @Context UriInfo uriInfo) {
        try {
            Map<String, String> metricContext = extractContext(uriInfo,
                                                               page,
                                                               perPage);
            MetricValueDTO value = metricHandler.getPublicValue(metricName, metricContext, uriInfo);
            return Response.status(Response.Status.OK).entity(value).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("Unexpected error occurred. Can't get value for metric " + metricName).build();
        }
    }

    @GenerateLink(rel = "list of metric values")
    @POST
    @Path("/metric/user")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    public Response getUserValues(List<String> metricNames, @Context UriInfo uriInfo) {
        try {
            Map<String, String> metricContext = extractContext(uriInfo);
            MetricValueListDTO list = metricHandler.getUserValues(new JsonArrayImpl<>(metricNames),
                                                                  metricContext,
                                                                  uriInfo);
            return Response.status(Response.Status.OK).entity(list).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
                    "Unexpected error occurred. Can't get values of metrics").build();
        }
    }

    @GenerateLink(rel = "metric info")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("metricinfo/{name}")
    @RolesAllowed({"user", "system/admin", "system/manager"})
    public Response getInfo(@PathParam("name") String metricName, @Context UriInfo uriInfo) {
        try {
            MetricInfoDTO metricInfoDTO = metricHandler.getInfo(metricName, uriInfo);
            return Response.status(Response.Status.OK).entity(metricInfoDTO).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("Unexpected error occurred. Can't get info for metric " + metricName).build();
        }
    }

    @GenerateLink(rel = "all metric info")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("metricinfo")
    @RolesAllowed({"user", "system/admin", "system/manager"})
    public Response getAllInfo(@Context UriInfo uriInfo) {
        try {
            MetricInfoListDTO metricInfoListDTO = metricHandler.getAllInfo(uriInfo);
            return Response.status(Response.Status.OK).entity(metricInfoListDTO).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("Unexpected error occurred. Can't get metric info").build();
        }
    }

    @GenerateLink(rel = "log analytics event")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("log/{event}")
    @RolesAllowed({"user", "system/admin", "system/manager"})
    public Response logEvent(@PathParam("event") String event, EventParameters parameters) {
        try {
            eventLogger.log(event, parameters.getParams());
            return Response.status(Response.Status.ACCEPTED).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Unexpected error occurred. Can't log event " + event).build();
        }
    }

    @GenerateLink(rel = "log use dashboard event")
    @POST
    @Path("log/dashboard-usage/{action}")
    @RolesAllowed({"user", "system/admin", "system/manager"})
    public Response logUserDashboardEvent(@PathParam("action") String action) {
        try {
            Map<String, String> parameters = new HashMap<>(1);
            parameters.put(EventLogger.ACTION_PARAM, action);

            eventLogger.log(EventLogger.DASHBOARD_USAGE, parameters);
            return Response.status(Response.Status.ACCEPTED).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("Unexpected error occurred. Can't log dashboard event for action " + action).build();
        }
    }

    private Map<String, String> extractContext(UriInfo info,
                                               String page,
                                               String perPage) {

        MultivaluedMap<String, String> parameters = info.getQueryParameters();
        Map<String, String> context = new HashMap<>(parameters.size());

        for (String key : parameters.keySet()) {
            context.put(key.toUpperCase(), parameters.getFirst(key));
        }

        if (page != null && perPage != null) {
            context.put("PAGE", page);
            context.put("PER_PAGE", perPage);
        }

        return context;
    }

    private Map<String, String> extractContext(UriInfo info) {
        return extractContext(info, null, null);
    }
}
