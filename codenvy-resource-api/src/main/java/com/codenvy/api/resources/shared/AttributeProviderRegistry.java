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

import java.util.List;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
public interface AttributeProviderRegistry {
    /**
     * Register attribute provider for specified {@code itemType}. Expected item types: FILE, FOLDER, PROJECT.
     *
     * @param itemType
     *         item type: FILE, FOLDER or PROJECT
     * @param attributeProvider
     *         AttributeProvider
     */
    void addAttributeProvider(String itemType, AttributeProvider<?> attributeProvider);

    /**
     * Get names of AttributeProviders registered for specified {@code itemType}. Modifications to the returned {@code List} should not
     * affect the internal state of this object.
     *
     * @param itemType
     *         item type: FILE, FOLDER or PROJECT
     * @return known Attribute Providers
     */
    List<String> getAttributeProviderNames(String itemType);

    AttributeProvider<?> getAttributeProvider(String itemType, String name);
}
