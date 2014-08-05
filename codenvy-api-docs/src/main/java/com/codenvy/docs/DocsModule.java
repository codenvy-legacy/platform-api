/*******************************************************************************
 * Copyright (c) 2012-2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.docs;

import com.codenvy.inject.DynaModule;
import com.google.inject.AbstractModule;
import com.wordnik.swagger.config.ConfigFactory;
import com.wordnik.swagger.config.ScannerFactory;
import com.wordnik.swagger.config.SwaggerConfig;
import com.wordnik.swagger.jaxrs.config.DefaultJaxrsScanner;
import com.wordnik.swagger.jaxrs.listing.ApiDeclarationProvider;
import com.wordnik.swagger.jaxrs.listing.ApiListingResource;
import com.wordnik.swagger.jaxrs.listing.ResourceListingProvider;
import com.wordnik.swagger.jaxrs.reader.DefaultJaxrsApiReader;
import com.wordnik.swagger.reader.ClassReaders;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * @author andrew00x
 */
@DynaModule
public class DocsModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(CodenvyApiDocsService.class);
        bind(ResourceListingProvider.class);
        bind(ApiDeclarationProvider.class);
        bind(SwaggerBootstrap.class).asEagerSingleton();
    }

    @Path("/docs")
    @Produces("application/json")
    public static class CodenvyApiDocsService extends ApiListingResource {
    }

    static class SwaggerBootstrap {
        @Inject
        @Named("api.endpoint")
        String baseApiUrl;

        @PostConstruct
        public void init() {
            final SwaggerConfig config = ConfigFactory.config();
            config.setBasePath(baseApiUrl);
            ScannerFactory.setScanner(new DefaultJaxrsScanner());
            ClassReaders.setReader(new DefaultJaxrsApiReader());
        }
    }
}
