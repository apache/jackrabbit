/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  The ASF licenses this file to You
* under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.jackrabbit.jcr2spi.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.jcr.RepositoryException;
import javax.jcr.Workspace;

import org.apache.jackrabbit.commons.xml.SerializingContentHandler;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * <code>WorkspaceContentHandler</code>...
 */
public class WorkspaceContentHandler extends DefaultHandler {

    private static Logger log = LoggerFactory.getLogger(WorkspaceContentHandler.class);

    private final String parentAbsPath;
    private final int uuidBehavior;
    private final Workspace workspace;

    private final File tmpFile;
    private final OutputStream tmpStream;
    private final ContentHandler delegatee;

    public WorkspaceContentHandler(Workspace workspace, String parentAbsPath, int uuidBehavior) throws RepositoryException {
        this.workspace = workspace;
        this.parentAbsPath = parentAbsPath;
        this.uuidBehavior = uuidBehavior;

        try {
            String tmpName = Text.md5(parentAbsPath);
            this.tmpFile = File.createTempFile("___" + tmpName, ".xml");
            this.tmpStream = new FileOutputStream(tmpFile);
            this.delegatee = SerializingContentHandler.getSerializer(tmpStream);
        } catch (FileNotFoundException e) {
            throw new RepositoryException(e);
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (SAXException e) {
            throw new RepositoryException(e);
        }
    }

    @Override
    public void endDocument() throws SAXException {
        delegatee.endDocument();
        try (InputStream is = new FileInputStream(tmpFile)) {
            workspace.importXML(parentAbsPath, is, uuidBehavior);
        } catch (IOException e) {
            throw new SAXException(e);
        } catch (RepositoryException e) {
            throw new SAXException(e);
        } finally {
            try {
                tmpStream.close();
            } catch (IOException ex) {
                // best effort
                log.error("closing temp file stream", ex);
            }
            tmpFile.delete();
        }
    }

    @Override
    public void startDocument() throws SAXException {
        delegatee.startDocument();
    }

    @Override
    public void characters(char ch[], int start, int length) throws SAXException {
        delegatee.characters(ch, start, length);
    }

    @Override
    public void ignorableWhitespace(char ch[], int start, int length) throws SAXException {
        delegatee.ignorableWhitespace(ch, start, length);
    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
        delegatee.endPrefixMapping(prefix);
    }

    @Override
    public void skippedEntity(String name) throws SAXException {
        delegatee.skippedEntity(name);
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        delegatee.setDocumentLocator(locator);
    }

    @Override
    public void processingInstruction(String target, String data) throws SAXException {
        delegatee.processingInstruction(target, data);
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        delegatee.startPrefixMapping(prefix, uri);
    }

    @Override
    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        delegatee.endElement(namespaceURI, localName, qName);
    }

    @Override
    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        delegatee.startElement(namespaceURI, localName, qName, atts);
    }
}
