/*
 *
 * CODENVY CONFIDENTIAL
 * ________________
 *
 * [2012] - [2013] Codenvy, S.A.
 * All Rights Reserved.
 * NOTICE: All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any. The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */

package com.codenvy.api.analytics;


import com.codenvy.api.analytics.dto.MetricInfoDTO;
import com.codenvy.api.analytics.dto.MetricInfoListDTO;
import com.codenvy.api.analytics.dto.MetricValueDTO;
import com.codenvy.api.analytics.dto.MetricValueListDTO;
import com.codenvy.api.analytics.logger.EventLogger;
import com.codenvy.api.core.rest.Service;
import com.codenvy.api.core.rest.annotations.GenerateLink;
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
 * @author <a href="mailto:abazko@codenvy.com">Anatoliy Bazko</a>
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
    public Response getValue(@PathParam("name") String metricName,
                             @QueryParam("page") String page,
                             @QueryParam("per_page") String perPage,
                             @Context UriInfo uriInfo) {
        try {
            Map<String, String> metricContext = extractContext(uriInfo,
                                                               page,
                                                               perPage);
            MetricValueDTO value = metricHandler.getValue(metricName, metricContext, uriInfo);
            return Response.status(Response.Status.OK).entity(value).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
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
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
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
            MetricValueListDTO list = metricHandler.getUserValues(metricNames, metricContext, uriInfo);
            return Response.status(Response.Status.OK).entity(list).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
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
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
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
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    @GenerateLink(rel = "log analytics event")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("log/{event}")
    @RolesAllowed({"user"})
    public Response logEvent(@PathParam("event") String event, Map<String, String> parameters) {
        try {
            eventLogger.log(event, parameters);
            return Response.status(Response.Status.ACCEPTED).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
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
