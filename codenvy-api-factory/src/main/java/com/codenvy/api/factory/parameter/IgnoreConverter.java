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
    public void convert(Method method, FactoryParameter factoryParameter, Object object) throws FactoryUrlException {
        try {
            Class<?> returnClass = method.getReturnType();
            String setterMethodName =
                    "set" + Character.toUpperCase(factoryParameter.name().charAt(0)) + factoryParameter.name().substring(1);
            Method setMethod = object.getClass().getMethod(setterMethodName, returnClass);
            if (boolean.class.equals(returnClass)) {
                setMethod.invoke(object, false);
            } else if (returnClass.isPrimitive()) {
                setMethod.invoke(object, 0);
            } else {
                Object[] params = {null};
                setMethod.invoke(object, params);
            }

        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new FactoryUrlException(e.getLocalizedMessage(), e);
        }
    }
}
