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
package com.codenvy.api.core.util;

/**
 * Cancellable wrapper of {@code Process}.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
public final class CancellableProcessWrapper implements Cancellable {
    private final Process process;

    public CancellableProcessWrapper(Process process) {
        this.process = process;
    }

    @Override
    public void cancel() {
        ProcessUtil.kill(process);
    }
}
