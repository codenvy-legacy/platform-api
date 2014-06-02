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
package com.codenvy.api.core.rest.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation may be applied to methods of sub-class of {@link com.codenvy.api.core.rest.Service}. When client accesses method {@link
 * com.codenvy.api.core.rest.Service#getServiceDescriptor()} all methods with this annotation are analyzed to provide descriptor of
 * particular service. In instance of {@link com.codenvy.api.core.rest.shared.dto.ServiceDescriptor ServiceDescriptor} methods with this annotation
 * are represented as set of links. It should help client to understand capabilities of particular RESTful service.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @see com.codenvy.api.core.rest.Service
 * @see com.codenvy.api.core.rest.shared.dto.ServiceDescriptor
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface GenerateLink {
    String rel();
}
