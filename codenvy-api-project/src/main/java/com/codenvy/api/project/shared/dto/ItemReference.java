package com.codenvy.api.project.shared.dto;

import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.dto.shared.DTO;

import java.util.List;

/**
 * @author andrew00x
 */
@DTO
public interface ItemReference {
    String getName();

    void setName(String name);

    ItemReference withName(String name);

    List<Link> getLinks();

    ItemReference withLinks(List<Link> links);

    void setLinks(List<Link> links);
}
