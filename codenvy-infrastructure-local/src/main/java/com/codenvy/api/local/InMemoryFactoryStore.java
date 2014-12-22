/*******************************************************************************
 * Copyright (c) 2012-2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.api.local;

import com.codenvy.api.core.ApiException;
import com.codenvy.api.core.ConflictException;
import com.codenvy.api.factory.FactoryImage;
import com.codenvy.api.factory.FactoryStore;
import com.codenvy.api.factory.dto.Factory;
import com.codenvy.commons.lang.NameGenerator;
import com.codenvy.commons.lang.Pair;
import com.codenvy.dto.server.DtoFactory;

import javax.inject.Singleton;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Vladyslav Zhukovskii
 */
@Singleton
public class InMemoryFactoryStore implements FactoryStore {
    private final Map<String, Set<FactoryImage>> images    = new HashMap<>();
    private final Map<String, Factory>           factories = new HashMap<>();
    private final ReentrantReadWriteLock         lock      = new ReentrantReadWriteLock();

    @Override
    public String saveFactory(Factory factoryUrl, Set<FactoryImage> images) throws ApiException {
        lock.writeLock().lock();
        try {
            final Factory newFactoryUrl = DtoFactory.getInstance().clone(factoryUrl);
            newFactoryUrl.setId(NameGenerator.generate("", 16));
            final Set<FactoryImage> newImages = new LinkedHashSet<>(images.size());
            for (FactoryImage image : images) {
                newImages.add(new FactoryImage(Arrays.copyOf(image.getImageData(), image.getImageData().length), image.getMediaType(),
                                               image.getName()));
            }

            factories.put(newFactoryUrl.getId(), newFactoryUrl);
            this.images.put(newFactoryUrl.getId(), newImages);

            return newFactoryUrl.getId();
        } catch (IOException e) {
            throw new ConflictException(e.getLocalizedMessage());
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void removeFactory(String id) throws ApiException {
        lock.writeLock().lock();
        try {
            factories.remove(id);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Factory getFactory(String id) throws ApiException {
        lock.readLock().lock();
        try {
            return factories.get(id);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<FactoryImage> getFactoryImages(String factoryId, String imageId) throws ApiException {
        lock.readLock().lock();
        try {
            if (imageId == null) {
                return images.get(factoryId);
            }
            for (FactoryImage image : images.get(factoryId)) {
                if (image.getName().equals(imageId)) {
                    FactoryImage imageCopy;
                    try {
                        imageCopy = new FactoryImage(Arrays.copyOf(image.getImageData(), image.getImageData().length), image.getMediaType(),
                                                     image.getName());
                    } catch (IOException e) {
                        // Seems that error may happen just if media type isn't supported but since we just create copy of existed FactoryImage
                        // such error isn't excepted here.
                        throw new ConflictException(e.getMessage());
                    }
                    return Collections.singleton(imageCopy);
                }
            }
            return Collections.emptySet();
        } finally {
            lock.readLock().unlock();
        }
    }
}
