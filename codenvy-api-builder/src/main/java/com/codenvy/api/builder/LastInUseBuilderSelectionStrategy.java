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
package com.codenvy.api.builder;

import javax.inject.Singleton;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Implementation of BuilderSelectionStrategy that selects most recent used builder.
 *
 * @author andrew00x
 */
@Singleton
public class LastInUseBuilderSelectionStrategy implements BuilderSelectionStrategy, Comparator<RemoteBuilder> {
    @Override
    public RemoteBuilder select(List<RemoteBuilder> remoteBuilders) {
        if (remoteBuilders == null || remoteBuilders.isEmpty()) {
            throw new IllegalArgumentException("empty or null list");
        }
        Collections.sort(remoteBuilders, this);
        return remoteBuilders.get(0);
    }

    @Override
    public int compare(RemoteBuilder o1, RemoteBuilder o2) {
        final long time1 = o1.getLastUsageTime();
        final long time2 = o2.getLastUsageTime();
        if (time1 < time2) {
            return 1;
        }
        if (time1 > time2) {
            return -1;
        }
        return 0;
    }
}
