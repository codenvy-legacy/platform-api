/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.api.machine.server;

import com.codenvy.api.machine.server.spi.ImageKey;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Alexander Garagatyi
 */
public class ImageKeyImpl implements ImageKey {
    private final Map<String, String> fields;

    public ImageKeyImpl(Map<String, String> fields) {
        this.fields = new LinkedHashMap<>(fields);
    }

    @Override
    public Map<String, String> getFields() {
        return Collections.unmodifiableMap(fields);
    }

    @Override
    public String toJson() {
        throw new UnsupportedOperationException();
    }
}
