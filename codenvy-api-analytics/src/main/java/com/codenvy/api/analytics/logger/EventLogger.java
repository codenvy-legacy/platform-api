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

import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Anatoliy Bazko
 */
@Singleton
public class EventLogger {
    private static final Logger LOG = LoggerFactory.getLogger(EventLogger.class);

    private static final int MAX_EXTENDED_PARAMS_NUMBER = 3;
    private static final int RESERVED_PARAMS_NUMBER     = 6;
    private static final int MAX_PARAM_NAME_LENGTH      = 20;
    private static final int MAX_PARAM_VALUE_LENGTH     = 50;
    private static final int QUEUE_MAX_CAPACITY         = 10000;

    private static final String EVENT_PARAM        = "EVENT";
    private static final String WS_PARAM           = "WS";
    private static final String USER_PARAM         = "USER";
    private static final String SOURCE_PARAM       = "SOURCE";
    private static final String ACTION_PARAM       = "ACTION";
    private static final String PROJECT_NAME_PARAM = "PROJECT";
    private static final String PROJECT_TYPE_PARAM = "TYPE";
    private static final String PARAMETERS_PARAM   = "PARAMETERS";

    public static final String IDE_USAGE_EVENT = "ide-usage";

    private static final Set<String> ALLOWED_EVENTS = new HashSet<String>() {{
        add(IDE_USAGE_EVENT);
    }};

    private final Queue<String> queue;

    /**
     * Stores the number of ignored events due to maximum queue capacity
     */
    private long ignoredEvents;

    public EventLogger() {
        this.queue = new LinkedBlockingQueue<>(QUEUE_MAX_CAPACITY);
        this.ignoredEvents = 0;

        Thread logThread = new LogThread();
        logThread.setDaemon(true);
        logThread.start();
    }

    public void log(String event, Map<String, String> parameters) throws UnsupportedEncodingException {
        if (event != null && ALLOWED_EVENTS.contains(event)) {
            parameters = parameters != null ? parameters : Collections.<String, String>emptyMap();

            validate(parameters);

            String message = createMessage(event, parameters);
            if (!offerEvent(message)) {
                if (ignoredEvents++ % 1000 == 0) {
                    LOG.warn("Ignored " + ignoredEvents + " events due to maximum queue capacity");
                }
            }
        }
    }

    protected boolean offerEvent(String message) {
        return queue.offer(message);
    }

    private String createMessage(String event, Map<String, String> parameters) throws UnsupportedEncodingException {
        StringBuilder message = new StringBuilder();

        addParam(message, EVENT_PARAM, event);

        addParam(message, WS_PARAM, parameters);
        addParam(message, USER_PARAM, parameters);
        addParam(message, PROJECT_NAME_PARAM, parameters);
        addParam(message, PROJECT_TYPE_PARAM, parameters);
        addParam(message, SOURCE_PARAM, parameters);
        addParam(message, ACTION_PARAM, parameters);

        addParam(message, PARAMETERS_PARAM, getParametersAsString(parameters));

        return message.toString();
    }

    private String getParametersAsString(Map<String, String> parameters) throws UnsupportedEncodingException {
        StringBuilder builder = new StringBuilder();

        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            if (builder.length() > 0) {
                builder.append(',');
            }

            builder.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            builder.append('=');
            builder.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }

        return builder.toString();
    }

    private void addParam(StringBuilder message, String param, Map<String, String> parameters) {
        if (parameters.containsKey(param)) {
            addParam(message, param, parameters.remove(param));
        }
    }

    private void addParam(StringBuilder message, String param, String value) {
        if (message.length() > 0) {
            message.append(' ');
        }

        message.append(param);
        message.append('#');
        message.append(value);
        message.append('#');
    }

    private void validate(Map<String, String> additionalParams) throws IllegalArgumentException {
        if (additionalParams.size() > MAX_EXTENDED_PARAMS_NUMBER + RESERVED_PARAMS_NUMBER) {
            throw new IllegalArgumentException("The number of parameters exceeded the limit in " +
                                               MAX_EXTENDED_PARAMS_NUMBER);
        }

        for (Map.Entry<String, String> entry : additionalParams.entrySet()) {
            String param = entry.getKey();
            String value = entry.getValue();

            if (param.length() > MAX_PARAM_NAME_LENGTH) {
                throw new IllegalArgumentException(
                        "The length of parameter name " + param + " exceeded the length in " + MAX_PARAM_NAME_LENGTH +
                        " characters");

            } else if (value.length() > MAX_PARAM_VALUE_LENGTH) {
                throw new IllegalArgumentException(
                        "The length of parameter value " + value + " exceeded the length in " + MAX_PARAM_VALUE_LENGTH +
                        " characters");
            }
        }
    }

    /**
     * Is responsible for logging events.
     * Rate-limit is 50 messages per second.
     */
    private class LogThread extends Thread {
        private LogThread() {
            super("Analytics Event Logger");
            LOG.info(getName() + " thread is started, queue is initialized for " + QUEUE_MAX_CAPACITY + " messages");
        }

        @Override
        public void run() {
            while (!isInterrupted()) {
                String message = queue.poll();

                try {
                    if (message != null) {
                        LOG.info(message);
                        sleep(20);
                    } else {
                        sleep(1000);
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }

            LOG.info(getName() + " thread is stopped");
        }
    }
}
