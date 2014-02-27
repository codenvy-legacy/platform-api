package com.codenvy.api.factory.dto;

import com.codenvy.dto.shared.DTO;

import java.util.List;

/**
 * ReplacementImpl variable, that contains list of files to find variables and replace by specified values.
 */
@DTO
public interface Variable {
    List<String> getFiles();

    void setFiles(List<String> files);

    List<Replacement> getEntries();

    void setEntries(List<Replacement> entries);
}
