/*
 * CODENVY CONFIDENTIAL
 * __________________
 * 
 *  [2012] - [2013] Codenvy, S.A. 
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.api.project.server;

import com.codenvy.api.project.shared.DefaultValueProvider;
import com.codenvy.api.vfs.server.VirtualFileSystem;
import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;
import com.codenvy.api.vfs.shared.dto.Project;
import com.codenvy.api.vfs.shared.dto.Property;
import com.codenvy.dto.server.DtoFactory;

import java.util.ArrayList;
import java.util.List;

/** @author andrew00x */
public class VfsPropertyValueProvider extends DefaultValueProvider implements PersistentValueProvider {
    private final String propertyName;

    public VfsPropertyValueProvider(String propertyName, List<String> values) {
        super(values);
        this.propertyName = propertyName;
    }

    public VfsPropertyValueProvider(String propertyName, String... values) {
        super(values);
        this.propertyName = propertyName;
    }

    public VfsPropertyValueProvider(String propertyName, String value) {
        super(value);
        this.propertyName = propertyName;
    }

    public VfsPropertyValueProvider(String propertyName) {
        this.propertyName = propertyName;
    }

    @Override
    public void store(Project project, VirtualFileSystem vfs) throws VirtualFileSystemException {
        final List<Property> properties = new ArrayList<>(1);
        properties.add(DtoFactory.getInstance().createDto(Property.class).withName(propertyName).withValue(getValues()));
        vfs.updateItem(project.getId(), properties, null);
    }
}
