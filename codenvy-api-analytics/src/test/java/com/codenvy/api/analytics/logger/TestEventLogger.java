/*
 *
 * CODENVY CONFIDENTIAL
 * ________________
 *
 * [2012] - [2014] Codenvy, S.A.
 * All Rights Reserved.
 * NOTICE: All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any. The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.api.analytics.logger;

import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;

/**
 * @author Anatoliy Bazko
 */
public class TestEventLogger {

    private EventLogger eventLogger;

    @BeforeMethod
    public void setUp() throws Exception {
        eventLogger = spy(new EventLogger());
    }

    @Test
    public void shouldNotLogNullEvent() throws UnsupportedEncodingException {
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);

        eventLogger.log(null, null);

        verify(eventLogger, never()).offerEvent(message.capture());
    }

    @Test
    public void shouldNotLogArbitraryEvent() throws UnsupportedEncodingException {
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);

        eventLogger.log("arbitrary-event", null);

        verify(eventLogger, never()).offerEvent(message.capture());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldThrowExceptionIfValidationFailedForValue() throws UnsupportedEncodingException {
        Map<String, String> parameters = new HashMap<String, String>() {{
            put("PARAM", "012345678901234567890123456789012345678901234567891");
        }};

        eventLogger.log(EventLogger.IDE_USAGE, parameters);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldThrowExceptionIfValidationFailedForParam() throws UnsupportedEncodingException {
        Map<String, String> parameters = new HashMap<String, String>() {{
            put("0123456789012345678901234567890123456789", "value");
        }};

        eventLogger.log(EventLogger.IDE_USAGE, parameters);
    }

    @Test
    public void shouldLogEventWithoutParameters() throws UnsupportedEncodingException {
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);

        eventLogger.log(EventLogger.IDE_USAGE, null);

        verify(eventLogger, times(1)).offerEvent(message.capture());
        assertEquals(message.getValue(), "EVENT#ide-usage# PARAMETERS##");
    }

    @Test
    public void shouldLogEventWithParameters() throws UnsupportedEncodingException {
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        Map<String, String> parameters = new HashMap<String, String>() {{
            put("file", "myfile.txt");
        }};

        eventLogger.log(EventLogger.IDE_USAGE, parameters);

        verify(eventLogger, times(1)).offerEvent(message.capture());
        assertEquals(message.getValue(), "EVENT#ide-usage# PARAMETERS#file=myfile.txt#");
    }

    @Test
    public void shouldLogEventWithParametersSpecialCharactersUseCase1() throws UnsupportedEncodingException {
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        Map<String, String> parameters = new LinkedHashMap<String, String>() {{
            put("p1", ",");
            put("p2", "=");
            put("p3", "#");
        }};

        eventLogger.log(EventLogger.IDE_USAGE, parameters);

        verify(eventLogger, times(1)).offerEvent(message.capture());
        assertEquals(message.getValue(), "EVENT#ide-usage# PARAMETERS#p1=%2C,p2=%3D,p3=%23#");
        assertEquals(URLDecoder.decode(message.getValue(), "UTF-8"), "EVENT#ide-usage# PARAMETERS#p1=,,p2==,p3=##");
    }

    @Test
    public void shouldLogEventWithParametersSpecialCharactersUseCase2() throws UnsupportedEncodingException {
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        Map<String, String> parameters = new LinkedHashMap<String, String>() {{
            put("p4", " ");
            put("p5", "+");
        }};

        eventLogger.log(EventLogger.IDE_USAGE, parameters);

        verify(eventLogger, times(1)).offerEvent(message.capture());
        assertEquals(message.getValue(), "EVENT#ide-usage# PARAMETERS#p4=+,p5=%2B#");
        assertEquals(URLDecoder.decode(message.getValue(), "UTF-8"), "EVENT#ide-usage# PARAMETERS#p4= ,p5=+#");
    }
}
