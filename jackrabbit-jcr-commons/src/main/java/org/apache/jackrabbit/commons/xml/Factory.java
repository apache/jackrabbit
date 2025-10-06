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
package org.apache.jackrabbit.commons.xml;

import org.xml.sax.EntityResolver;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;

/**
 * Factory for "safe" instances of XML parsers (wrt XXE etc.).
 */
public class Factory {

    private Factory() {
    }

    /**
     * @return a "safe" {@link DocumentBuilderFactory}
     */
    public static DocumentBuilderFactory safeDocumentBuilderFactory() {
        // see https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html

        javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();

        factory.setIgnoringComments(false);
        factory.setIgnoringElementContentWhitespace(true);
        factory.setXIncludeAware(false);

        // Prevent XXE attacks by disabling external entity processing
        factory.setExpandEntityReferences(false);

        String feature = null;

        try {
            feature = XMLConstants.FEATURE_SECURE_PROCESSING;
            factory.setFeature(feature, true);
            feature = "http://apache.org/xml/features/disallow-doctype-decl";
            factory.setFeature(feature, true);
            feature = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
            factory.setFeature(feature, false);
            feature = "http://xml.org/sax/features/external-general-entities";
            factory.setFeature(feature, false);
            feature = "http://xml.org/sax/features/external-parameter-entities";
            factory.setFeature(feature, false);
        } catch (Exception ex) {
            // abort if secure processing is not supported
            throw new IllegalStateException("Secure processing feature '" + feature + "' not supported by the DocumentBuilderFactory: " + factory.getClass().getName(), ex);
        }
        return factory;
    }

    /**
     * @return an {@link EntityResolver} which will always cause a parse exception.
     */
    public static EntityResolver nonResolvingEntityResolver() {
        return (publicId, systemId) -> {
            throw new IOException("This parser does not support resolution of external entities (publicId: " + publicId
                    + ", systemId: " + systemId + ")");
        };
    }
}
