/*******************************************************************************
* Copyright (c) 2012-2014 Codenvy, S.A.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
* Codenvy, S.A. - initial API and implementation
*******************************************************************************/
package com.codenvy.api.vfs.server;

import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

/**
 * Setup {@link URLStreamHandlerFactory} to be able use URL for access to virtual file system. It is not possible to
 * provide
 * correct {@link URLStreamHandler} by system property 'java.protocol.handler.pkgs'. Bug in Oracle JDK:
 * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4648098
 *
 * @author <a href="mailto:aparfonov@exoplatform.com">Andrey Parfonov</a>
 */
public class URLHandlerFactorySetup {
    private static final Logger LOG = LoggerFactory.getLogger(URLHandlerFactorySetup.class);

    public synchronized static void setup(VirtualFileSystemRegistry registry) {
        try {
            new URL("ide+vfs", "", "");
        } catch (MalformedURLException mue) {
            // URL with protocol 'ide+vfs' is not supported yet. Need register URLStreamHandlerFactory.

            if (LOG.isDebugEnabled()) {
                LOG.debug("--> Try setup URLStreamHandlerFactory for protocol 'ide+vfs'. ");
            }
            try {
                // Get currently installed URLStreamHandlerFactory.
                Field factoryField = URL.class.getDeclaredField("factory");
                factoryField.setAccessible(true);
                URLStreamHandlerFactory currentFactory = (URLStreamHandlerFactory)factoryField.get(null);

                if (LOG.isDebugEnabled()) {
                    LOG.debug("--> Current instance of URLStreamHandlerFactory: "
                              + (currentFactory != null ? currentFactory.getClass().getName() : null));
                }

                //
                URLStreamHandlerFactory vfsURLFactory = new VirtualFileSystemURLHandlerFactory(currentFactory, registry);
                factoryField.set(null, vfsURLFactory);
            } catch (SecurityException | NoSuchFieldException | IllegalAccessException se) {
                throw new VirtualFileSystemRuntimeException(se.getMessage(), se);
            }

            // Check 'ide+vfs' again. From now it should be possible to use such URLs.
            // At the same time we force URL to remember our protocol handler.
            // URL knows about it even if the URLStreamHandlerFactory is changed.

            try {
                new URL("ide+vfs", "", "");

                //
                if (LOG.isDebugEnabled()) {
                    LOG.debug("--> URLStreamHandlerFactory installed. ");
                }
            } catch (MalformedURLException e) {
                throw new VirtualFileSystemRuntimeException(e.getMessage(), e);
            }
        }
    }

    protected URLHandlerFactorySetup() {
    }
}
