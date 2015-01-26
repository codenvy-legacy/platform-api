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

/**
 * Factory for builders of different types of machines
 *
 * @author Alexander Garagatyi
 */
public interface MachineBuilderFactory {
    /** Returns new machine builder. */
    MachineBuilder newMachineBuilder();

    /** Returns type of machine builder that this factory produces. */
    String getMachineBuilderType();
}
