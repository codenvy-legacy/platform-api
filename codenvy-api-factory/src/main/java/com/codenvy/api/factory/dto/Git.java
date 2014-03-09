package com.codenvy.api.factory.dto;

import com.codenvy.api.factory.parameter.FactoryParameter;
import com.codenvy.dto.shared.DTO;

import static com.codenvy.api.factory.parameter.FactoryParameter.Obligation.OPTIONAL;

/**
 * @author Sergii Kabashniuk
 */
@DTO
public interface Git {
    /**
     * @return Allows get ref-specs of the changes of remote repository
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "configremoteoriginfetch")
    String getConfigremoteoriginfetch();

    void setConfigremoteoriginfetch(String configremoteoriginfetch);

    /**
     * See https://www.kernel.org/pub/software/scm/git/docs/git-config.html push.default
     *
     * @return
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "configpushdefault")
    String getConfigpushdefault();

    void setConfigpushdefault(String configpushdefault);

    /**
     * @return Defines the upstream branch for vcsbranch.
     * <p/>
     * Git configuration branch.<queryParameterName>.merge makes sense only for
     * <p/>
     * vcsbranch because in this task we need push only in vcsbranch.
     * <p/>
     * In gerrit case it will be:"refs/for/" + queryParameterName branch for which this changes are proposed
     * <p/>
     * vcsbranch should be not null.
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "configbranchmerge")
    String getConfigbranchmerge();

    void setConfigbranchmerge(String configbranchmerge);
}
