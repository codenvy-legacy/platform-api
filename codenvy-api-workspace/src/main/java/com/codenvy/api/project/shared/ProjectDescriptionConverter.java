package com.codenvy.api.project.shared;

import com.codenvy.api.project.shared.dto.AttributeDTO;
import com.codenvy.api.project.shared.dto.ProjectDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Helps convert {@link ProjectDescription} to {@link com.codenvy.api.project.shared.dto.ProjectDescriptor} and back. Since we use
 * {@link ProjectDescription} on client and server sides we need separate sub-classes of this class for transformation {@link
 * ProjectDescription} to {@link com.codenvy.api.project.shared.dto.ProjectDescriptor}.
 *
 * @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a>
 */
public abstract class ProjectDescriptionConverter {
    private static final Comparator<AttributeDTO> ATTRIBUTE_NAME_COMPARATOR = new Comparator<AttributeDTO>() {
        @Override
        public int compare(AttributeDTO o1, AttributeDTO o2) {
            final char[] name1 = o1.getName().toCharArray();
            final char[] name2 = o2.getName().toCharArray();
            int d1 = 0;
            int d2 = 0;
            for (int i = 0; i < name1.length; i++) {
                if (name1[i] == '.') {
                    d1++;
                }
            }
            for (int i = 0; i < name2.length; i++) {
                if (name2[i] == '.') {
                    d2++;
                }
            }
            return d1 - d2;
        }
    };

    public ProjectDescription fromDescriptor(ProjectDescriptor descriptor) {
        final List<AttributeDTO> attributesDTO = descriptor.getAttributes();
        final Map<String, Attribute> attributes = new LinkedHashMap<>();
        // sorting is important to fill attributes hierarchically.
        Collections.sort(attributesDTO, ATTRIBUTE_NAME_COMPARATOR);
        for (AttributeDTO dto : attributesDTO) {
            final String fullName = dto.getName();
            int i = fullName.indexOf('.');
            if (i > 0) {
                // fullName is hierarchical
                Attribute root = attributes.get(fullName.substring(0, i));
                int j = fullName.lastIndexOf('.');
                if (j > i) {
                    // fullName has more then one item in hierarchy
                    String parentName = fullName.substring(i + 1, j);
                    Attribute parent = root.getChild(parentName);
                    if (parent == null) {
                        // parent doesn't exist yet, create all parent structure
                        int k = 0, l = 0;
                        parent = root;
                        for (; l < parentName.length(); k = l + 1) {
                            l = parentName.indexOf('.', k);
                            if (l < 0) {
                                l = parentName.length();
                            }
                            String elemName = parentName.substring(k, l);
                            Attribute attr = parent.getChild(elemName);
                            if (attr == null) {
                                // if attribute isn't exist in hierarchy create it without value
                                attr = new Attribute(elemName, (String)null);
                                parent.addChild(attr);
                            }
                            parent = attr;
                        }
                    }
                    parent.addChild(new Attribute(fullName.substring(j + 1), dto.getValue()));
                } else {
                    root.addChild(new Attribute(fullName.substring(i + 1), dto.getValue()));
                }
            } else {
                attributes.put(fullName, new Attribute(fullName, dto.getValue()));
            }
        }
        return new ProjectDescription(descriptor.getName(),
                                      new ProjectType(descriptor.getProjectTypeId(), descriptor.getProjectTypeName()),
                                      new ArrayList<>(attributes.values()));
    }

    public abstract ProjectDescriptor toDescriptor(ProjectDescription description);
}
