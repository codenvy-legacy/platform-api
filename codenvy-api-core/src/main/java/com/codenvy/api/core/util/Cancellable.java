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
package com.codenvy.api.core.util;

/**
 * Implementation of this interface may be used with {@link Watchdog} to make possible terminate task by timeout.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
public interface Cancellable {
    /**
     * Attempts to cancel execution of this {@code Cancellable}.
     *
     * @throws Exception
     *         if cancellation is failed
     */
    void cancel() throws Exception;
}
