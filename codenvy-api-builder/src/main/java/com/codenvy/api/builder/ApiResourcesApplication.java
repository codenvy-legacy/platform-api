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
package com.codenvy.api.builder;

import com.codenvy.api.builder.internal.SlaveBuilderService;
import com.codenvy.api.core.rest.ApiExceptionMapper;

import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
public class ApiResourcesApplication extends Application {
    private final Set<Class<?>> classes;
    private final Set<Object>   instances;

    public ApiResourcesApplication() {
        classes = new HashSet<>();
        classes.add(SlaveBuilderService.class);
        classes.add(BuilderService.class);
        instances = new HashSet<>();
        instances.add(new ApiExceptionMapper());
    }

    @Override
    public Set<Class<?>> getClasses() {
        return classes;
    }

    @Override
    public Set<Object> getSingletons() {
        return instances;
    }
}
