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

package com.codenvy.api.analytics.impl;

import com.codenvy.api.analytics.MetricHandler;
import com.codenvy.api.analytics.dto.MetricInfoDTO;
import com.codenvy.api.analytics.dto.MetricInfoListDTO;
import com.codenvy.api.analytics.dto.MetricValueDTO;
import com.codenvy.api.analytics.exception.MetricNotFoundException;
import com.codenvy.api.core.rest.ServiceContext;
import com.codenvy.dto.server.DtoFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Simple dummy extension to avoid huge operations. Instead default values returned.
 *
 * @author <a href="mailto:dkuleshov@codenvy.com">Dmitry Kuleshov</a>
 */
public class DummyMetricHandler implements MetricHandler {

    @Override
    public MetricValueDTO getValue(String metricName, Map<String, String> metricContext, ServiceContext serviceContext)
            throws MetricNotFoundException {
        return createDummyMetricValueDTO(metricName);
    }

    @Override
    public MetricInfoDTO getInfo(String metricName, ServiceContext serviceContext) throws MetricNotFoundException {
        return createDummyMetricInfoDto(metricName);
    }

    @Override
    public MetricInfoListDTO getAllInfo(ServiceContext serviceContext) {
        return null;
    }

    private MetricInfoDTO createDummyMetricInfoDto(String metricName) {
        MetricInfoDTO metricInfoDTO = DtoFactory.getInstance().createDto(MetricInfoDTO.class);
        metricInfoDTO.setName(metricName);
        metricInfoDTO.setDescription(metricName + " description");

        return metricInfoDTO;
    }

    private MetricValueDTO createDummyMetricValueDTO(String metricName) {
        MetricValueDTO metricValueDTO = DtoFactory.getInstance().createDto(MetricValueDTO.class);
        metricValueDTO.setValue(metricName + " value");

        return metricValueDTO;
    }

    private MetricInfoListDTO createDummyMetricInfoListDTO() {
        MetricInfoListDTO metricInfoListDTO = DtoFactory.getInstance().createDto(MetricInfoListDTO.class);
        int counter = 10;
        List<MetricInfoDTO> metricInfoDTOs = new ArrayList<>();

        while (counter-- > 0) {
            metricInfoDTOs.add(createDummyMetricInfoDto("Metric " + counter));
        }
        metricInfoListDTO.setMetrics(metricInfoDTOs);
        return metricInfoListDTO;
    }
}
