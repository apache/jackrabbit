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
package org.apache.jackrabbit.core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Error handler for errors in the repository or workspace configuration.
 */
public class ConfigurationErrorHandler implements ErrorHandler {

    private static Logger log = LoggerFactory.getLogger(ConfigurationErrorHandler.class);

    /**
     * This method is called when there is an error parsing the configuration file.
     * The relevant information is written to the log file.
     */
    public void error(SAXParseException exception) throws SAXException {
        log("Warning", exception);
    }

    private void log(String type, SAXParseException exception) {
        log.warn(type + " parsing the configuration at line " + exception.getLineNumber() + " using system id " + exception.getSystemId() + ": " + exception.toString());
    }

    /**
     * This method is called when there is a fatal error parsing the configuration file.
     * The relevant information is written to the log file.
     */
    public void fatalError(SAXParseException exception) throws SAXException {
        log("Fatal error", exception);
        throw exception;
    }

    /**
     * This method is called when there is a warning parsing the configuration file.
     * The relevant information is written to the log file.
     */
    public void warning(SAXParseException exception) throws SAXException {
        log("Warning", exception);
    }

}
