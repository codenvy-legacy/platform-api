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
package com.codenvy.api.builder.manager.dto;

import com.codenvy.api.core.rest.dto.DtoType;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
@DtoType(BuilderManagerDtoTypes.BUILDER_SERVICE_REGISTRATION_TYPE)
public class BuilderServiceRegistration {
    private BuilderServiceLocation       builderServiceLocation;
    private BuilderServiceAccessCriteria builderServiceAccessCriteria;

    public BuilderServiceRegistration(BuilderServiceLocation builderServiceLocation,
                                      BuilderServiceAccessCriteria builderServiceAccessCriteria) {
        this.builderServiceLocation = builderServiceLocation;
        this.builderServiceAccessCriteria = builderServiceAccessCriteria;
    }

    public BuilderServiceRegistration() {
    }

    public BuilderServiceLocation getBuilderServiceLocation() {
        return builderServiceLocation;
    }

    public void setBuilderServiceLocation(BuilderServiceLocation builderServiceLocation) {
        this.builderServiceLocation = builderServiceLocation;
    }

    public BuilderServiceAccessCriteria getBuilderServiceAccessCriteria() {
        return builderServiceAccessCriteria;
    }

    public void setBuilderServiceAccessCriteria(BuilderServiceAccessCriteria builderServiceAccessCriteria) {
        this.builderServiceAccessCriteria = builderServiceAccessCriteria;
    }

    @Override
    public String toString() {
        return "BuilderServiceRegistration{" +
               "builderServiceLocation=" + builderServiceLocation +
               ", builderServiceAccessCriteria=" + builderServiceAccessCriteria +
               '}';
    }
}
