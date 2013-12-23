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


import com.codenvy.api.analytics.dto.*;
import com.codenvy.api.analytics.exception.MetricNotFoundException;
import com.codenvy.api.core.rest.Service;
import com.codenvy.api.core.rest.annotations.GenerateLink;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.HashMap;
import java.util.Map;


/**
 * Service is responsible for processing REST requests for analytics data.
 *
 * @author <a href="mailto:abazko@codenvy.com">Anatoliy Bazko</a>
 */
@Path("analytics")
public class AnalyticsService extends Service {

    @Inject
    private MetricHandler metricHandler;

    @GenerateLink(rel = "metric value")
    @GET
    @Path("metric/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getValue(@PathParam("name") String metricName,
                             @QueryParam("page") String page,
                             @QueryParam("per_page") String perPage,
                             @Context UriInfo uriInfo,
                             @Context SecurityContext securityContext) {
        try {
            // TODO DAHSB-243
            // validateRoles(metricName, securityContext);

            Map<String, String> metricContext = extractContext(uriInfo, page, perPage);
            MetricValueDTO value = metricHandler.getValue(metricName, metricContext, getServiceContext());
            return Response.status(Response.Status.OK).entity(value).build();
        } catch (MetricNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    @GenerateLink(rel = "metric info")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("metricinfo/{name}")
    public Response getInfo(@PathParam("name") String metricName, @Context UriInfo uriInfo) {
        try {
            MetricInfoDTO metricInfoDTO = metricHandler.getInfo(metricName, getServiceContext());
            return Response.status(Response.Status.OK).entity(metricInfoDTO).build();
        } catch (MetricNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    @GenerateLink(rel = "all metric info")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("metricinfo")
    public Response getAllInfo(@Context UriInfo uriInfo) {
        try {
            MetricInfoListDTO metricInfoListDTO = metricHandler.getAllInfo(getServiceContext());
            return Response.status(Response.Status.OK).entity(metricInfoListDTO).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    /** Extract the execution context from passed query parameters. */
    private Map<String, String> extractContext(UriInfo info, String page, String perPage) {
        MultivaluedMap<String, String> parameters = info.getQueryParameters();
        Map<String, String> context = new HashMap<>(parameters.size());

        for (String key : parameters.keySet()) {
            context.put(key.toUpperCase(), parameters.getFirst(key));
        }

        if (page != null) {
            context.put("PAGE", page);
            context.put("PER_PAGE", perPage);
        }

        return context;
    }

    /**
     * Checks if user is allowed to perform request.
     *
     * @throws IllegalStateException
     */
    private void validateRoles(String metricName, SecurityContext securityContext) {
        MetricRolesAllowedListDTO rolesAllowed = metricHandler.getRolesAllowed(metricName, getServiceContext());
        for (MetricRolesAllowedDTO dto : rolesAllowed.getRolesAllowed()) {
            if (securityContext.isUserInRole(dto.getRolesAllowed())) {
                return;
            }
        }

        throw new IllegalStateException("User is not allowed to get the value of " + metricName);
    }
}
