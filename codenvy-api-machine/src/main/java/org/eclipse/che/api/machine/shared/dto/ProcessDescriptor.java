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
package org.eclipse.che.api.machine.shared.dto;

import org.eclipse.che.api.core.rest.shared.dto.Hyperlinks;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.dto.shared.DTO;

import java.util.List;

/**
 * @author andrew00x
 */
@DTO
public interface ProcessDescriptor extends Hyperlinks {
    int getPid();

    void setPid(int pid);

    ProcessDescriptor withPid(int pid);

    String getCommandLine();

    void setCommandLine(String commandLine);

    ProcessDescriptor withCommandLine(String commandLine);

    boolean getIsAlive();

    void setIsAlive(boolean isAlive);

    ProcessDescriptor withIsAlive(boolean isAlive);

    @Override
    ProcessDescriptor withLinks(List<Link> links);
}
