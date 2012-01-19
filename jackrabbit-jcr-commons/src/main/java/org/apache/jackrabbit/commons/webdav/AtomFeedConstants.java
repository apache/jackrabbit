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
package org.apache.jackrabbit.commons.webdav;

import javax.xml.namespace.QName;

/**
 * <code>AtomFeedConstants</code> provides string constants for Atom feed 
 * (RFC 4287) resources.
 */
public interface AtomFeedConstants {

    /**
     * Namespace URI for RFC 4287 elements.
     */
    public static final String NS_URI = "http://www.w3.org/2005/Atom";
    
    public static final String MEDIATYPE = "application/atom+xml";

    public static final String XML_AUTHOR = "author";
    public static final String XML_CONTENT = "content";
    public static final String XML_ENTRY = "entry";
    public static final String XML_FEED = "feed";
    public static final String XML_ID = "id";
    public static final String XML_LINK = "link";
    public static final String XML_NAME = "name";
    public static final String XML_TITLE = "title";
    public static final String XML_UPDATED = "updated";

    public static final QName N_AUTHOR = new QName(NS_URI, XML_AUTHOR);
    public static final QName N_CONTENT = new QName(NS_URI, XML_CONTENT);
    public static final QName N_ENTRY = new QName(NS_URI, XML_ENTRY);
    public static final QName N_FEED = new QName(NS_URI, XML_FEED);
    public static final QName N_ID = new QName(NS_URI, XML_ID);
    public static final QName N_LINK = new QName(NS_URI, XML_LINK);
    public static final QName N_NAME = new QName(NS_URI, XML_NAME);
    public static final QName N_TITLE = new QName(NS_URI, XML_TITLE);
    public static final QName N_UPDATED = new QName(NS_URI, XML_UPDATED);
}
