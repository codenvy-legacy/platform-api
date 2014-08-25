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
package com.codenvy.api.project.shared.dto;

import com.codenvy.dto.shared.DTO;
import com.wordnik.swagger.annotations.ApiModelProperty;

/**
 * @author Vitaly Parfonov
 */
@DTO
public interface ImportSourceDescriptor {

    /**
     * @param type e.g git, zip
     */
    void setType(String type);

    /**
     * @return type of importer e.g zip, git
     */
    @ApiModelProperty(value = "Importer type", required = true, allowableValues = "zip,git,svn")
    String getType();

    /**
     * @param location to the resource
     */
    void setLocation(String location);

    /**
     * @return location to the resource
     */
    @ApiModelProperty(value = "Location of remote resources to be imported", required = true)
    String getLocation();

    ImportSourceDescriptor withType(String type);

    ImportSourceDescriptor withLocation(String location);

}
