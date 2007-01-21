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
package org.apache.jackrabbit.spi.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.compact.CompactNodeTypeDefReader;
import org.apache.jackrabbit.core.nodetype.compact.ParseException;
import org.apache.jackrabbit.jcr2spi.RepositoryImpl;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.xml.nodetype.XMLQNodeTypeDefinition;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XMLRepositoryFactory {

    private final QNodeTypeDefinition[] types;

    public XMLRepositoryFactory() throws RepositoryException {
        try {
            InputStream cnd =
                XMLRepositoryFactory.class.getResourceAsStream("types.cnd");
            CompactNodeTypeDefReader reader = new CompactNodeTypeDefReader(
                    new InputStreamReader(cnd, "UTF-8"), null);
            NodeTypeDef[] defs = (NodeTypeDef[])
                reader.getNodeTypeDefs().toArray(new NodeTypeDef[0]);
            types = new QNodeTypeDefinition[defs.length];
            for (int i = 0; i < defs.length; i++) {
                types[i] = new XMLQNodeTypeDefinition(defs[i]);
            }
        } catch (ParseException e) {
            throw new RepositoryException("Invalid node type definitions", e);
        } catch (UnsupportedEncodingException e) {
            throw new RepositoryException("UTF-8 not supported", e);
        }
    }

    public Repository getRepository(Document xml) throws RepositoryException {
        XMLRepositoryService service = new XMLRepositoryService(xml, types);
        return RepositoryImpl.create(new XMLRepositoryConfig(service));
    }

    public Repository getRepository(InputSource xml)
            throws RepositoryException, IOException, SAXException,
            ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return getRepository(builder.parse(xml));
    }

    public Repository getRepository(InputStream xml)
            throws RepositoryException, IOException, SAXException,
            ParserConfigurationException {
        return getRepository(new InputSource(xml));
    }

    public Repository getRepository(String xml)
            throws RepositoryException, IOException, SAXException,
            ParserConfigurationException {
        return getRepository(new InputSource(new StringReader(xml)));
    }

}
