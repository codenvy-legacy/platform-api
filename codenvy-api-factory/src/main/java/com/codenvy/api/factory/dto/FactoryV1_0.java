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

import static com.codenvy.api.core.factory.FactoryParameter.FactoryFormat.NONENCODED;
import static com.codenvy.api.core.factory.FactoryParameter.Obligation.MANDATORY;
import static com.codenvy.api.core.factory.FactoryParameter.Obligation.OPTIONAL;
import static com.codenvy.api.core.factory.FactoryParameter.Version.V1_0;
import static com.codenvy.api.core.factory.FactoryParameter.Version.V1_1;
import static com.codenvy.api.core.factory.FactoryParameter.Version.V2_0;

/**
 * Factory of version 1.0
 *
 * @author Sergii Kabashniuk
 */
@DTO
public interface FactoryV1_0 {
    /**
     * @return Version for Codenvy Factory API.
     */
    @FactoryParameter(obligation = MANDATORY, queryParameterName = "v")
    String getV();

    void setV(String v);

    FactoryV1_0 withV(String v);

    /**
     * @return Version Control System used. Now only one possible value: git
     */
    @FactoryParameter(obligation = MANDATORY, queryParameterName = "vcs", deprecatedSince = V2_0)
    @Deprecated
    String getVcs();

    @Deprecated
    void setVcs(String vcs);

    @Deprecated
    FactoryV1_0 withVcs(String vcs);

    /**
     * @return Locations of sources in Version Control System.
     */
    @FactoryParameter(obligation = MANDATORY, queryParameterName = "vcsurl", deprecatedSince = V2_0)
    @Deprecated
    String getVcsurl();

    @Deprecated
    void setVcsurl(String vcs);

    @Deprecated
    FactoryV1_0 withVcsurl(String vcs);

    /**
     * @return Latest commit ID.
     * <p/>
     * (using the commit ID avoid to remember which branch the user was working on, it will b)
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "commitid", deprecatedSince = V2_0)
    @Deprecated
    String getCommitid();

    @Deprecated
    void setCommitid(String commitid);

    @Deprecated
    FactoryV1_0 withCommitid(String commitid);

    @Deprecated
    @FactoryParameter(obligation = OPTIONAL, format = NONENCODED, queryParameterName = "idcommit", deprecatedSince = V1_1)
    String getIdcommit();

    @Deprecated
    void setIdcommit(String idcommit);

    @Deprecated
    FactoryV1_0 withIdcommit(String idcommit);

    /**
     * @return Name of the project in temporary workspace after the exporting source code from vcsurl.
     * <p/>
     * Project queryParameterName should be in valid format,
     * <p/>
     * specified by this spec: URL Scheme V3 for workspaces/projects#ImpactsonProjectnames
     */
    @Deprecated
    @FactoryParameter(obligation = OPTIONAL, format = NONENCODED, queryParameterName = "pname", deprecatedSince = V1_1)
    String getPname();

    @Deprecated
    void setPname(String ptype);

    @Deprecated
    FactoryV1_0 withPname(String ptype);

    /**
     * @return Project type.
     */
    @Deprecated
    @FactoryParameter(obligation = OPTIONAL, format = NONENCODED, queryParameterName = "ptype", deprecatedSince = V1_1)
    String getPtype();

    @Deprecated
    void setPtype(String ptype);

    @Deprecated
    FactoryV1_0 withPtype(String ptype);

    /**
     * @return This value indicates actions to be performed after a temporary workspace has been created.
     * <p/>
     * Currently, the only available value is openproject. More actions will be available in future.
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "action", deprecatedSince = V2_0)
    @Deprecated
    String getAction();

    @Deprecated
    void setAction(String action);

    @Deprecated
    FactoryV1_0 withAction(String action);

    /**
     * @return Indicates should .git folder be removed after cloning (allow commit to origin repository)
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "vcsinfo", deprecatedSince = V2_0)
    @Deprecated
    boolean getVcsinfo();

    @Deprecated
    void setVcsinfo(boolean vcsinfo);

    @Deprecated
    FactoryV1_0 withVcsinfo(boolean vcsinfo);

    /**
     * @return path of the file to open in the project.
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "openfile", deprecatedSince = V2_0)
    @Deprecated
    String getOpenfile();

    @Deprecated
    void setOpenfile(String openfile);

    @Deprecated
    FactoryV1_0 withOpenfile(String openfile);

    @Deprecated
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "wname", ignoredSince = V1_0, deprecatedSince = V1_1)
    String getWname();

    @Deprecated
    void setWname(String wname);

    @Deprecated
    FactoryV1_0 withWname(String wname);
}
