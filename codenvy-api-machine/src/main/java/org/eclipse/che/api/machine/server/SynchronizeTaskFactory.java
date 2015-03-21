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
package org.eclipse.che.api.machine.server;

import com.google.inject.assistedinject.Assisted;

import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.machine.shared.ProjectBinding;

/**
 * @author Alexander Garagatyi
 */
public interface SynchronizeTaskFactory {
    SynchronizeTask create(@Assisted("workspaceId") String workspaceId,
                           ProjectBinding projectBinding,
                           @Assisted("machineProjectFolder") String machineProjectFolder) throws ServerException;
}
