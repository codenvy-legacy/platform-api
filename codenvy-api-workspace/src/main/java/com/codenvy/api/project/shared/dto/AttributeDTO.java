package com.codenvy.api.project.shared.dto;

import com.codenvy.dto.shared.DTO;

import java.util.List;

/**
 * DTO for {@link com.codenvy.api.project.shared.Attribute}
 *
 * @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a>
 */
@DTO
public interface AttributeDTO {
    String getName();

    void setName(String name);

    AttributeDTO withName(String name);

    List<String> getValue();

    void setValue(List<String> value);

    AttributeDTO withValue(List<String> value);
}
