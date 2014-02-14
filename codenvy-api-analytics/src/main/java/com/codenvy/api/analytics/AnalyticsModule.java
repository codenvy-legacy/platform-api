/*
 *
 * CODENVY CONFIDENTIAL
 * ________________
 *
 * [2012] - [2014] Codenvy, S.A.
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

import com.codenvy.api.analytics.impl.DummyMetricHandler;
import com.codenvy.inject.DynaModule;
import com.google.inject.AbstractModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

/** @author Anatoliy Bazko */
@DynaModule
public class AnalyticsModule extends AbstractModule {

    private static final Logger LOG = LoggerFactory.getLogger(AnalyticsModule.class);

    private static final String CODENVY_LOCAL_CONF_DIR    = "codenvy.local.conf.dir";
    private static final String ANALYTICS_CONF_FILENAME   = "analytics.properties";
    private static final String METRIC_HANDLER_CLASS_NAME = MetricHandler.class.getName();

    @Override
    protected void configure() {
        MetricHandler metricHandler;
        try {
            metricHandler = instantiateMetricHandler();
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException |
                InstantiationException e) {
            metricHandler = new DummyMetricHandler();
            LOG.error(e.getMessage(), e);
        }

        LOG.info(metricHandler.getClass().getName() + " is used");

        bind(MetricHandler.class).toInstance(metricHandler);
        bind(AnalyticsService.class);
    }

    private MetricHandler instantiateMetricHandler() throws NoSuchMethodException,
                                                            IllegalAccessException,
                                                            InvocationTargetException,
                                                            InstantiationException {
        Properties properties;
        try {
            properties = readAnalyticsProperties();
        } catch (IOException e) {
            LOG.warn("Error reading " + ANALYTICS_CONF_FILENAME + " " + e.getMessage());
            return new DummyMetricHandler();
        }

        String clazzName = (String)properties.get(METRIC_HANDLER_CLASS_NAME);
        if (clazzName == null) {
            return new DummyMetricHandler();

        } else {
            try {
                Class<?> clazz = Class.forName(clazzName);

                try {
                    return (MetricHandler)clazz.getConstructor(Properties.class).newInstance(properties);
                } catch (NoSuchMethodException e) {
                    return (MetricHandler)clazz.getConstructor().newInstance();
                }
            } catch (ClassNotFoundException e) {
                return new DummyMetricHandler();
            }
        }
    }

    private Properties readAnalyticsProperties() throws IOException {
        String fileName = System.getProperty(CODENVY_LOCAL_CONF_DIR) + File.separator + ANALYTICS_CONF_FILENAME;

        try (InputStream in = new FileInputStream(new File(fileName))) {
            Properties properties = new Properties();
            properties.load(in);

            return properties;
        }
    }
}
