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
package com.codenvy.api.core.config;

import com.codenvy.api.core.util.ApplicationServerPathDetector;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Initialize singleton configuration when servlet context started.
 * It checks configuration in next order:
 * <ol>
 * <li>Programmatically added entries to the {@code SingletonConfiguration.get()} instance</li>
 * <li>The file <i>configuration.properties</i> in root directory of application server</li>
 * <li>The file <i>configuration.properties</i> in directory <i>WEB-INF/classes/conf/</i> in web application</li>
 * <li>The resources named <i>conf/configuration.properties</i> in all jar files</li>
 * </l>
 * Configuration that is loaded from <i>WEB-INF/classes/conf/</i> has preference over configuration that is loaded from jar files.
 * Configuration that is loaded from root directory of application server has preference over over all other configuration.
 *
 * @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a>
 */
public class ConfigurationListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        final java.io.File serverRoot = ApplicationServerPathDetector.findRoot();
        final Configuration configuration = SingletonConfiguration.get();
        if (serverRoot != null) {
            final java.io.File rootCfg = new java.io.File(serverRoot, "configuration.properties");
            if (rootCfg.exists()) {
                final Properties rootProps = new Properties();
                try (InputStream in = new FileInputStream(rootCfg)) {
                    rootProps.load(in);
                } catch (IOException e) {
                    throw new IllegalStateException(e.getMessage(), e);
                }
                configuration.merge(new Configuration(rootProps), Configuration.MergePolicy.RETAIN);
            }
        }
        final ServletContext servletContext = sce.getServletContext();
        try {
            final URL webAppCfg = servletContext.getResource("WEB-INF/classes/conf/configuration.properties");
            if (webAppCfg != null) {
                final Properties webAppProps = new Properties();
                try (InputStream in = webAppCfg.openStream()) {
                    webAppProps.load(in);
                } catch (IOException e) {
                    throw new IllegalStateException(e.getMessage(), e);
                }
                configuration.merge(new Configuration(webAppProps), Configuration.MergePolicy.RETAIN);
            }
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        try {
            final Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources("conf/configuration.properties");
            final Properties resProps = new Properties();
            while (resources.hasMoreElements()) {
                final URL resUrl = resources.nextElement();
                if (resUrl.toString().endsWith("/WEB-INF/classes/conf/configuration.properties")) {
                    // already loaded
                    continue;
                }
                try (InputStream in = resUrl.openStream()) {
                    resProps.load(in);
                } catch (IOException e) {
                    throw new IllegalStateException(e.getMessage(), e);
                }
            }
            configuration.merge(new Configuration(resProps), Configuration.MergePolicy.RETAIN);
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }
}
