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
package com.codenvy.api.analytics.dto.util;

import com.codenvy.analytics.metrics.Metric;
import com.codenvy.analytics.metrics.MetricType;
import com.codenvy.api.analytics.AnalyticsService;
import com.codenvy.api.analytics.dto.Constants;
import com.codenvy.api.analytics.dto.MetricInfoDTO;
import com.codenvy.api.core.rest.ServiceContext;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.dto.server.DtoFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to create {@link com.codenvy.api.analytics.dto.MetricInfoDTO} instances and to update its values
 *
 * @author <a href="mailto:dkuleshov@codenvy.com">Dmitry Kuleshov</a>
 */
public class MetricDTOFactory {

    private MetricDTOFactory() {
    }

    public static MetricInfoDTO createMetricDTO(Metric metric, MetricType metricType, ServiceContext restfulRequestContext) {
        return createMetricDTO(metric, metricType.name(), restfulRequestContext);
    }

    public static MetricInfoDTO createMetricDTO(Metric metric, String metricName, ServiceContext restfulRequestContext) {
        MetricInfoDTO metricInfoDTO = DtoFactory.getInstance().createDto(MetricInfoDTO.class);
        metricInfoDTO.setName(metricName);
        metricInfoDTO.setDescription(metric.getDescription());
        metricInfoDTO.setLinks(getLinks(metricName, restfulRequestContext));
        return metricInfoDTO;
    }

    public static List<Link> getLinks(String metricName, ServiceContext restfulRequestContext) {
        final UriBuilder servicePathBuilder = restfulRequestContext.getServiceUriBuilder();
        List<Link> links = new ArrayList<>();

        final Link statusLink = DtoFactory.getInstance().createDto(Link.class);
        statusLink.setRel(Constants.LINK_REL_GET_METRIC_VALUE);
        statusLink.setHref(servicePathBuilder.clone().path(getMethod("getValue")).build(metricName, "name").toString());
        statusLink.setMethod("GET");
        statusLink.setProduces(MediaType.APPLICATION_JSON);
        links.add(statusLink);
        return links;
    }

    public static Method getMethod(String name) {
        for (Method analyticsMethod : AnalyticsService.class.getMethods()) {
            if (analyticsMethod.getName().equals(name)) {
                return analyticsMethod;
            }
        }

        throw new RuntimeException("No '" + name + "' method found in " + AnalyticsService.class + "class");
    }
}
