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

import com.codenvy.api.core.factory.FactoryParameter;
import com.codenvy.dto.shared.DTO;

import java.util.List;

import static com.codenvy.api.core.factory.FactoryParameter.FactoryFormat.ENCODED;
import static com.codenvy.api.core.factory.FactoryParameter.Obligation.OPTIONAL;

/**
 * Describes actions that should be done after loading of the IDE
 *
 * @author andrew00x
 * @author Alexander Garagatyi
 */
@DTO
public interface Actions {
    /**
     * Welcome page configuration.
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "welcome", format = ENCODED, trackedOnly = true)
    WelcomePage getWelcome();

    void setWelcome(WelcomePage welcome);

    Actions withWelcome(WelcomePage welcome);

    /**
     * Allow to use text replacement in project files after clone
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "findReplace")
    List<Variable> getFindReplace();

    void setFindReplace(List<Variable> variable);

    Actions withFindReplace(List<Variable> variable);

    // TODO
//    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "macro")
    String getMacro();

    void setMacro(String macro);

    Actions withMacro(String macro);

    /**
     * Path of the file to open in the project.
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "openFile")
    String getOpenFile();

    void setOpenFile(String openFile);

    Actions withOpenFile(String openFile);

    /**
     * Warn on leave page
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "warnOnClose")
    boolean getWarnOnClose();

    void setWarnOnClose(boolean warnOnClose);

    Actions withWarnOnClose(boolean warnOnClose);

    // TODO add plugins section
//    Plugins getPlugins();
//
//    void setPlugins(Plugins plugins);
//
//    Actions withPlugins(Plugins plugins);
}
