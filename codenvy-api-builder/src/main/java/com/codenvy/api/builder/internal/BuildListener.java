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
package com.codenvy.api.builder.internal;

/**
 * Build listener abstraction. Implementation of this interface may be registered in {@code Builder} with method {@link
 * com.codenvy.api.builder.internal.Builder#addBuildListener(BuildListener)}.
 *
 * @author andrew00x
 * @see com.codenvy.api.builder.internal.Builder#addBuildListener(BuildListener)
 * @see com.codenvy.api.builder.internal.Builder#removeBuildListener(BuildListener)
 */
public interface BuildListener {
    /** Builder invokes this method when build process starts. */
    void begin(BuildTask task);

    /** Builder invokes this method when build process ends. */
    void end(BuildTask task);
}
