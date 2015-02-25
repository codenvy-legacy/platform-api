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
package com.codenvy.api.auth.cookie;

import javax.servlet.ServletOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author Sergii Kabashniuk
 */
public class ByteArrayServletStream extends ServletOutputStream {

    ByteArrayOutputStream baos;

    ByteArrayServletStream(ByteArrayOutputStream baos) {
        this.baos = baos;
    }

    public void write(int param) throws IOException {
        baos.write(param);
    }
}