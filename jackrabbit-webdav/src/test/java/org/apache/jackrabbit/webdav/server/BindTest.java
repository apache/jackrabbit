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
package org.apache.jackrabbit.webdav.server;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import junit.framework.TestCase;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.DavMethod;
import org.apache.jackrabbit.webdav.client.methods.MoveMethod;
import org.apache.jackrabbit.webdav.client.methods.OptionsMethod;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.client.methods.VersionControlMethod;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Test cases for WebDAV BIND functionality (see <a href="http://greenbytes.de/tech/webdav/draft-ietf-webdav-bind-20.html">draft-ietf-webdav-bind-20</a>
 * <p>
 * Required system properties:
 * <ul>
 *   <li>webdav.test.url</li>
 *   <li>webdav.test.username</li>
 *   <li>webdav.test.password</li>
 * </ul>
 */

public class BindTest extends TestCase {

    private URI uri;
    private String username, password;
    private HttpClient client;
    
    protected void setUp() throws Exception {
        this.uri = URI.create(System.getProperty("webdav.test.url"));
        this.username = System.getProperty(("webdav.test.username"), "");
        this.password = System.getProperty(("webdav.test.password"), "");
        this.client = new HttpClient();
        this.client.getState().setCredentials(
                new AuthScope(this.uri.getHost(), this.uri.getPort()),
                new UsernamePasswordCredentials(this.username, this.password));
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    // http://greenbytes.de/tech/webdav/draft-ietf-webdav-bind-20.html#rfc.section.8.1
    public void testOptions() throws HttpException, IOException {
        OptionsMethod options = new OptionsMethod(this.uri.toASCIIString());
        int status = this.client.executeMethod(options);
        assertEquals(200, status);
        Set features = getDavFeatures(options);
        assertTrue("DAV header should include 'bind' feature: " + features, features.contains("bind"));
    }

    // create test resource, make it referenceable, check resource id, move resource, check again
    public void testResourceId() throws HttpException, IOException, DavException, URISyntaxException {
        String testuri = this.uri.toASCIIString() + (this.uri.toASCIIString().endsWith("/") ? "" : "/") + "bindtest"; 
        String testuri2 = this.uri.toASCIIString() + (this.uri.toASCIIString().endsWith("/") ? "" : "/") + "bindtest2"; 
        
        PutMethod put = new PutMethod(testuri);
        put.setRequestEntity(new StringRequestEntity("foo", "text/plain", "UTF-8"));
        int status = this.client.executeMethod(put);
        assertTrue(status == 200 || status == 201 || status == 204);
        
        // enabling version control always makes the resource referenceable
        VersionControlMethod versioncontrol = new VersionControlMethod(testuri);
        status = this.client.executeMethod(versioncontrol);
        assertTrue(status == 200 || status == 201);
        
        URI resourceId = getResourceId(testuri);
        
        MoveMethod move = new MoveMethod(testuri, testuri2, true);
        status = this.client.executeMethod(move);
        String s = move.getResponseBodyAsString();
        assertTrue(status == 204);
        
        URI resourceId2 = getResourceId(testuri2);
        assertEquals(resourceId, resourceId2);
    }
    
    // utility methods
    
    // see http://greenbytes.de/tech/webdav/draft-ietf-webdav-bind-20.html#rfc.section.3.1
    private URI getResourceId(String uri) throws IOException, DavException, URISyntaxException {
        DavPropertyNameSet names = new DavPropertyNameSet();
        names.add(DavPropertyName.RESOURCEID); 
        PropFindMethod propfind = new PropFindMethod(uri, names, 0);
        int status = this.client.executeMethod(propfind);
        assertTrue(status == 207);
        MultiStatus multistatus = propfind.getResponseBodyAsMultiStatus();
        MultiStatusResponse[] responses = multistatus.getResponses();
        assertEquals(1, responses.length);
        DavProperty resourceId = responses[0].getProperties(200).get(DavPropertyName.RESOURCEID);
        assertNotNull(resourceId);
        assertTrue(resourceId.getValue() instanceof Element);
        Element href = (Element)resourceId.getValue();
        assertEquals("href", href.getLocalName());
        String text = getUri(href);
        URI resid = new URI(text);
        return resid;
    }
    
    private String getUri(Element href) {
        String s = "";
        for (Node c = href.getFirstChild(); c != null; c = c.getNextSibling()) {
            if (c.getNodeType() == Node.TEXT_NODE) {
                s += c.getNodeValue();
            }
        }
        return s;
    }
    
    private Set getDavFeatures(DavMethod method) {
        Set result = new HashSet();
        Header[] features = method.getResponseHeaders("DAV");
        for (int i = 0; i < features.length; i++) {
            String val = features[i].getValue();
            StringTokenizer tok = new StringTokenizer(val, "\t ,");
            while (tok.hasMoreTokens()) {
                result.add(tok.nextToken());
            }
        }
        return result;
    }
}
