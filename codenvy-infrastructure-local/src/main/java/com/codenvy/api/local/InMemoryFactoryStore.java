/*******************************************************************************
* Copyright (c) 2012-2014 Codenvy, S.A.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
* Codenvy, S.A. - initial API and implementation
*******************************************************************************/
package com.codenvy.api.local;

import com.codenvy.api.core.util.Pair;
import com.codenvy.api.factory.FactoryImage;
import com.codenvy.api.factory.FactoryStore;
import com.codenvy.api.factory.FactoryUrlException;
import com.codenvy.api.factory.dto.Factory;
import com.codenvy.commons.lang.NameGenerator;
import com.codenvy.dto.server.DtoFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Vladyslav Zhukovskii
 */
public class InMemoryFactoryStore implements FactoryStore {
    private              Map<String, Set<FactoryImage>> images    = new HashMap<>();
    private              Map<String, Factory>           factories = new HashMap<>();
    private static final ReentrantReadWriteLock         lock      = new ReentrantReadWriteLock();

    @Override
    public String saveFactory(Factory factoryUrl, Set<FactoryImage> images) throws FactoryUrlException {
        lock.writeLock().lock();
        try {
            Factory newFactoryUrl = DtoFactory.getInstance().clone(factoryUrl);
            newFactoryUrl.setId(NameGenerator.generate("", 16));
            Set<FactoryImage> newImages = new HashSet<>();
            for (FactoryImage image : images) {
                FactoryImage newImage =
                        new FactoryImage(Arrays.copyOf(image.getImageData(), image.getImageData().length), image.getMediaType(),
                                         image.getName());
                newImages.add(newImage);
            }

            factories.put(newFactoryUrl.getId(), newFactoryUrl);
            this.images.put(newFactoryUrl.getId(), newImages);

            return newFactoryUrl.getId();
        } catch (IOException e) {
            throw new FactoryUrlException(e.getLocalizedMessage(), e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void removeFactory(String id) throws FactoryUrlException {
        lock.writeLock().lock();
        try {
            factories.remove(id);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Factory getFactory(String id) throws FactoryUrlException {
        lock.readLock().lock();
        try {
            return factories.get(id);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<Factory> findByAttribute(Pair<String, String>... attributes) throws FactoryUrlException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Set<FactoryImage> getFactoryImages(String factoryId, String imageId) throws FactoryUrlException {
        lock.readLock().lock();
        try {
            if (imageId == null)
                return images.get(factoryId);
            for (FactoryImage one : images.get(factoryId)) {
                if (one.getName().equals(imageId))
                    return new HashSet<>(java.util.Arrays.asList(one));
            }
            return Collections.emptySet();
        } finally {
            lock.readLock().unlock();
        }
    }
}
