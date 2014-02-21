package com.codenvy.api.factory.dto;

import com.codenvy.dto.shared.DTO;

/**
 * An object that contain URL for some resource.
 */
@DTO
public interface Link {
    String getType();

    void setType(String type);

    String getHref();

    void setHref(String href);

    String getRel();

    void setRel(String rel);
}
