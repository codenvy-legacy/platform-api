package com.codenvy.api.factory.dto;

import com.codenvy.api.factory.FactoryParameter;
import com.codenvy.api.factory.parameter.*;
import com.codenvy.dto.shared.DTO;

import static com.codenvy.api.factory.FactoryParameter.Obligation.MANDATORY;
import static com.codenvy.api.factory.FactoryParameter.Obligation.OPTIONAL;
import static com.codenvy.api.factory.FactoryParameter.Version.V1_0;
import static com.codenvy.api.factory.FactoryParameter.Version.V1_1;

/**
 * @author Sergii Kabashniuk
 */
@DTO
public interface FactoryV1_0 {
    /**
     * @return Version for Codenvy Factory API.
     */
    @FactoryParameter(obligation = MANDATORY)
    String getV();

    void setV(String v);

    /**
     * @return Version Control System used. Now only one possible value: git
     */
    @FactoryParameter(obligation = MANDATORY)
    String getVcs();

    void setVcs(String vcs);

    /**
     * @return Locations of sources in Version Control System.
     */
    @FactoryParameter(obligation = MANDATORY)
    String getVcsurl();

    void setVcsurl(String vcs);

    /**
     * @return Latest commit ID.
     * <p/>
     * (using the commit ID avoid to remember which branch the user was working on, it will b)
     */
    @FactoryParameter(obligation = OPTIONAL)
    String getCommitid();

    void setCommitid(String commitid);

    @Deprecated
    @FactoryParameter(obligation = OPTIONAL, converter = IdCommitConverter.class, deprecatedSince = V1_1)
    String getIdcommit();

    @Deprecated
    void setIdcommit(String idcommit);

    /**
     * @return Name of the project in temporary workspace after the exporting source code from vcsurl.
     * <p/>
     * Project name should be in valid format,
     * <p/>
     * specified by this spec: URL Scheme V3 for workspaces/projects#ImpactsonProjectnames
     */
    @Deprecated
    @FactoryParameter(obligation = OPTIONAL, converter = PnameConverter.class, deprecatedSince = V1_1)
    String getPname();

    @Deprecated
    void setPname(String ptype);

    /**
     * @return Project type.
     */
    @Deprecated
    @FactoryParameter(obligation = OPTIONAL, converter = PtypeConverter.class, deprecatedSince = V1_1)
    String getPtype();

    @Deprecated
    void setPtype(String ptype);

    /**
     * @return This value indicates actions to be performed after a temporary workspace has been created.
     * <p/>
     * Currently, the only available value is openproject. More actions will be available in future.
     */
    @FactoryParameter(obligation = OPTIONAL)
    String getAction();

    void setAction(String action);

    @Deprecated
    @FactoryParameter(obligation = OPTIONAL, ignoredSince = V1_0)
    String getWname();

    @Deprecated
    void setWname(String wname);
}
