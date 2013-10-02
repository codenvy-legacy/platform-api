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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a> */
public class LastInUseBuilderListSorter implements BuilderListSorter, Comparator<RemoteBuilder> {

    @Override
    public void sort(List<RemoteBuilder> candidates) {
        Collections.sort(candidates, this);
    }

    @Override
    public int compare(RemoteBuilder o1, RemoteBuilder o2) {
        final Long time1 = o1.getLastUsageTime();
        final Long time2 = o2.getLastUsageTime();
        if (time1 < time2) {
            return 1;
        }
        if (time1 > time2) {
            return -1;
        }
        return 0;
    }
}
