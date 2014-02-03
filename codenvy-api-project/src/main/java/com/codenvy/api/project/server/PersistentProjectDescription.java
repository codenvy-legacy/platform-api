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

import com.codenvy.api.project.shared.Attribute;
import com.codenvy.api.project.shared.ProjectDescription;
import com.codenvy.api.project.shared.ProjectType;
import com.codenvy.api.project.shared.ValueProvider;
import com.codenvy.api.project.shared.dto.ProjectDescriptor;
import com.codenvy.api.vfs.server.VirtualFileSystem;
import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;
import com.codenvy.api.vfs.shared.dto.Project;
import com.codenvy.dto.server.DtoFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** @author andrew00x */
public class PersistentProjectDescription extends ProjectDescription {
    PersistentProjectDescription(ProjectType projectType, Attribute... attributes) {
        super(projectType, attributes);
    }

    PersistentProjectDescription(ProjectType projectType, List<Attribute> attributes) {
        super(projectType, attributes);
    }

    public void update(ProjectDescriptor descriptor) {
        setProjectType(new ProjectType(descriptor.getProjectTypeId(), descriptor.getProjectTypeName()));
        for (Map.Entry<String, List<String>> entry : descriptor.getAttributes().entrySet()) {
            final String name = entry.getKey();
            final List<String> value = entry.getValue();
            final Attribute attribute = getAttribute(name);
            if (attribute != null) {
                attribute.setValues(value);
            } else {
                // If attribute doesn't exist then treat this attribute as one that may be stored in project properties on
                // Virtual Filesystem. If some attribute needs something special then it must be added in ProjectTypeDescription
                // and must have corresponded ValueProvider for handling value of attribute.
                setAttribute(new Attribute(name, new VfsPropertyValueProvider(name, value)));
            }
        }
    }

    @Override
    public void setProjectType(ProjectType projectType) {
        Attribute attribute = getAttribute("vfs:projectType");
        if (attribute == null) {
            attribute = new Attribute("vfs:projectType", new VfsPropertyValueProvider("vfs:projectType"));
        }
        attribute.setValue(projectType.getId());
        super.setProjectType(projectType);
    }

    public ProjectDescriptor getDescriptor() {
        final ProjectType projectType = getProjectType();
        final Map<String, List<String>> attributeValues = new HashMap<>();
        for (Attribute attribute : getAttributes()) {
            attributeValues.put(attribute.getName(), attribute.getValues());
        }
        return DtoFactory.getInstance().createDto(ProjectDescriptor.class)
                         .withProjectTypeId(projectType.getId())
                         .withProjectTypeName(projectType.getName())
                         .withAttributes(attributeValues);
    }

    public void store(Project project, VirtualFileSystem vfs) throws VirtualFileSystemException {
        for (Attribute attribute : getAttributes()) {
            final ValueProvider valueProvider = attribute.getValueProvider();
            if (valueProvider instanceof PersistentValueProvider) {
                ((PersistentValueProvider)valueProvider).store(project, vfs);
            }
        }
    }
}
