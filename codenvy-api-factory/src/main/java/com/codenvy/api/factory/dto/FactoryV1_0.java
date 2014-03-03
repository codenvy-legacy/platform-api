package com.codenvy.api.factory.dto;

import com.codenvy.api.factory.Compatibility;
import com.codenvy.api.factory.V1_2AggregateConverter;
import com.codenvy.dto.shared.DTO;

import static com.codenvy.api.factory.Compatibility.Optionality.MANDATORY;
import static com.codenvy.api.factory.Compatibility.Optionality.OPTIONAL;
import static com.codenvy.api.factory.Compatibility.Version.V1_0;
import static com.codenvy.api.factory.Compatibility.Version.V1_1;

/**
 * @author Sergii Kabashniuk
 */
@DTO
public interface FactoryV1_0 {
    /**
     * @return Version for Codenvy Factory API.
     */
    @Compatibility(optionality = MANDATORY)
    String getV();

    void setV(String v);

    /**
     * @return Version Control System used. Now only one possible value: git
     */
    @Compatibility(optionality = MANDATORY)
    String getVcs();

    void setVcs(String vcs);

    /**
     * @return Locations of sources in Version Control System.
     */
    @Compatibility(optionality = MANDATORY)
    String getVcsurl();

    void setVcsurl(String vcs);

    /**
     * @return Latest commit ID.
     * <p/>
     * (using the commit ID avoid to remember which branch the user was working on, it will b)
     */
    @Compatibility(optionality = OPTIONAL)
    String getCommitid();

    void setCommitid(String commitid);

    @Deprecated
    @Compatibility(optionality = OPTIONAL, deprecatedSince = V1_1, converter = V1_2AggregateConverter.class)
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
    @Compatibility(optionality = OPTIONAL, deprecatedSince = V1_1, converter = V1_2AggregateConverter.class)
    String getPname();

    @Deprecated
    void setPname(String ptype);

    /**
     * @return Project type.
     */
    @Deprecated
    @Compatibility(optionality = OPTIONAL, deprecatedSince = V1_1, converter = V1_2AggregateConverter.class)
    String getPtype();

    @Deprecated
    void setPtype(String ptype);

    /**
     * @return This value indicates actions to be performed after a temporary workspace has been created.
     * <p/>
     * Currently, the only available value is openproject. More actions will be available in future.
     */
    @Compatibility(optionality = OPTIONAL)
    String getAction();

    void setAction(String action);

    @Deprecated
    @Compatibility(optionality = OPTIONAL, ignoredSince = V1_0)
    String getWname();

    @Deprecated
    void setWname(String wname);
}
