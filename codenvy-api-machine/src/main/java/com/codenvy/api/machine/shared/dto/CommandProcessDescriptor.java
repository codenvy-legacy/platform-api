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
package com.codenvy.api.machine.shared.dto;

import com.codenvy.api.core.rest.shared.dto.Hyperlinks;
import com.codenvy.api.core.rest.shared.dto.Link;

import java.util.List;

/**
 * Describe application process inside of machine
 *
 * @author Alexander Garagatyi
 */
public interface CommandProcessDescriptor extends Hyperlinks {
    int getId();

    void setId(String id);

    CommandProcessDescriptor withId(int id);

    String getCommandLine();

    void setCommandLine(String commandLine);

    CommandProcessDescriptor withCommandLine(String commandLine);

    @Override
    CommandProcessDescriptor withLinks(List<Link> links);
}
