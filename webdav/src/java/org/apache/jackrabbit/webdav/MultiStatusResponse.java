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

import java.util.HashMap;
import java.util.Iterator;

/**
 * The <code>WebdavMultistatusResponse</code> class implements a structure that
 * hold a WebDAV multistatus response. Properties can be added to this response
 * with the respective error/status code.
 */
public class MultiStatusResponse implements DavConstants {

    /**
     * The content the 'href' element for this resposne
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
     * Hashmap with all stati
     */
    private final HashMap stati = new HashMap();

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
        stati.put(new Integer(DavServletResponse.SC_OK), status200);
        stati.put(new Integer(DavServletResponse.SC_NOT_FOUND), status404);
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
        stati.put(new Integer(status), new Element(null));
    }

    /**
     * Adds a JDOM element to this response
     *
     * @param prop the property to add
     * @param status the status of the response set to select
     */
    private void add(Element prop, int status) {
        Integer statusKey = new Integer(status);
        Element propsContainer = (Element) stati.get(statusKey);
        if (propsContainer == null) {
            propsContainer = new Element(XML_PROP, NAMESPACE);
            stati.put(statusKey, propsContainer);
        }
        propsContainer.addContent(prop);
    }

    /**
     * Adds a property to this response '200' propstat set.
     *
     * @param prop the property to add
     */
    public void add(DavProperty prop) {
        status200.addContent(prop.toXml());
    }

    /**
     * Adds a property name to this response '200' propstat set.
     *
     * @param name the property name to add
     */
    public void add(DavPropertyName name) {
        status200.addContent(name.toXml());
    }

    /**
     * Adds a property to this response
     *
     * @param prop the property to add
     * @param status the status of the response set to select
     */
    public void add(DavProperty prop, int status) {
        add(prop.toXml(), status);
    }

    /**
     * Adds a property name to this response
     *
     * @param name the property name to add
     * @param status the status of the response set to select
     */
    public void add(DavPropertyName name, int status) {
        add(name.toXml(), status);
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
        Iterator iter = stati.keySet().iterator();
        while (iter.hasNext()) {
            Integer statusKey = (Integer) iter.next();
	    Element prop = (Element) stati.get(statusKey);
            if (prop != null) {
                Element status = new Element(XML_STATUS, NAMESPACE);
                status.setText("HTTP/1.1 " + statusKey + " " + DavException.getStatusPhrase(statusKey.intValue()));

                if (XML_PROP.equals(prop.getName())) {
                    // do not add empty propstat elements
                    if (prop.getContentSize() > 0) {
                        Element propstat = new Element(XML_PROPSTAT, NAMESPACE);
                        propstat.addContent(prop);
                        propstat.addContent(status);
                        response.addContent(propstat);
                    }
                } else {
                    response.addContent(status);
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
}