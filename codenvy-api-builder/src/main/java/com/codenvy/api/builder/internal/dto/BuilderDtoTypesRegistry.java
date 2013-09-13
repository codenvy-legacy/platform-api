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
package com.codenvy.api.builder.internal.dto;

import com.codenvy.api.builder.manager.dto.BuilderServiceAccessCriteria;
import com.codenvy.api.builder.manager.dto.BuilderServiceLocation;
import com.codenvy.api.builder.manager.dto.BuilderServiceRegistration;
import com.codenvy.api.builder.manager.dto.BuilderState;
import com.codenvy.api.core.rest.dto.DtoTypesRegistry;

import java.util.Set;

/**
 * Delivers DTOs of Builder API.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
public class BuilderDtoTypesRegistry extends DtoTypesRegistry {
    @Override
    protected void addDtos(Set<Class<?>> dtos) {
        dtos.add(BuilderList.class);
        dtos.add(BuildTaskDescriptor.class);
        dtos.add(BuilderDescriptor.class);
        dtos.add(BuildRequest.class);
        dtos.add(BuildStatus.class);
        dtos.add(DependencyRequest.class);
        dtos.add(SlaveBuilderState.class);
        dtos.add(InstanceState.class);
        dtos.add(BuilderServiceRegistration.class);
        dtos.add(BuilderServiceLocation.class);
        dtos.add(BuilderServiceAccessCriteria.class);
        dtos.add(BuilderState.class);
    }
}
