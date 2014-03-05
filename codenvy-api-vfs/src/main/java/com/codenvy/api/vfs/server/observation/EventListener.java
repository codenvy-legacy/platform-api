package com.codenvy.api.vfs.server.observation;

/**
 * @author andrew00x
 */
public interface EventListener {
    void create(CreateEvent event);

    void move(MoveEvent event);

    void rename(RenameEvent event);

    void delete(DeleteEvent event);

    void updateContent(UpdateContentEvent event);

    void updateProperties(UpdatePropertiesEvent event);

    void updateACL(UpdateACLEvent event);
}
