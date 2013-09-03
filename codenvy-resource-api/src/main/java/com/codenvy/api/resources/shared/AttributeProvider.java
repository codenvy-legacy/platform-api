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
package com.codenvy.api.resources.shared;

import com.codenvy.api.vfs.shared.Item;

/**
 * Mapping between {@link com.codenvy.api.vfs.shared.Property Property} of Virtual File System and {@link Attribute}. Implementation may
 * obtain {@link com.codenvy.api.vfs.shared.Property Property} from {@link Item} and represent it as {@link Attribute} or calculate value
 * of {@link Attribute} in some way.
 *
 * @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a>
 */
public abstract class AttributeProvider<T> {
    private final String name;
    private final String vfsPropertyName;

    protected AttributeProvider(String name, String vfsPropertyName) {
        this.name = name;
        this.vfsPropertyName = vfsPropertyName;
    }

    public final String getName() {
        return name;
    }

    public final String getVfsPropertyName() {
        return vfsPropertyName;
    }

    public abstract Attribute<T> getAttribute(Item item);
}
