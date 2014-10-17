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
package com.codenvy.api.runner;

import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.runner.dto.RunnerDescriptor;
import com.codenvy.dto.server.DtoFactory;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

import java.util.Collections;
import java.util.List;


public class RandomRunnerSelectionStrategyTest {

    RemoteRunner runner1 = new RemoteRunner("url1", DtoFactory.getInstance().createDto(RunnerDescriptor.class).withName("java/web")
                                                              .withDescription("test description"), Collections.<Link>emptyList());

    RemoteRunner runner2 = new RemoteRunner("url2", DtoFactory.getInstance().createDto(RunnerDescriptor.class).withName("java/web2")
                                                              .withDescription("test description"), Collections.<Link>emptyList());

    RemoteRunner runner3 = new RemoteRunner("url3", DtoFactory.getInstance().createDto(RunnerDescriptor.class).withName("java/web3")
                                                              .withDescription("test description"), Collections.<Link>emptyList());

    @Test
    public void shouldSelectFromOneElementInList() {
        //given
        RandomRunnerSelectionStrategy strategy = new RandomRunnerSelectionStrategy();
        //when
        RemoteRunner actual = strategy.select(Lists.newArrayList(runner1));
        //then
        Assert.assertEquals(actual, runner1);
    }

    @Test
    public void shouldSelectFromTwoElementInList() {
        //given
        RandomRunnerSelectionStrategy strategy = new RandomRunnerSelectionStrategy();
        List<RemoteRunner> remoteRunners = Lists.newArrayList(runner1, runner2);
        //when

        RemoteRunner actual = strategy.select(remoteRunners);
        //then
        Assert.assertTrue(remoteRunners.contains(actual));
    }


    @Test
    public void shouldSelectFromThree() {
        //given
        RandomRunnerSelectionStrategy strategy = new RandomRunnerSelectionStrategy();
        List<RemoteRunner> remoteRunners = Lists.newArrayList(runner1, runner2, runner3);
        //when
        int runner1Select = 0;
        int runner2Select = 0;
        int runner3Select = 0;
        for (int i = 0; i < 1000; i++) {
            RemoteRunner actual = strategy.select(remoteRunners);
            if (actual == runner1) {
                runner1Select++;
            } else if (actual == runner2) {
                runner2Select++;
            } else if (actual == runner3) {
                runner3Select++;
            }


        }


        //then
        Assert.assertTrue(runner1Select > 250);
        Assert.assertTrue(runner2Select > 250);
        Assert.assertTrue(runner3Select > 250);
        Assert.assertEquals(runner1Select + runner2Select + runner3Select, 1000);
    }
}