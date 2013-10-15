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

import com.codenvy.api.core.util.ComponentLoader;
import com.codenvy.api.project.shared.AttributeValueProvider;
import com.codenvy.api.vfs.shared.dto.Project;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for {@link AttributeValueProvider}.
 * <p/>
 * FQN of implementation of this interface must be placed in file META-INF/services/com.codenvy.api.project.server.AttributeValueProviderFactory.
 *
 * @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a>
 */
public abstract class AttributeValueProviderFactory {
    private static final AttributeValueProviderFactory[] EMPTY = new AttributeValueProviderFactory[0];

    private static List<AttributeValueProviderFactory> all = new ArrayList<>(ComponentLoader.all(AttributeValueProviderFactory.class));

    /** All known factories. */
    public static AttributeValueProviderFactory[] getInstances() {
        return all.toArray(EMPTY);
    }

    /** Name of Attribute for which this factory may produce AttributeValueProvider. */
    public abstract String getName();

    public abstract AttributeValueProvider newInstance(Project project);
}
