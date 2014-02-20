package com.codenvy.api.factory.dto;

import com.codenvy.dto.shared.DTO;

/**
 * @author Sergii Kabashniuk
 */
@DTO
public interface Git {
    /**
     * @return Allows get ref-specs of the changes of remote repository
     */
    String getConfigremoteoriginfetch();

    void setConfigremoteoriginfetch(String configremoteoriginfetch);

    /**
     * See https://www.kernel.org/pub/software/scm/git/docs/git-config.html push.default
     *
     * @return
     */
    String getConfigpushdefault();

    void setConfigpushdefault(String configpushdefault);

    /**
     * @return Defines the upstream branch for vcsbranch.
     * <p/>
     * Git configuration branch.<name>.merge makes sense only for
     * <p/>
     * vcsbranch because in this task we need push only in vcsbranch.
     * <p/>
     * In gerrit case it will be:"refs/for/" + name branch for which this changes are proposed
     * <p/>
     * vcsbranch should be not null.
     */
    String getConfigbranchdestmerge();

    void setConfigbranchdestmerge(String configbranchdestmerge);
}
