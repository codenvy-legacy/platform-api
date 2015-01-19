package com.codenvy.api.machine.shared.dto;

/**
 * Describe application process inside of machine
 *
 * @author Alexander Garagatyi
 */
public interface ApplicationProcessDescriptor {
    long getId();

    void setId(String id);

    ApplicationProcessDescriptor withId(long id);

}
