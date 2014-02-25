package com.codenvy.api.factory.dto;

import com.codenvy.dto.shared.DTO;

/**
 * @author Sergii Kabashniuk
 */
@DTO
public interface FactoryV1_0 {
    /**
     * @return Version for Codenvy Factory API.
     */
    String getV();

    void setV(String v);

    /**
     * @return Version Control System used. Now only one possible value: git
     */
    String getVcs();

    void setVcs(String vcs);

    /**
     * @return Locations of sources in Version Control System.
     */
    String getVcsurl();

    void setVcsurl(String vcs);

    /**
     * @return Latest commit ID.
     * <p/>
     * (using the commit ID avoid to remember which branch the user was working on, it will b)
     */
    String getCommitid();

    void setCommitid(String commitid);

    @Deprecated
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
    String getPname();

    @Deprecated
    void setPname(String ptype);

    /**
     * @return Project type.
     */
    @Deprecated
    String getPtype();

    @Deprecated
    void setPtype(String ptype);

    /**
     * @return This value indicates actions to be performed after a temporary workspace has been created.
     * <p/>
     * Currently, the only available value is openproject. More actions will be available in future.
     */
    String getAction();

    void setAction(String action);

    @Deprecated
    String getWname();

    @Deprecated
    void setWname(String wname);
}
