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
import com.codenvy.api.analytics.exception.MetricNotFoundException;

import javax.ws.rs.core.UriInfo;

import java.util.List;
import java.util.Map;

/**
 * Defines methods to interact with analytic system metrics to get needed metric values or info. In order to proper
 * instantiate an
 * implementation, it must either have default constructor or
 * constructor with single parameter {@link java.util.Properties}.
 *
 * @author <a href="mailto:dkuleshov@codenvy.com">Dmitry Kuleshov</a>
 */
public interface MetricHandler {
    public MetricValueDTO getValue(String metricName,
                                   Map<String, String> metricContext,
                                   UriInfo uriInfo) throws MetricNotFoundException;
    
    public MetricValueListDTO getUserValues(List<String> metricNames,
                                          Map<String, String> metricContext,
                                          UriInfo uriInfo) throws MetricNotFoundException;

    public MetricInfoDTO getInfo(String metricName, UriInfo uriInfo) throws MetricNotFoundException;

    public MetricInfoListDTO getAllInfo(UriInfo uriInfo);
}
