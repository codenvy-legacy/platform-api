package com.codenvy.api.factory.dto;

import com.codenvy.dto.shared.DTO;

import java.util.List;


/** Replacement variable, that contains list of files to find variables and replace by specified values. */
@DTO
public interface Variable {
    /**
     * @return - list of files to make replacement
     */
    List<String> getFiles();

    void setFiles(List<String> files);

    Variable withFiles(List<String> files);

    /**
     * @return - list of replacement
     */
    List<Replacement> getEntries();

    void setEntries(List<Replacement> entries);

    Variable withEntries(List<Replacement> entries);

}
