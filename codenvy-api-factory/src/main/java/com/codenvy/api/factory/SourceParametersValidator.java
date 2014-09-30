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
package com.codenvy.api.factory;

import com.codenvy.api.core.ConflictException;
import com.codenvy.api.core.factory.FactoryParameter;
import com.codenvy.api.project.shared.dto.ImportSourceDescriptor;

import java.util.Map;

import static com.codenvy.api.factory.FactoryConstants.PARAMETRIZED_ILLEGAL_PARAMETER_VALUE_MESSAGE;
import static com.codenvy.api.factory.FactoryConstants.PARAMETRIZED_INVALID_PARAMETER_MESSAGE;
import static java.lang.String.format;

/**
 * @author Alexander Garagatyi
 */
public class SourceParametersValidator implements FactoryParameterValidator<ImportSourceDescriptor> {
    @Override
    public void validate(ImportSourceDescriptor source, FactoryParameter.Version version) throws ConflictException {
        if ("git".equals(source.getType())) {
            for (Map.Entry<String, String> entry : source.getParameters().entrySet()) {
                switch (entry.getKey()) {
                    case "branch":
                        break;
                    case "keepVcs":
                        final String keepVcs = entry.getValue();
                        if (!"true".equals(keepVcs) && !"false".equals(keepVcs)) {
                            throw new ConflictException(
                                    format(PARAMETRIZED_ILLEGAL_PARAMETER_VALUE_MESSAGE, "source.parameters.keepVcs", entry.getValue()));
                        }
                        break;
                    case "commitId":
                        break;
                    case "keepDirectory":
                        break;
                    case "remoteOriginFetch":
                        break;
                    default:
                        throw new ConflictException(format(PARAMETRIZED_INVALID_PARAMETER_MESSAGE, entry.getKey(), version));
                }
            }
        } else {
            throw new ConflictException(format(PARAMETRIZED_ILLEGAL_PARAMETER_VALUE_MESSAGE, "source.type", source.getType()));
        }
    }
}
