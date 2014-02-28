package com.codenvy.api.factory.dto;

import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.factory.Compatibility;
import com.codenvy.dto.shared.DTO;

import java.util.List;

import static com.codenvy.api.factory.Compatibility.Optionality.OPTIONAL;

/**
 * @author Sergii Kabashniuk
 */
@DTO
public interface FactoryV1_2 extends FactoryV1_1 {
    /**
     * @return additinal git configuration
     */
    @Compatibility(optionality = OPTIONAL)
    Git getGit();

    void setGit(Git git);

    /**
     * @return Factory acceptance restrictions
     */
    @Compatibility(optionality = OPTIONAL, trackedOnly = true)
    Restriction getRestriction();

    void setRestriction(Restriction restriction);

    /**
     * @return path to the image
     */
    @Deprecated
    String getImage();

    @Deprecated
    void setImage(String image);

    @Override
    FactoryV1_2 withLinks(List<Link> links);
}
