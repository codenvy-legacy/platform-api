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
package com.codenvy.api.machine.v2.server.spi;

import java.util.Map;

/**
 * Describes set of attributes that uniquely identifies in implementation specific way.
 *
 * @author andrew00x
 */
public interface ImageId {
    Map<String, String> getAttributes();

    void setAttributes(Map<String, String> attributes);

    ImageId withAttributes(Map<String, String> attributes);

    /** Serializes this {@code ImageId} in JSON format. */
    String toJson();
}
