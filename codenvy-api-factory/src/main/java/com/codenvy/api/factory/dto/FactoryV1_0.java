package com.codenvy.api.factory.dto;

import com.codenvy.api.factory.parameter.FactoryParameter;
import com.codenvy.dto.shared.DTO;

import static com.codenvy.api.factory.FactoryFormat.NONENCODED;
import static com.codenvy.api.factory.parameter.FactoryParameter.Obligation.MANDATORY;
import static com.codenvy.api.factory.parameter.FactoryParameter.Obligation.OPTIONAL;
import static com.codenvy.api.factory.parameter.FactoryParameter.Version.V1_0;
import static com.codenvy.api.factory.parameter.FactoryParameter.Version.V1_1;

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
    @FactoryParameter(obligation = MANDATORY, queryParameterName = "vcs")
    String getVcs();

    void setVcs(String vcs);

    FactoryV1_0 withVcs(String vcs);

    /**
     * @return Locations of sources in Version Control System.
     */
    @FactoryParameter(obligation = MANDATORY, queryParameterName = "vcsurl")
    String getVcsurl();

    void setVcsurl(String vcs);

    FactoryV1_0 withVcsurl(String vcs);

    /**
     * @return Latest commit ID.
     * <p/>
     * (using the commit ID avoid to remember which branch the user was working on, it will b)
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "commitid")
    String getCommitid();

    void setCommitid(String commitid);

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
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "action")
    String getAction();

    void setAction(String action);

    FactoryV1_0 withAction(String action);

    /**
     * @return Indicates should .git folder be removed after cloning (allow commit to origin repository)
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "vcsinfo")
    boolean getVcsinfo();

    void setVcsinfo(boolean vcsinfo);

    FactoryV1_0 withVcsinfo(boolean vcsinfo);

    @Deprecated
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "wname", ignoredSince = V1_0, deprecatedSince = V1_1)
    String getWname();

    @Deprecated
    void setWname(String wname);

    @Deprecated
    FactoryV1_0 withWname(String wname);
}
