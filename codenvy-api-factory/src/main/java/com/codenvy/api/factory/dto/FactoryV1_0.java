package com.codenvy.api.factory.dto;

import java.util.List;
import com.codenvy.api.core.rest.shared.dto.Link;
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

    /**
     * @return The time when the factory becomes valid (in milliseconds, from Unix epoch, no timezone)
     */
    @Deprecated
    long getValidsince();

    @Deprecated
    void setValidsince(long validsince);


    /**
     * @return The time when the factory becomes invalid (in milliseconds, from Unix epoch, no timezone)
     */
    @Deprecated
    long getValiduntil();

    @Deprecated
    void setValiduntil(long validuntil);

    /**
     * @return Allow to use text replacement in project files after clone
     */
    Variable getVariable();

    void setVariable(Variable variable);

    /**
     * @return welcome page configuration.
     */
    WelcomePage getWelcome();

    void setWelcome(WelcomePage welcome);


    List<Link> getLinks();

    void setLinks(List<Link> links);

    FactoryV1_0 withLinks(List<Link> links);


}
