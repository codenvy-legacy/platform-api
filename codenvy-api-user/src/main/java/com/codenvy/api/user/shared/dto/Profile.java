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
package com.codenvy.api.user.shared.dto;

import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.dto.shared.DTO;

import java.util.List;
import java.util.Map;

/**
 * @author andrew00x
 */
@DTO
public interface Profile {

    void setId(String id);

    String getId();

    Profile withId(String id);

    String getUserId();

    void setUserId(String id);

    Profile withUserId(String id);

    List<Attribute> getAttributes();

    void setAttributes(List<Attribute> attributes);

    Profile withAttributes(List<Attribute> attributes);

    List<Link> getLinks();

    void setLinks(List<Link> links);

    Profile withLinks(List<Link> links);

    Map<String, String> getPreferences();

    void setPreferences(Map<String, String> prefs);

    Profile withPreferences(Map<String, String> prefs);
}
