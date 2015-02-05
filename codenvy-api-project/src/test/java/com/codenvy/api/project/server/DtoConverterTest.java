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
package com.codenvy.api.project.server;

import com.codenvy.api.project.shared.dto.BuilderConfiguration;
import com.codenvy.api.project.shared.dto.BuildersDescriptor;
import com.codenvy.commons.lang.NameGenerator;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * @author Vitaly Parfonov
 */
public class DtoConverterTest {


    @Test
    public void buildersFromDtoBuildersDescriptor() {
        //prepare
        String optionsKey = NameGenerator.generate("optionsKey",5);
        String optionsValue = NameGenerator.generate("optionsValue",5);
        String optionsKey1 = NameGenerator.generate("optionsKey",5);
        String optionsValue1 = NameGenerator.generate("optionsValue",5);
        String optionsKey2 = NameGenerator.generate("optionsKey",5);
        String optionsValue2 = NameGenerator.generate("optionsValue",5);
        Map<String, String> options = new HashMap<>(3);
        options.put(optionsKey,optionsValue);
        options.put(optionsKey1,optionsValue1);
        options.put(optionsKey2,optionsValue2);

        String target1 = NameGenerator.generate("target",5);
        String target2 = NameGenerator.generate("target",5);
        List<String> targets = new ArrayList<>(2);
        targets.add(target1);
        targets.add(target2);

        BuilderConfiguration builderConfiguration = mock(BuilderConfiguration.class);
        when(builderConfiguration.getOptions()).thenReturn(options);
        when(builderConfiguration.getTargets()).thenReturn(targets);

        Map<String, BuilderConfiguration> configurationMap =  new HashMap<>();
        String confName = NameGenerator.generate("conf",5);
        configurationMap.put(confName, builderConfiguration);

        String defaultBuilder  = NameGenerator.generate("builder",5);
        BuildersDescriptor buildersDescriptor = mock(BuildersDescriptor.class);
        when(buildersDescriptor.getConfigs()).thenReturn(configurationMap);
        when(buildersDescriptor.getDefault()).thenReturn(defaultBuilder);

        //check
        Builders builders = DtoConverter.fromDto(buildersDescriptor);
        Assert.assertNotNull(builders);
        Assert.assertEquals(defaultBuilder, builders.getDefault());

        Builders.Config config = builders.getConfig(confName);
        Assert.assertNotNull(config);
        Assert.assertNotNull(config.getTargets());
        Assert.assertEquals(2, config.getTargets().size());
        Assert.assertTrue(config.getTargets().contains(target1));
        Assert.assertTrue(config.getTargets().contains(target2));

        Assert.assertNotNull(config.getOptions());
        Assert.assertEquals(3, config.getOptions().size());
        Assert.assertTrue(config.getOptions().containsKey(optionsKey));
        Assert.assertTrue(config.getOptions().containsKey(optionsKey1));
        Assert.assertTrue(config.getOptions().containsKey(optionsKey2));

        Assert.assertTrue(config.getOptions().containsValue(optionsValue));
        Assert.assertTrue(config.getOptions().containsValue(optionsValue1));
        Assert.assertTrue(config.getOptions().containsValue(optionsValue2));
    }
}
