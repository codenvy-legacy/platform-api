package com.codenvy.api.factory.dto;

import com.codenvy.dto.shared.DTO;

/**
 * @author Sergii Kabashniuk
 */
@DTO
public interface FactoryV1_2 extends FactoryV1_1 {
    /**
     * @return additinal git configuration
     */
    Git getGit();

    void setGit(Git git);

    /**
     * @return Factory acceptance restrictions
     */
    Restriction getRestriction();

    void setRestriction(Restriction restriction);

    /**
     * @return path to the image
     */
    @Deprecated
    String getImage();

    @Deprecated
    void setImage(String image);


}
