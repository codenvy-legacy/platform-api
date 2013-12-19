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
package com.codenvy.api.builder.internal;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Set;

/**
 * Helps register {@link Builder}s on startup.
 *
 * @author andrew00x
 */
@Singleton
public final class BuilderRegistryPlugin {
    @Inject
    public BuilderRegistryPlugin(BuilderRegistry registry, Set<Builder> builders) {
        for (Builder builder : builders) {
            registry.add(builder);
        }
    }
}
