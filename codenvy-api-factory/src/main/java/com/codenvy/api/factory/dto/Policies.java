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
package com.codenvy.api.factory.dto;

import com.codenvy.dto.shared.DTO;

/**
 * @author andrew00x
 */
@DTO
public interface Policies {
    String getAuthor();

    void setAuthor(String author);

    Policies withAuthor(String author);

    String getEmail();

    void setEmail(String email);

    Policies withEmail(String email);

    String getRefererhostname();

    void setRefererhostname(String refererhostname);

    Policies withRefererhostname(String refererhostname);

    long getValidsince();

    void setValidsince(long validsince);

    Policies withValidsince(long validsince);

    long getValiduntil();

    void setValiduntil(long validuntil);

    Policies withValiduntil(long validuntil);
}
