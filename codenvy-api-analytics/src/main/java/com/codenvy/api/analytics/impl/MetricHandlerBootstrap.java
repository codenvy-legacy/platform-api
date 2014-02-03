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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

/** @author <a href="mailto:dkuleshov@codenvy.com">Dmitry Kuleshov</a> */
public class MetricHandlerBootstrap implements ServletContextListener {
    public static final  String METRIC_HANDLER_NAME     = MetricHandler.class.getName();
    public static final  String CODENVY_LOCAL_CONF_DIR  = "codenvy.local.conf.dir";
    public static final  String ANALYTICS_CONF_FILENAME = "old/analytics.conf";
    private static final Logger LOG                     = LoggerFactory.getLogger(MetricHandlerBootstrap.class);

    public void contextDestroyed(ServletContextEvent sce) {
        ServletContext servletContext = sce.getServletContext();
        servletContext.removeAttribute(METRIC_HANDLER_NAME);
    }

    public void contextInitialized(ServletContextEvent sce) {
        ServletContext servletContext = sce.getServletContext();
        servletContext.setAttribute(METRIC_HANDLER_NAME, new DummyMetricHandler());

        String fileName = System.getProperty(CODENVY_LOCAL_CONF_DIR) + File.separator + ANALYTICS_CONF_FILENAME;
        Properties properties = readProperties(fileName);
        String metricHandlerImplFQN = properties.getProperty(METRIC_HANDLER_NAME);

        if (metricHandlerImplFQN == null) {
            LOG.warn("Configuration file " + fileName + " does not define proper " + MetricHandler.class.getName() +
                     " implementation, default implementation will be used");
            return;

        } else {
            try {
                Constructor constructor = getConstructorWithProperties(metricHandlerImplFQN);

                if (constructor != null) {
                    servletContext.setAttribute(METRIC_HANDLER_NAME, constructor.newInstance(properties));
                    return;
                } else {
                    servletContext.setAttribute(METRIC_HANDLER_NAME, getDefaultConstructor(metricHandlerImplFQN).newInstance());
                    return;
                }

            } catch (IllegalStateException | IllegalAccessException | InstantiationException | InvocationTargetException | ClassNotFoundException e) {
                LOG.warn(
                        "Error during instantiation of " + MetricHandler.class.getName() +
                        " implementation defined in configuration file " + fileName + ", default implementation will be used");
            }

        }
    }

    private Constructor getConstructorWithProperties(String metricHandlerImplFQN)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException {
        for (Constructor constructor : Class.forName(metricHandlerImplFQN).getDeclaredConstructors()) {
            Class<?>[] pType = constructor.getParameterTypes();
            if (pType.length == 1 && pType[0].equals(Properties.class)) {
                return constructor;
            }
        }
        return null;

    }

    private Constructor getDefaultConstructor(String metricHandlerImplFQN)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException {
        for (Constructor constructor : Class.forName(metricHandlerImplFQN).getDeclaredConstructors()) {
            Class<?>[] pType = constructor.getParameterTypes();
            if (pType.length == 0) {
                return constructor;
            }
        }
        return null;

    }

    private Properties readProperties(String fileName) {
        Properties properties = new Properties();

        try {
            properties.load(new FileInputStream(new File(fileName)));
        } catch (FileNotFoundException e) {
            LOG.warn("File " + fileName + " not found, default implementation will be used");
        } catch (IOException e) {
            LOG.warn("Error while reading file " + fileName + ", default implementation will be used");
        }

        return properties;
    }


}
