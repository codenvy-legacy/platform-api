package com.codenvy.api.project.shared;

import com.codenvy.api.vfs.shared.dto.Project;

/**
 * Factory for {@link AttributeValueProvider}.
 *
 * @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a>
 */
public interface AttributeValueProviderFactory {
    /** Check is this factory is applicable for specified project type. */
    boolean isApplicable(ProjectType type);

    /** Name of Attribute for which this factory may produce AttributeValueProvider. */
    String getName();

    /** Create new instance of AttributeValueProvider. Project is used for access to low-level information about project. */
    AttributeValueProvider newInstance(Project project);
}
