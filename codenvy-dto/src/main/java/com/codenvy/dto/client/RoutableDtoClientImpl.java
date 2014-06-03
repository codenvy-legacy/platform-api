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
// Copyright 2012 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.codenvy.dto.client;

import com.codenvy.dto.shared.RoutableDto;

/** Client side base class for all DTO payload implementations. */
public abstract class RoutableDtoClientImpl /*extends Jso TODO: check*/ implements RoutableDto {

    // To work around devmode bug where static field references on the interface
    // implemented by this SingleJsoImpl, blow up.
    @SuppressWarnings("unused")
    private static final String TYPE_FIELD = RoutableDto.TYPE_FIELD;

    @SuppressWarnings("unused")
    private static final int NON_ROUTABLE_TYPE = RoutableDto.NON_ROUTABLE_TYPE;

    protected RoutableDtoClientImpl() {
    }

    /** @return the type of the JsonMessage so the client knows how to route it. */
    public final native int getType() /*-{
        return this[@com.codenvy.dto.generator.client.RoutableDtoClientImpl::TYPE_FIELD] ||
            @com.codenvy.dto.generator.client.RoutableDtoClientImpl::NON_ROUTABLE_TYPE;
    }-*/;
}