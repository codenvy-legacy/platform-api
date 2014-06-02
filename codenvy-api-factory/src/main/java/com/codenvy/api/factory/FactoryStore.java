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

import com.codenvy.api.core.util.Pair;
import com.codenvy.api.factory.dto.Factory;

import java.util.List;
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
    public String saveFactory(Factory factoryUrl, Set<FactoryImage> images) throws FactoryUrlException;

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
    public Factory getFactory(String id) throws FactoryUrlException;

    /**
     * Retrieve factory by given attribute name and value.
     *
     * @param  attributes
     *              - attribute pairs to search for
     *
     * @return - List {@code AdvancedFactoryUrl} if factory(s) exist and found, empty list otherwise
     * @throws FactoryUrlException
     */
    public List<Factory> findByAttribute(Pair<String, String>... attributes) throws FactoryUrlException;

    /**
     * Retrieve factory images by factory id
     *
     * @param factoryId
     *         - factory id
     * @param imageId
     *         - id of the requested image. When null, all images for given factory will be returned.
     * @return - {@code Set} of images if factory found, empty set otherwise
     * @throws FactoryUrlException
     */
    public Set<FactoryImage> getFactoryImages(String factoryId, String imageId) throws FactoryUrlException;
}
