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
package com.codenvy.api.factory.dto;

import com.codenvy.api.factory.parameter.FactoryParameter;
import com.codenvy.dto.shared.DTO;

import static com.codenvy.api.factory.parameter.FactoryParameter.Obligation.OPTIONAL;

/** @author Sergii Kabashniuk */
@DTO
public interface ProjectAttributes {

    /**
     * @return Name of the project in temporary workspace after the exporting source code from vcsurl.
     * <p/>
     * Project queryParameterName should be in valid format,
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "pname")
    String getPname();

    void setPname(String pname);

    ProjectAttributes withPname(String pname);

    /**
     * @return Project type.
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "ptype")
    public String getPtype();

    void setPtype(String ptype);

    ProjectAttributes withPtype(String ptype);
}
