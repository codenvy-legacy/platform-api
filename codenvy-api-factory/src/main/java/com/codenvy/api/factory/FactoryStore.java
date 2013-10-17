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
package com.codenvy.api.factory;

import java.util.Set;

/** Interface for CRUD operations with factory data. */
public interface FactoryStore {
    /**
     * Save factory at storage.
     *
     * @param factoryUrl
     *         - factory information
     * @param images
     *         - factory images
     * @return - if of stored factory
     * @throws FactoryUrlException
     */
    public String saveFactory(AdvancedFactoryUrl factoryUrl, Set<FactoryImage> images) throws FactoryUrlException;

    /**
     * Remove factory by id
     *
     * @param id
     *         - id of factory to remove
     * @throws FactoryUrlException
     */
    public void removeFactory(String id) throws FactoryUrlException;

    /**
     * Retrieve factory data by its id
     *
     * @param id
     *         - factory id
     * @return - {@code AdvancedFactoryUrl} if factory exist and found, null otherwise
     * @throws FactoryUrlException
     */
    public AdvancedFactoryUrl getFactory(String id) throws FactoryUrlException;

    /**
     * Retrieve factory images by factory id
     *
     * @param factoryId
     *         - factory id
     * @param imageId
     *         - id of the requested image. When null, all images for given factory will be returned.
     * @return - {@code Set} of images if factory found, null otherwise
     * @throws FactoryUrlException
     */
    public Set<FactoryImage> getFactoryImages(String factoryId, String imageId) throws FactoryUrlException;
}
