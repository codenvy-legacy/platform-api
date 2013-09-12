/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 * [2012] - [2013] Codenvy, S.A.
 * All Rights Reserved.
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
package com.codenvy.api.analytics.server;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * Metrics rest service.
 *
 * @author <a href="mailto:dkuleshov@exoplatform.com">Dmitry Kuleshov</a>
 */
@Path("{" + MetricService.PATH_PARAM_NAME + "}")
public interface MetricService {

    /**
     * Rest call URL path parameter
     */
    String PATH_PARAM_NAME   = "metricName";
    /**
     * Rest call URL path element used to determine request purposes.
     * Used for informational requests.
     */
    String INFO_PATH_ELEMENT = "info";
    /**
     * Rest call URL path element used to determine request purposes.
     * Used to indicate that request correspond to all entities.
     */
    String ALL_PATH_ELEMENT  = "all";

    /**
     * Provides metric value according to the internal implementation (e.g. executes script,
     * uses cached result, etc.).
     *
     * @param metricName
     *         requested metric name (to receive all metric list use "all" string)
     * @param uriInfo
     *         contextual parameter mostly used to get base URI and query parameters values
     * @return response entity containing metric value
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getValue(@PathParam(PATH_PARAM_NAME) String metricName, @Context UriInfo uriInfo);

    /**
     * Performs informational request to get name, description and rest request link for
     * metric by name or list of all metrics (if 'all' set instead of the name) in JSON format.
     *
     * @param metricName
     *         requested metric name (to receive all metric list use "all" string)
     * @param uriInfo
     *         contextual parameter mostly used to get base URI and query parameters values
     * @return response entity containing list of requested metric data JSON formatted
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path(INFO_PATH_ELEMENT)
    public Response getInfo(@PathParam(PATH_PARAM_NAME) String metricName, @Context UriInfo uriInfo);
}
