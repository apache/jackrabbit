/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.xml.nodetype;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.jackrabbit.state.nodetype.NodeTypeManagerState;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * TODO
 */
public class NodeTypeXML {

    public static NodeTypeManagerState read(Source source)
            throws TransformerConfigurationException, TransformerException {
        NodeTypeXMLReader reader = new NodeTypeXMLReader();
        TransformerFactory factory = TransformerFactory.newInstance();
        // factory.setFeature("http://xml.org/sax/features/namespaces", true);
        Transformer transformer = factory.newTransformer();
        transformer.transform(source, new SAXResult(reader));
        return reader.getNodeTypeManagerState();
    }

    public static NodeTypeManagerState read(InputSource source)
            throws IOException, ParserConfigurationException, SAXException {
        NodeTypeXMLReader reader = new NodeTypeXMLReader();
        SAXParserFactory factory = SAXParserFactory.newInstance();
        // factory.setFeature("http://xml.org/sax/features/namespaces", true);
        SAXParser parser = factory.newSAXParser();
        parser.parse(source, reader);
        return reader.getNodeTypeManagerState();
    }

    public static NodeTypeManagerState read(File file)
            throws IOException, ParserConfigurationException, SAXException {
        NodeTypeXMLReader reader = new NodeTypeXMLReader();
        SAXParserFactory factory = SAXParserFactory.newInstance();
        // factory.setFeature("http://xml.org/sax/features/namespaces", true);
        SAXParser parser = factory.newSAXParser();
        parser.parse(file, reader);
        return reader.getNodeTypeManagerState();
    }

    public static NodeTypeManagerState read(InputStream input)
            throws IOException, ParserConfigurationException, SAXException {
        NodeTypeXMLReader reader = new NodeTypeXMLReader();
        SAXParserFactory factory = SAXParserFactory.newInstance();
        // factory.setFeature("http://xml.org/sax/features/namespaces", true);
        SAXParser parser = factory.newSAXParser();
        parser.parse(input, reader);
        return reader.getNodeTypeManagerState();
    }

    public static void write(NodeTypeManagerState state, ContentHandler handler)
            throws SAXException {
        NodeTypeXMLWriter writer = new NodeTypeXMLWriter(state);
        writer.write(handler);
    }

    public static void write(NodeTypeManagerState state, Result result)
            throws TransformerConfigurationException, SAXException {
        SAXTransformerFactory factory = (SAXTransformerFactory)
            SAXTransformerFactory.newInstance();
        TransformerHandler handler = factory.newTransformerHandler();
        handler.setResult(result);
        write(state, handler);
    }


    public static void write(NodeTypeManagerState state, OutputStream output)
            throws TransformerConfigurationException, SAXException {
        write(state, new StreamResult(output));
    }

    public static void write(NodeTypeManagerState state, File file)
            throws IOException, TransformerConfigurationException, SAXException {
        OutputStream output = new FileOutputStream(file);
        try {
            write(state, output);
        } finally {
            output.close();
        }
    }

    public static void write(NodeTypeManagerState state, String filename)
            throws IOException, TransformerConfigurationException, SAXException {
        write(state, new File(filename));
    }

}
