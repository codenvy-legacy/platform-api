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

import java.io.Closeable;
import java.io.IOException;

/**
 * Consumes text line by line for analysing, writing, storing, etc.
 *
 * @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a>
 */
public interface LineConsumer extends Closeable {
    /** Consumes single line. */
    void writeLine(String line) throws IOException;
}
