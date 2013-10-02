package com.codenvy.api.project.shared.dto;

import com.codenvy.dto.shared.DTO;

import java.util.Map;

/**
 * Represent attributes of Project.
 *
 * @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a>
 */
@DTO
public interface Attributes {
    Map<String, String> getAttributes();

    void setAttributes(Map<String, String> attributes);
}
