package org.apache.jackrabbit.tck;

import java.io.IOException;
import java.io.StringReader;
import java.io.ByteArrayOutputStream;

import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.*;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.RepositoryException;

import org.xml.sax.*;
import org.xml.sax.helpers.*;



/**
 * Builds a simple HTML page which lists tip titles
 * and provides links to HTML and text versions
 */
public class TestResultParser extends DefaultHandler {

    Map results = new TreeMap();
    String strLevels[] = new String[]{"level1", "level2", "sql", "locking", "versioning", "uuid", "observation"};
    String currLevel = "";

    boolean status = false;
    boolean getStatValue = false;

    public TestResultParser() {
        super();

        for (int i = 0; i < strLevels.length; i++) {
            results.put(strLevels[i], null);
        }
    }

    public void startElement(String namespace,
                            String localName,
                            String qName,
                            Attributes atts) {
        if (localName.equals("node")) {
            String name = atts.getValue("sv:name");
            if (results.containsKey(name)) {
                currLevel = name;
                results.put(currLevel, new Boolean(true));
            }
        } else if (localName.equals("property")) {
            String name = atts.getValue("sv:name");
            if (name.equals("status")) {
                status = true;
            }
        } else if (localName.equals("value") && status) {
            getStatValue = true;
            status = false;
        }
    }

    public Map interpretResult(String xmldoc) throws ParserConfigurationException, SAXException, IOException {

        SAXParserFactory spf = SAXParserFactory.newInstance();
        SAXParser sp = spf.newSAXParser();
        ParserAdapter pa = new ParserAdapter(sp.getParser());
        pa.setContentHandler(this);
        pa.parse(new InputSource(new StringReader(xmldoc)));
        return results;
    }

    public void characters (char ch[], int start, int length) {
        if (getStatValue) {
            String val = new String(ch, start, length);
            if (!"0".equals(val)) {
                results.put(currLevel, new Boolean(false));
            }
            getStatValue = false;
        }
    }
}

