/*
 * CODENVY CONFIDENTIAL
 * __________________
 * 
 *  [2012] - [2013] Codenvy, S.A. 
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.deploy;

import com.codenvy.api.builder.BuildQueue;
import com.codenvy.api.builder.BuilderService;
import com.codenvy.api.core.Lifecycle;
import com.codenvy.inject.DynaModule;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

/** @author andrew00x */
@DynaModule
public class TestModule implements Module {
    private static final InjectionListener<Lifecycle> START_LISTENER = new InjectionListener<Lifecycle>() {
        public void afterInjection(Lifecycle lifecycle) {
            lifecycle.start();
        }
    };

    private static class Starter implements TypeListener {
        @Override
        @SuppressWarnings("unchecked")
        public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
            if (Lifecycle.class.isAssignableFrom(type.getRawType())) {
                ((TypeEncounter<Lifecycle>)encounter).register(START_LISTENER);
            }
        }
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(BuilderService.class);
        binder.bind(BuildQueue.class);
        binder.bindListener(new AbstractMatcher<TypeLiteral<?>>() {
            public boolean matches(TypeLiteral<?> typeLiteral) {
                return Lifecycle.class.isAssignableFrom(typeLiteral.getRawType());
            }
        }, new Starter());
    }
}
