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
package org.apache.jackrabbit.server.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>DefaultIOListener</code> implements an <code>IOListener</code> that
 * writes debug/error output to the {@link Logger logger} specified in the constructor.
 */
public class DefaultIOListener implements IOListener {

    private static Logger log = LoggerFactory.getLogger(DefaultIOListener.class);

    private Logger ioLog;

    /**
     * Creates a new <code>DefaultIOListener</code>
     */
    public DefaultIOListener(Logger ioLog) {
        this.ioLog = (ioLog != null) ? ioLog : log;
    }

    /**
     * @see IOListener#onBegin(IOHandler, IOContext)
     */
    public void onBegin(IOHandler handler, IOContext ioContext) {
        ioLog.debug("Starting IOHandler (" + handler.getName() + ")");
    }

    /**
     * @see IOListener#onEnd(IOHandler, IOContext, boolean)
     */
    public void onEnd(IOHandler handler, IOContext ioContext, boolean success) {
        ioLog.debug("Result for IOHandler (" + handler.getName() + "): " + (success ? "OK" : "Failed"));
    }

    /**
     * @see IOListener#onError(IOHandler, IOContext, Exception)
     */
    public void onError(IOHandler ioHandler, IOContext ioContext, Exception e) {
        ioLog.debug("Error: " + e.getMessage());
    }
}