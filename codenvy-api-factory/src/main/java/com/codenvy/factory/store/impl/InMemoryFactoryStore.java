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
package com.codenvy.factory.store.impl;

import com.codenvy.commons.lang.NameGenerator;
import com.codenvy.factory.commons.AdvancedFactoryUrl;
import com.codenvy.factory.commons.FactoryUrlException;
import com.codenvy.factory.commons.Image;
import com.codenvy.factory.store.FactoryStore;
import com.codenvy.factory.store.SavedFactoryData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class InMemoryFactoryStore implements FactoryStore {
    private              Map<String, Image>            images    = new HashMap<>();
    private              Map<String, SavedFactoryData> factories = new HashMap<>();
    private static final ReentrantReadWriteLock        lock      = new ReentrantReadWriteLock();

    @Override
    public SavedFactoryData saveFactory(AdvancedFactoryUrl factoryUrl, Image image) throws FactoryUrlException {
        lock.writeLock().lock();
        try {
            image.setName(NameGenerator.generate("", 16) + image.getName());
            factoryUrl.setId(NameGenerator.generate("", 16));

            Set<Image> factoryImages = new HashSet<>();
            factoryImages.add(image);

            SavedFactoryData factoryData = new SavedFactoryData(factoryUrl, factoryImages);
            factories.put(factoryUrl.getId(), factoryData);
            images.put(image.getName(), image);


            return factoryData;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void removeFactory(String id) throws FactoryUrlException {
        lock.writeLock().lock();
        try {
            SavedFactoryData removedItem = factories.remove(id);
            if (removedItem != null) {
                for (Image image : removedItem.getImages()) {
                    images.remove(image.getName());
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public SavedFactoryData getFactory(String id) throws FactoryUrlException {
        lock.readLock().lock();
        try {
            return factories.get(id);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Image getImage(String id) throws FactoryUrlException {
        lock.readLock().lock();
        try {
            return images.get(id);
        } finally {
            lock.readLock().unlock();
        }
    }
}
