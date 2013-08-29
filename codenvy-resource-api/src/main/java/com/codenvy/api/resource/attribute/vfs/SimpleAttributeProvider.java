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
package com.codenvy.api.resource.attribute.vfs;

import com.codenvy.api.resource.attribute.Attribute;
import com.codenvy.api.resource.attribute.AttributeProvider;

import com.codenvy.api.vfs.shared.Item;

/**
 * Maps property of virtual file system item to attribute.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
public final class SimpleAttributeProvider extends AttributeProvider<String> {
    public SimpleAttributeProvider(String name) {
        super(name, name);
    }

    @Override
    public Attribute<String> getAttribute(Item item) {
        return new Attribute<>(getName(), "", false, true, item.getPropertyValue(getVfsPropertyName()));
    }
}
