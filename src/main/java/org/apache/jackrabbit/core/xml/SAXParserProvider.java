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
package org.apache.jackrabbit.core.xml;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * Provides configured SAXParser instances and logs which
 * parser class is being used.
 */
public class SAXParserProvider {
    private static Logger log = LoggerFactory.getLogger(SAXParserProvider.class);
    private static boolean infoLogged;

    /**
     * Return a configured SAXParser
     */
    public static SAXParser getParser() throws SAXException, ParserConfigurationException {
        // we could save the factory, but it's not threadsafe so keep it simple
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(
                "http://xml.org/sax/features/namespace-prefixes", false);
        SAXParser parser = factory.newSAXParser();

        // log only once (not threadsafe but good enough ;-)
        if (log.isInfoEnabled() && !infoLogged) {
            log.info("Using SAXParser class " + parser.getClass().getName());
            infoLogged = true;
        }
        return parser;
    }
}