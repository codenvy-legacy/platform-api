package com.codenvy.api.project.server;

import com.codenvy.api.project.shared.Attribute;
import com.codenvy.api.project.shared.ProjectDescription;
import com.codenvy.api.project.shared.ProjectDescriptionConverter;
import com.codenvy.api.project.shared.ProjectType;
import com.codenvy.api.project.shared.dto.AttributeDTO;
import com.codenvy.api.project.shared.dto.ProjectDescriptor;
import com.codenvy.dto.server.DtoFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/** @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a> */
public class ProjectDescriptionConverterImpl extends ProjectDescriptionConverter {
    @Override
    public ProjectDescriptor toDescriptor(ProjectDescription description) {
        final List<AttributeDTO> flatten = new ArrayList<>();
        final LinkedList<Attribute> q = new LinkedList<>(description.getAttributes());
        while (!q.isEmpty()) {
            final Attribute current = q.pop();
            flatten.add(DtoFactory.getInstance().createDto(AttributeDTO.class)
                                  .withName(current.getFullName())
                                  .withValue(current.getValues()));
            for (Attribute child : current.getChildren()) {
                q.add(child);
            }
        }
        final ProjectType projectType = description.getProjectType();
        return DtoFactory.getInstance().createDto(ProjectDescriptor.class)
                         .withProjectTypeId(projectType.getId())
                         .withProjectTypeName(projectType.getName())
                         .withAttributes(flatten);
    }
}
