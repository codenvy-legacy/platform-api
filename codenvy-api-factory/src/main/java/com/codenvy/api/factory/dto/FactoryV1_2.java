package com.codenvy.api.factory.dto;

import com.codenvy.api.factory.parameter.FactoryParameter;
import com.codenvy.dto.shared.DTO;

import static com.codenvy.api.factory.parameter.FactoryParameter.Obligation.OPTIONAL;
import static com.codenvy.api.factory.parameter.FactoryParameter.Version.V1_2;

/**
 * @author Sergii Kabashniuk
 */
@DTO
public interface FactoryV1_2 extends FactoryV1_1 {
    /**
     * @return additinal git configuration
     */
    @FactoryParameter(obligation = OPTIONAL, name = "git")
    Git getGit();

    void setGit(Git git);

    /**
     * @return Factory acceptance restrictions
     */
    @FactoryParameter(obligation = OPTIONAL, name = "restriction", trackedOnly = true)
    Restriction getRestriction();

    void setRestriction(Restriction restriction);

    /**
     * @return path to the image
     */
    @FactoryParameter(obligation = OPTIONAL, name = "image", deprecatedSince = V1_2)
    @Deprecated
    String getImage();

    @Deprecated
    void setImage(String image);
}
