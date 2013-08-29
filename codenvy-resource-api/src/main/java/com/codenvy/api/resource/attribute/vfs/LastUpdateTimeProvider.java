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
import com.codenvy.api.resource.attribute.Attributes;

import org.exoplatform.ide.vfs.shared.Item;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
public class LastUpdateTimeProvider extends AttributeProvider<Long> {

    public LastUpdateTimeProvider() {
        super(Attributes.LAST_UPDATE_TIME, "vfs:lastUpdateTime");
    }

    @Override
    public Attribute<Long> getAttribute(Item item) {
        long lastUpdateTime;
        try {
            lastUpdateTime = Long.parseLong(item.getPropertyValue(getVfsPropertyName()));
        } catch (NumberFormatException e) {
            lastUpdateTime = 0;
        }
        return new Attribute<>(getName(), "Last update time", false, true, lastUpdateTime);
    }
}
