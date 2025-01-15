package org.apache.jackrabbit.webdav.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpRequest;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.apache.jackrabbit.webdav.xml.XmlSerializable;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/** 
 * Factory methods for HTTP request body publishers. Body publishers are used to provide the body of an HTTP request.
 */
public class DavBodyPublishers {

    private DavBodyPublishers() {
        
    }

    public static HttpRequest.BodyPublisher ofXmlSerializable(XmlSerializable xmlSerializable) throws IOException {
        try {
            Document doc = DomUtil.createDocument();
            doc.appendChild(xmlSerializable.toXml(doc));
            return ofDocument(doc);
        } catch (ParserConfigurationException ex) {
            throw new IOException(ex);
        }
    }

    private static HttpRequest.BodyPublisher ofDocument(Document doc) throws IOException {
        try {
            ByteArrayOutputStream xml = new ByteArrayOutputStream();
            DomUtil.transformDocument(doc, xml);
            return HttpRequest.BodyPublishers.ofByteArray(xml.toByteArray());
        } catch (TransformerException|SAXException ex) {
            throw new IOException(ex);
        }
    }

}
