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
package com.codenvy.api.factory.parameter;

import com.codenvy.api.factory.FactoryUrlException;

import javax.inject.Singleton;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author Alexander Garagatyi
 */
@Singleton
public class IgnoreConverter {
    public void convert(Method method, Object object) throws FactoryUrlException {
        try {
            Class<?> returnClass = method.getReturnType();
            // TODO improve by checking is it getSmth
            Method setMethod = object.getClass().getMethod("set" + method.getName().substring(3), returnClass);
            if (boolean.class.equals(returnClass)) {
                method.invoke(object, false);
            } else {
                method.invoke(object);
            }

        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new FactoryUrlException(e.getLocalizedMessage(), e);
        }
    }
}
