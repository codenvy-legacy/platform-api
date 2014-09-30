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
package com.codenvy.api.factory.dto;

import com.codenvy.api.core.factory.FactoryParameter;
import com.codenvy.dto.shared.DTO;

import static com.codenvy.api.core.factory.FactoryParameter.Obligation.OPTIONAL;

/** @author Sergii Kabashniuk */
@DTO
@Deprecated
public interface ProjectAttributes {

    /**
     * @return Name of the project in temporary workspace after the exporting source code from vcsurl.
     * <p/>
     * Project queryParameterName should be in valid format,
     */
    @Deprecated
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "pname")
    String getPname();

    void setPname(String pname);

    ProjectAttributes withPname(String pname);

    /**
     * @return Project type.
     */
    @Deprecated
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "ptype")
    public String getPtype();

    void setPtype(String ptype);

    ProjectAttributes withPtype(String ptype);


    /**
     * @return builder name.
     */
    @Deprecated
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "buildername")
    public String getBuildername();

    void setBuildername(String builderName);

    ProjectAttributes withBuildername(String builderName);

    /**
     * @return runner name.
     */
    @Deprecated
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "runnername")
    public String getRunnername();

    void setRunnername(String runnerName);

    ProjectAttributes withRunnername(String runnerName);


    /**
     * @return runner environment id.
     */
    @Deprecated
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "runnerenvironmentid")
    public String getRunnerenvironmentid();

    void setRunnerenvironmentid(String runnerEnvironmentId);

    ProjectAttributes withRunnerenvironmentid(String runnerEnvironmentId);

}
