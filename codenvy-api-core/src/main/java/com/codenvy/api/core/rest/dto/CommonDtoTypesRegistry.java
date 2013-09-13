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
package com.codenvy.api.core.rest.dto;

import java.util.Set;

/**
 * Delivers embedded DTO.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
public class CommonDtoTypesRegistry extends DtoTypesRegistry {
    @Override
    protected void addDtos(Set<Class<?>> dtos) {
        dtos.add(Link.class);
        dtos.add(ParameterDescriptor.class);
        dtos.add(ParameterType.class);
        dtos.add(RequestBodyDescriptor.class);
        dtos.add(ServiceDescriptor.class);
        dtos.add(ServiceError.class);
    }
}
