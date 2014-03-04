package com.codenvy.api.factory.dto;

import com.codenvy.api.factory.FactoryParameter;
import com.codenvy.dto.shared.DTO;

import static com.codenvy.api.factory.FactoryParameter.Obligation.OPTIONAL;
import static com.codenvy.api.factory.FactoryParameter.Version.V1_2;

/**
 * @author Sergii Kabashniuk
 */
@DTO
public interface FactoryV1_2 extends FactoryV1_1 {
    /**
     * @return additinal git configuration
     */
    @FactoryParameter(obligation = OPTIONAL)
    Git getGit();

    void setGit(Git git);

    /**
     * @return Factory acceptance restrictions
     */
    @FactoryParameter(obligation = OPTIONAL, trackedOnly = true)
    Restriction getRestriction();

    void setRestriction(Restriction restriction);

    /**
     * @return path to the image
     */
    @FactoryParameter(obligation = OPTIONAL, deprecatedSince = V1_2)
    @Deprecated
    String getImage();

    @Deprecated
    void setImage(String image);
}
