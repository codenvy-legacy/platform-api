package com.codenvy.api.project.server;

import com.codenvy.api.project.shared.ProjectType;
import com.codenvy.api.project.shared.ValueProvider;
import com.codenvy.api.vfs.shared.dto.Project;

/**
 * Factory for {@link com.codenvy.api.project.shared.ValueProvider}.
 *
 * @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a>
 */
public interface ValueProviderFactory {
    /** Check is this factory is applicable for specified project type. */
    boolean isApplicable(ProjectType type);

    /** Name of Attribute for which this factory may produce ValueProvider. */
    String getName();

    /** Create new instance of ValueProvider. Project is used for access to low-level information about project. */
    ValueProvider newInstance(Project project);
}
