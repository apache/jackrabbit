/*
 * Copyright 2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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
package org.apache.jackrabbit.webdav;

import org.jdom.Element;
import org.apache.jackrabbit.webdav.property.*;
import org.apache.jackrabbit.webdav.util.XmlUtil;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Iterator;

/**
 * The <code>WebdavMultistatusResponse</code> class implements a structure that
 * hold a WebDAV multistatus response. Properties can be added to this response
 * with the respective error/status code.
 */
// todo: the propstat element may also contain a responsedescription (currently ignored)
public class MultiStatusResponse implements DavConstants {

    private static Logger log = Logger.getLogger(MultiStatusResponse.class);

    /**
     * The content the 'href' element for this response
     */
    private final String href;

    /**
     * The '200' set (since this is used very often)
     */
    private final Element status200;

    /**
     * The '404' set (since this is used very often)
     */
    private final Element status404;

    /**
     * Hashmap containing all status
     */
    private final HashMap statusMap = new HashMap();

    /**
     * An optional response description.
     */
    private String responseDescription;

    /**
     * Constructs an empty WebDAV multistatus response
     */
    public MultiStatusResponse(String href) {
        this.href = href;
        status200 = new Element(XML_PROP, NAMESPACE);
        status404 = new Element(XML_PROP, NAMESPACE);
        statusMap.put(new Integer(DavServletResponse.SC_OK), status200);
        statusMap.put(new Integer(DavServletResponse.SC_NOT_FOUND), status404);
    }

    /**
     * Constucts a WebDAV multistatus response and retrieves the resource properties
     * according to the given <code>DavPropertyNameSet</code>.
     *
     * @param resource
     * @param propNameSet
     */
    public MultiStatusResponse(DavResource resource, DavPropertyNameSet propNameSet) {
        this(resource, propNameSet, PROPFIND_BY_PROPERTY);
    }

    /**
     * Constucts a WebDAV multistatus response and retrieves the resource properties
     * according to the given <code>DavPropertyNameSet</code>. It adds all known
     * property to the '200' set, while unknown properties are added to the '404' set.
     * <p/>
     * Note, that the set of property names is ignored in case of a {@link #PROPFIND_ALL_PROP}
     * and {@link #PROPFIND_PROPERTY_NAMES} propFindType.
     *
     * @param resource The resource to retrieve the property from
     * @param propNameSet The property name set as obtained from the request body.
     * @param propFindType any of the following values: {@link #PROPFIND_ALL_PROP},
     * {@link #PROPFIND_BY_PROPERTY}, {@link #PROPFIND_PROPERTY_NAMES}
     */
    public MultiStatusResponse(DavResource resource, DavPropertyNameSet propNameSet,
			       int propFindType) {
        this(resource.getHref());

	// only property names requested
	if (propFindType == PROPFIND_PROPERTY_NAMES) {
	    DavPropertyName[] propNames = resource.getPropertyNames();
	    for (int i = 0; i < propNames.length; i++) {
		status200.addContent(propNames[i].toXml());
	    }
	// all or a specified set of property and their values requested.
	} else {
	    // clone set of property, since several resources could use this again
	    propNameSet = new DavPropertyNameSet(propNameSet);
	    // Add requested properties or all non-protected properties
	    DavPropertyIterator iter = resource.getProperties().iterator();
	    while (iter.hasNext()) {
		DavProperty wdp = iter.nextProperty();
		if ((propFindType == PROPFIND_ALL_PROP && !wdp.isProtected()) || propNameSet.remove(wdp.getName())) {
		    status200.addContent(wdp.toXml());
		}
	    }

	    if (propFindType != PROPFIND_ALL_PROP) {
		Iterator iter1 = propNameSet.iterator();
		while (iter1.hasNext()) {
		    DavPropertyName propName = (DavPropertyName) iter1.next();
		    status404.addContent(propName.toXml());
		}
	    }
	}
    }

   /**
    * Constructs an WebDAV multistatus response for a given resource. This
    * would be used by COPY, MOVE, DELETE, LOCK, UNLOCK that require a multistatus
    * in case of failure.
    */
    public MultiStatusResponse(String href, int status) {
        this(href);
        statusMap.put(new Integer(status), new Element(null));
    }

    /**
     * Returns the href
     *
     * @return href
     */
    public String getHref() {
        return href;
    }

    /**
     * Adds a JDOM element to this response
     *
     * @param propertyElem the property to add
     * @param status the status of the response set to select
     */
    private void add(Element propertyElem, int status) {
        Integer statusKey = new Integer(status);
        Element propsContainer = (Element) statusMap.get(statusKey);
        if (propsContainer == null) {
            propsContainer = new Element(XML_PROP, NAMESPACE);
            statusMap.put(statusKey, propsContainer);
        }
        propsContainer.addContent(propertyElem.detach());
    }

    /**
     * Adds a property to this response '200' propstat set.
     *
     * @param property the property to add
     */
    public void add(DavProperty property) {
        status200.addContent(property.toXml());
    }

    /**
     * Adds a property name to this response '200' propstat set.
     *
     * @param propertyName the property name to add
     */
    public void add(DavPropertyName propertyName) {
        status200.addContent(propertyName.toXml());
    }

    /**
     * Adds a property to this response
     *
     * @param property the property to add
     * @param status the status of the response set to select
     */
    public void add(DavProperty property, int status) {
        add(property.toXml(), status);
    }

    /**
     * Adds a property name to this response
     *
     * @param propertyName the property name to add
     * @param status the status of the response set to select
     */
    public void add(DavPropertyName propertyName, int status) {
        add(propertyName.toXml(), status);
    }

    /**
     * Get properties present in this response for the given status code.
     * @param status
     * @return property set
     */
    public DavPropertySet getProperties(int status) {
        DavPropertySet set = new DavPropertySet();
        Integer key = new Integer(status);
        if (statusMap.containsKey(key)) {
            Element propElem = (Element) statusMap.get(key);
            if (propElem != null) {
                Iterator it = propElem.getChildren().iterator();
                while (it.hasNext()) {
                    Element propEntry = (Element) it.next();
                    DavProperty prop = DefaultDavProperty.createFromXml(propEntry);
                    set.add(prop);
                }
            }
        }
        return set;
    }

    /**
     * Get property names present in this response for the given status code.
     *
     * @param status
     * @return property names
     */
    public DavPropertyNameSet getPropertyNames(int status) {
        DavPropertyNameSet set = new DavPropertyNameSet();
        Integer key = new Integer(status);
        if (statusMap.containsKey(key)) {
            Element propElem = (Element) statusMap.get(key);
            if (propElem != null) {
                Iterator it = propElem.getChildren().iterator();
                while (it.hasNext()) {
                    Element propEntry = (Element) it.next();
                    set.add(DavPropertyName.createFromXml(propEntry));
                }
            }
        }
        return set;
    }

    /**
     * @return responseDescription
     */
    public String getResponseDescription() {
	return responseDescription;
    }

    /**
     * Set the content of the optional response description element, which is
     * intended to contain a message that can be displayed to the user
     * explaining the nature of this response.
     *
     * @param responseDescription
     */
    public void setResponseDescription(String responseDescription) {
        this.responseDescription = responseDescription;
    }

    /**
     * Creates the JDOM element for this reponse.
     *
     * @return A JDOM element of this response
     */
    public Element toXml() {
        // don't create empty 'href' responses
        if ("".equals(href)) {
            return null;
        }
        Element response= new Element(XML_RESPONSE, NAMESPACE);
        // add '<href>'
        response.addContent(XmlUtil.hrefToXml(href));
        // add '<propstat>' elements or a single '<status>' element
        Iterator iter = statusMap.keySet().iterator();
        while (iter.hasNext()) {
            Integer statusKey = (Integer) iter.next();
	    Element prop = (Element) statusMap.get(statusKey);
            if (prop != null) {
                Status status = new Status(statusKey.intValue());
                if (XML_PROP.equals(prop.getName())) {
                    // do not add empty propstat elements
                    if (prop.getContentSize() > 0) {
                        Element propstat = new Element(XML_PROPSTAT, NAMESPACE);
                        propstat.addContent(prop);
                        propstat.addContent(status.toXml());
                        response.addContent(propstat);
                    }
                } else {
                    response.addContent(status.toXml());
                }
	    }
        }
        // add the optional '<responsedescription>' element
        if (responseDescription != null) {
            Element desc = new Element(XML_RESPONSEDESCRIPTION, NAMESPACE);
            desc.setText(responseDescription);
            response.addContent(desc);
        }
        return response;
    }

    /**
     * Build a new response object from the given xml element.
     *
     * @param responseElement
     * @return new <code>MultiStatusResponse</code> instance
     * @throws  IllegalArgumentException if the specified element is <code>null</code>
     */
    public static MultiStatusResponse createFromXml(Element responseElement) {
        if (responseElement == null) {
	    throw new IllegalArgumentException("The response element must not be null.");
	}
        String href = responseElement.getChildText(XML_HREF, NAMESPACE);
        String statusLine = responseElement.getChildText(XML_STATUS, NAMESPACE);
        MultiStatusResponse response = (statusLine != null) ? new MultiStatusResponse(href, Status.createFromStatusLine(statusLine).getStatusCode()) : new MultiStatusResponse(href);

        // read propstat elements
        Iterator it = responseElement.getChildren(XML_PROPSTAT, NAMESPACE).iterator();
	while (it.hasNext()) {
	    Element propstat = (Element)it.next();
            Element prop = propstat.getChild(XML_PROP, NAMESPACE);
            String propstatus = propstat.getChildText(XML_STATUS, NAMESPACE);
            if (propstatus != null) {
                Status st = Status.createFromStatusLine(propstatus);
                Element[] propertyElems = (Element[]) prop.getChildren().toArray(new Element[0]);
                for (int i = 0; i < propertyElems.length; i++) {
                    response.add(propertyElems[i], st.getStatusCode());
                }
            }
            // todo: propstat may also contain a responsedescription
	}

        response.setResponseDescription(responseElement.getChildText(XML_RESPONSEDESCRIPTION, NAMESPACE));
        return response;
    }

    /**
     * Inner class encapsulating the 'status' present in the multistatus response.
     */
    private static class Status {

        private String version = "HTTP/1.1";
        private int code;
        private String phrase = "";

        private Status(int code) {
            this.code = code;
            phrase = DavException.getStatusPhrase(code);
        }

        private Status(String version, int code, String phrase) {
            this.version = version;
            this.code = code;
            this.phrase = phrase;
        }

        private int getStatusCode() {
            return code;
        }

        private Element toXml() {
            String statusLine = version + " " + code + " " + phrase;
            return new Element(XML_STATUS, NAMESPACE).setText(statusLine);
        }

        private static Status createFromStatusLine(String statusLine) {
            if (statusLine == null) {
                throw new IllegalArgumentException("Unable to parse status line from null xml element.");
            }
            Status status;

            // code copied from org.apache.commons.httpclient.StatusLine
            int length = statusLine.length();
            int at = 0;
            int start = 0;
            try {
                while (Character.isWhitespace(statusLine.charAt(at))) {
                    ++at;
                    ++start;
                }
                if (!"HTTP".equals(statusLine.substring(at, at += 4))) {
                    log.warn("Status-Line '" + statusLine + "' does not start with HTTP");
                }
                //handle the HTTP-Version
                at = statusLine.indexOf(" ", at);
                if (at <= 0) {
                    log.warn("Unable to parse HTTP-Version from the status line: '"+ statusLine + "'");
                }
                String version = (statusLine.substring(start, at)).toUpperCase();
                //advance through spaces
                while (statusLine.charAt(at) == ' ') {
                    at++;
                }
                //handle the Status-Code
                int to = statusLine.indexOf(" ", at);
                if (to < 0) {
                    to = length;
                }
                try {
                    int code = Integer.parseInt(statusLine.substring(at, to));
                    status = new Status(code);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Unable to parse status code from status line: '"+ statusLine + "'");
                }
                //handle the Reason-Phrase
                String phrase = "";
                at = to + 1;
                if (at < length) {
                    phrase = statusLine.substring(at).trim();
                }

                status.version = version;
                status.phrase = phrase;

            } catch (StringIndexOutOfBoundsException e) {
                throw new IllegalArgumentException("Status-Line '" + statusLine + "' is not valid");
            }
            return status;
        }
    }
}