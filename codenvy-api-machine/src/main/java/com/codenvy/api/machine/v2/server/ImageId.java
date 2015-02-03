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
package com.codenvy.api.machine.v2.server;

import java.util.Map;

/**
 * @author andrew00x
 */
public interface ImageId { // Saved in DAO. Top level component finds it in DAO and uses to restore Image with ImageFactory
    Map<String, String> getValues();

    void setValues(Map<String, String> values);

    ImageId withValues(Map<String, String> values);
}
