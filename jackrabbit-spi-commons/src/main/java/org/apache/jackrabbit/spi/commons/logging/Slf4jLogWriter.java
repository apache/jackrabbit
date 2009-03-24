/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.spi.commons.logging;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.slf4j.Logger;

/**
 * {@link LogWriter} implementation which uses SLF4J for persisting log messages.
 */
public class Slf4jLogWriter implements LogWriter {
    private final Logger log;

    /**
     * Create a new instance which uses the passed SLF4J logger for persisting
     * the log messages.
     * @param log
     */
    public Slf4jLogWriter(Logger log) {
        super();
        this.log = log;
    }

    /**
     * Returns
     * <pre>
     *   System.currentTimeMillis();
     * </pre>
     * {@inheritDoc}
     */
    public long systemTime() {
        return System.currentTimeMillis();
    }

    /**
     * Logs the call at debug level is debug level is enabled.
     * {@inheritDoc}
     */
    public void enter(final String methodName, final Object[] args) {
        if (log.isDebugEnabled()) {
            log.debug("ENTER(" + systemTime() + ") | " + methodName + "(" + formatArgs(args) + ")");
        }
    }

    /**
     * Logs the call at debug level is debug level is enabled.
     * {@inheritDoc}
     */
    public void leave(final String methodName, final Object[] args, final Object result) {
        if (log.isDebugEnabled()) {
            log.debug("LEAVE(" + systemTime() + ") | " + methodName + "(" + formatArgs(args) + ") = "
                    + formatResult(result));
        }
    }

    /**
     * Logs the exception including a stack trace at debug level is debug level is enabled.
     * {@inheritDoc}
     */
    public void error(final String methodName, final Object[] args, final Exception e) {
        if (log.isDebugEnabled()) {
            log.debug("ERROR(" + systemTime() + ") | " + methodName + "(" + formatArgs(args) + ") | "
                    + formatException(e));
        }
    }

    // -----------------------------------------------------< private >---

    private String formatArgs(Object[] args) {
        StringBuffer b = new StringBuffer();
        formatArgs(args, b);
        return b.toString();
    }

    private String formatResult(Object result) {
        StringBuffer b = new StringBuffer();
        formatArg(result, b);
        return b.toString();
    }

    private String formatException(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private void formatArgs(Object[] args, StringBuffer b) {
        String separator = "";
        for (int k = 0; k < args.length; k++) {
            b.append(separator);
            formatArg(args[k], b);
            separator = ", ";
        }
    }

    private void formatArg(Object arg, StringBuffer b) {
        if (arg instanceof Object[]) {
            b.append('[');
            formatArgs((Object[]) arg, b);
            b.append(']');
        }
        else {
            b.append(arg);
        }
    }

}
