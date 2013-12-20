package com.codenvy.api.project.server;

import com.codenvy.api.project.shared.ValueProvider;
import com.codenvy.api.vfs.shared.dto.Project;

/**
 * Factory for {@link com.codenvy.api.project.shared.ValueProvider}.
 *
 * @author andrew00x
 */
public interface ValueProviderFactory {
    /** Name of Attribute for which this factory may produce ValueProvider. */
    String getName();

    /** Create new instance of ValueProvider. Project is used for access to low-level information about project. */
    ValueProvider newInstance(Project project);
}
