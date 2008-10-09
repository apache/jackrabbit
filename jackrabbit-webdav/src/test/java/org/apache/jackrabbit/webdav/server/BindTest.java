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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.bind.BindConstants;
import org.apache.jackrabbit.webdav.bind.BindInfo;
import org.apache.jackrabbit.webdav.bind.ParentElement;
import org.apache.jackrabbit.webdav.bind.RebindInfo;
import org.apache.jackrabbit.webdav.bind.UnbindInfo;
import org.apache.jackrabbit.webdav.client.methods.BindMethod;
import org.apache.jackrabbit.webdav.client.methods.DavMethodBase;
import org.apache.jackrabbit.webdav.client.methods.DeleteMethod;
import org.apache.jackrabbit.webdav.client.methods.MkColMethod;
import org.apache.jackrabbit.webdav.client.methods.MoveMethod;
import org.apache.jackrabbit.webdav.client.methods.OptionsMethod;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.client.methods.RebindMethod;
import org.apache.jackrabbit.webdav.client.methods.UnbindMethod;
import org.apache.jackrabbit.webdav.client.methods.VersionControlMethod;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Test cases for WebDAV BIND functionality (see <a href="http://greenbytes.de/tech/webdav/draft-ietf-webdav-bind-21.html">draft-ietf-webdav-bind-21</a>
 * <p>
 * Required system properties:
 * <ul>
 *   <li>webdav.test.url</li>
 *   <li>webdav.test.username</li>
 *   <li>webdav.test.password</li>
 * </ul>
 */

public class BindTest extends TestCase {

    private String root;
    private URI uri;
    private String username, password;
    private HttpClient client;
    
    protected void setUp() throws Exception {
        this.uri = URI.create(System.getProperty("webdav.test.url"));
        this.root = this.uri.toASCIIString();
        if (!this.root.endsWith("/")) {
            this.root += "/";
        }
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
        List allow = Arrays.asList(options.getAllowedMethods());
        assertTrue("DAV header should include 'bind' feature", options.hasComplianceClass("bind"));
        assertTrue("Allow header should include BIND method", allow.contains("BIND"));
        assertTrue("Allow header should include REBIND method", allow.contains("REBIND"));
        assertTrue("Allow header should include UNBIND method", allow.contains("UNBIND"));
    }

    // create test resource, make it referenceable, check resource id, move resource, check again
    public void testResourceId() throws HttpException, IOException, DavException, URISyntaxException {

        String testcol = this.root + "testResourceId/";
        String testuri1 = testcol + "bindtest1";
        String testuri2 = testcol + "bindtest2";
        int status;
        try {
            MkColMethod mkcol = new MkColMethod(testcol);
            status = this.client.executeMethod(mkcol);
            assertEquals(201, status);

            PutMethod put = new PutMethod(testuri1);
            put.setRequestEntity(new StringRequestEntity("foo", "text/plain", "UTF-8"));
            status = this.client.executeMethod(put);
            assertEquals(201, status);

            // enabling version control always makes the resource referenceable
            VersionControlMethod versioncontrol = new VersionControlMethod(testuri1);
            status = this.client.executeMethod(versioncontrol);
            assertTrue("status: " + status, status == 200 || status == 201);

            URI resourceId = getResourceId(testuri1);

            MoveMethod move = new MoveMethod(testuri1, testuri2, true);
            status = this.client.executeMethod(move);
            move.getResponseBodyAsString();
            assertEquals(201, status);

            URI resourceId2 = getResourceId(testuri2);
            assertEquals(resourceId, resourceId2);
        } finally {
            DeleteMethod delete = new DeleteMethod(testcol);
            status = this.client.executeMethod(delete);
            assertTrue("status: " + status, status == 200 || status == 204);
        }
    }
    
    // utility methods
    
    // see http://greenbytes.de/tech/webdav/draft-ietf-webdav-bind-20.html#rfc.section.3.1
    private URI getResourceId(String uri) throws IOException, DavException, URISyntaxException {
        DavPropertyNameSet names = new DavPropertyNameSet();
        names.add(BindConstants.RESOURCEID);
        PropFindMethod propfind = new PropFindMethod(uri, names, 0);
        int status = this.client.executeMethod(propfind);
        assertEquals(207, status);
        MultiStatus multistatus = propfind.getResponseBodyAsMultiStatus();
        MultiStatusResponse[] responses = multistatus.getResponses();
        assertEquals(1, responses.length);
        DavProperty resourceId = responses[0].getProperties(200).get(BindConstants.RESOURCEID);
        assertNotNull(resourceId);
        assertTrue(resourceId.getValue() instanceof Element);
        Element href = (Element)resourceId.getValue();
        assertEquals("href", href.getLocalName());
        String text = getUri(href);
        URI resid = new URI(text);
        return resid;
    }

    private DavProperty getParentSet(String uri) throws IOException, DavException, URISyntaxException {
        DavPropertyNameSet names = new DavPropertyNameSet();
        names.add(BindConstants.PARENTSET);
        PropFindMethod propfind = new PropFindMethod(uri, names, 0);
        int status = this.client.executeMethod(propfind);
        assertEquals(207, status);
        MultiStatus multistatus = propfind.getResponseBodyAsMultiStatus();
        MultiStatusResponse[] responses = multistatus.getResponses();
        assertEquals(1, responses.length);
        DavProperty parentset = responses[0].getProperties(200).get(BindConstants.PARENTSET);
        assertNotNull(parentset);
        return parentset;
    }

    public void testSimpleBind() throws Exception {
        String testcol = this.root + "testSimpleBind/";
        String subcol1 = testcol + "bindtest1/";
        String testres1 = subcol1 + "res1";
        String subcol2 = testcol + "bindtest2/";
        String testres2 = subcol2 + "res2";
        int status;
        try {
            MkColMethod mkcol = new MkColMethod(testcol);
            status = this.client.executeMethod(mkcol);
            assertEquals(201, status);
            mkcol = new MkColMethod(subcol1);
            status = this.client.executeMethod(mkcol);
            assertEquals(201, status);
            mkcol = new MkColMethod(subcol2);
            status = this.client.executeMethod(mkcol);
            assertEquals(201, status);

            //create new resource R with path bindtest1/res1
            PutMethod put = new PutMethod(testres1);
            put.setRequestEntity(new StringRequestEntity("foo", "text/plain", "UTF-8"));
            status = this.client.executeMethod(put);
            assertEquals(201, status);

            //create new binding of R with path bindtest2/res2
            DavMethodBase bind = new BindMethod(subcol2, new BindInfo(testres1, "res2"));
            status = this.client.executeMethod(bind);
            assertEquals(201, status);
            //check if both bindings report the same DAV:resource-id
            assertEquals(this.getResourceId(testres1), this.getResourceId(testres2));

            //compare representations retrieved with both paths
            GetMethod get = new GetMethod(testres1);
            status = this.client.executeMethod(get);
            assertEquals(200, status);
            assertEquals("foo", get.getResponseBodyAsString());
            get = new GetMethod(testres2);
            status = this.client.executeMethod(get);
            assertEquals(200, status);
            assertEquals("foo", get.getResponseBodyAsString());

            //modify R using the new path
            put = new PutMethod(testres2);
            put.setRequestEntity(new StringRequestEntity("bar", "text/plain", "UTF-8"));
            status = this.client.executeMethod(put);
            assertTrue("status: " + status, status == 200 || status == 204);

            //compare representations retrieved with both paths
            get = new GetMethod(testres1);
            status = this.client.executeMethod(get);
            assertEquals(200, status);
            assertEquals("bar", get.getResponseBodyAsString());
            get = new GetMethod(testres2);
            status = this.client.executeMethod(get);
            assertEquals(200, status);
            assertEquals("bar", get.getResponseBodyAsString());
        } finally {
            DeleteMethod delete = new DeleteMethod(testcol);
            status = this.client.executeMethod(delete);
            assertTrue("status: " + status, status == 200 || status == 204);
        }
    }

    public void testRebind() throws Exception {
        String testcol = this.root + "testRebind/";
        String subcol1 = testcol + "bindtest1/";
        String testres1 = subcol1 + "res1";
        String subcol2 = testcol + "bindtest2/";
        String testres2 = subcol2 + "res2";
        int status;
        try {
            MkColMethod mkcol = new MkColMethod(testcol);
            status = this.client.executeMethod(mkcol);
            assertEquals(201, status);
            mkcol = new MkColMethod(subcol1);
            status = this.client.executeMethod(mkcol);
            assertEquals(201, status);
            mkcol = new MkColMethod(subcol2);
            status = this.client.executeMethod(mkcol);
            assertEquals(201, status);

            //create new resource R with path bindtest1/res1
            PutMethod put = new PutMethod(testres1);
            put.setRequestEntity(new StringRequestEntity("foo", "text/plain", "UTF-8"));
            status = this.client.executeMethod(put);
            assertEquals(201, status);

            // enabling version control always makes the resource referenceable
            VersionControlMethod versioncontrol = new VersionControlMethod(testres1);
            status = this.client.executeMethod(versioncontrol);
            assertTrue("status: " + status, status == 200 || status == 201);

            URI r1 = this.getResourceId(testres1);

            GetMethod get = new GetMethod(testres1);
            status = this.client.executeMethod(get);
            assertEquals(200, status);
            assertEquals("foo", get.getResponseBodyAsString());

            //rebind R with path bindtest2/res2
            DavMethodBase rebind = new RebindMethod(subcol2, new RebindInfo(testres1, "res2"));
            status = this.client.executeMethod(rebind);
            assertEquals(201, status);

            URI r2 = this.getResourceId(testres2);

            get = new GetMethod(testres2);
            status = this.client.executeMethod(get);
            assertEquals(200, status);
            assertEquals("foo", get.getResponseBodyAsString());

            //make sure that rebind did not change the resource-id
            assertEquals(r1, r2);

            //verify that the initial binding is gone
            HeadMethod head = new HeadMethod(testres1);
            status = this.client.executeMethod(head);
            assertEquals(404, status);
        } finally {
            DeleteMethod delete = new DeleteMethod(testcol);
            status = this.client.executeMethod(delete);
            assertTrue("status: " + status, status == 200 || status == 204);
        }
    }

    public void testBindOverwrite() throws Exception {
        String testcol = this.root + "testSimpleBind/";
        String subcol1 = testcol + "bindtest1/";
        String testres1 = subcol1 + "res1";
        String subcol2 = testcol + "bindtest2/";
        String testres2 = subcol2 + "res2";
        int status;
        try {
            MkColMethod mkcol = new MkColMethod(testcol);
            status = this.client.executeMethod(mkcol);
            assertEquals(201, status);
            mkcol = new MkColMethod(subcol1);
            status = this.client.executeMethod(mkcol);
            assertEquals(201, status);
            mkcol = new MkColMethod(subcol2);
            status = this.client.executeMethod(mkcol);
            assertEquals(201, status);

            //create new resource R with path bindtest1/res1
            PutMethod put = new PutMethod(testres1);
            put.setRequestEntity(new StringRequestEntity("foo", "text/plain", "UTF-8"));
            status = this.client.executeMethod(put);
            assertEquals(201, status);

            //create new resource R' with path bindtest2/res2
            put = new PutMethod(testres2);
            put.setRequestEntity(new StringRequestEntity("bar", "text/plain", "UTF-8"));
            status = this.client.executeMethod(put);
            assertEquals(201, status);

            //try to create new binding of R with path bindtest2/res2 and Overwrite:F
            DavMethodBase bind = new BindMethod(subcol2, new BindInfo(testres1, "res2"));
            bind.addRequestHeader(new Header("Overwrite", "F"));
            status = this.client.executeMethod(bind);
            assertEquals(412, status);

            //verify that bindtest2/res2 still points to R'
            GetMethod get = new GetMethod(testres2);
            status = this.client.executeMethod(get);
            assertEquals(200, status);
            assertEquals("bar", get.getResponseBodyAsString());

            //create new binding of R with path bindtest2/res2
            bind = new BindMethod(subcol2, new BindInfo(testres1, "res2"));
            status = this.client.executeMethod(bind);
            assertTrue("status: " + status, status == 200 || status == 204);

            //verify that bindtest2/res2 now points to R
            get = new GetMethod(testres2);
            status = this.client.executeMethod(get);
            assertEquals(200, status);
            assertEquals("foo", get.getResponseBodyAsString());

            //verify that the initial binding is still there
            HeadMethod head = new HeadMethod(testres1);
            status = this.client.executeMethod(head);
            assertEquals(200, status);
        } finally {
            DeleteMethod delete = new DeleteMethod(testcol);
            status = this.client.executeMethod(delete);
            assertTrue("status: " + status, status == 200 || status == 204);
        }
    }

    public void testRebindOverwrite() throws Exception {
        String testcol = this.root + "testSimpleBind/";
        String subcol1 = testcol + "bindtest1/";
        String testres1 = subcol1 + "res1";
        String subcol2 = testcol + "bindtest2/";
        String testres2 = subcol2 + "res2";
        int status;
        try {
            MkColMethod mkcol = new MkColMethod(testcol);
            status = this.client.executeMethod(mkcol);
            assertEquals(201, status);
            mkcol = new MkColMethod(subcol1);
            status = this.client.executeMethod(mkcol);
            assertEquals(201, status);
            mkcol = new MkColMethod(subcol2);
            status = this.client.executeMethod(mkcol);
            assertEquals(201, status);

            //create new resource R with path testSimpleBind/bindtest1/res1
            PutMethod put = new PutMethod(testres1);
            put.setRequestEntity(new StringRequestEntity("foo", "text/plain", "UTF-8"));
            status = this.client.executeMethod(put);
            assertEquals(201, status);

            // enabling version control always makes the resource referenceable
            VersionControlMethod versioncontrol = new VersionControlMethod(testres1);
            status = this.client.executeMethod(versioncontrol);
            assertTrue("status: " + status, status == 200 || status == 201);

            //create new resource R' with path testSimpleBind/bindtest2/res2
            put = new PutMethod(testres2);
            put.setRequestEntity(new StringRequestEntity("bar", "text/plain", "UTF-8"));
            status = this.client.executeMethod(put);
            assertEquals(201, status);

            //try rebind R with path testSimpleBind/bindtest2/res2 and Overwrite:F
            RebindMethod rebind = new RebindMethod(subcol2, new RebindInfo(testres1, "res2"));
            rebind.addRequestHeader(new Header("Overwrite", "F"));
            status = this.client.executeMethod(rebind);
            assertEquals(412, status);

            //verify that testSimpleBind/bindtest2/res2 still points to R'
            GetMethod get = new GetMethod(testres2);
            status = this.client.executeMethod(get);
            assertEquals(200, status);
            assertEquals("bar", get.getResponseBodyAsString());

            //rebind R with path testSimpleBind/bindtest2/res2
            rebind = new RebindMethod(subcol2, new RebindInfo(testres1, "res2"));
            status = this.client.executeMethod(rebind);
            assertTrue("status: " + status, status == 200 || status == 204);

            //verify that testSimpleBind/bindtest2/res2 now points to R
            get = new GetMethod(testres2);
            status = this.client.executeMethod(get);
            assertEquals(200, status);
            assertEquals("foo", get.getResponseBodyAsString());

            //verify that the initial binding is gone
            HeadMethod head = new HeadMethod(testres1);
            status = this.client.executeMethod(head);
            assertEquals(404, status);
        } finally {
            DeleteMethod delete = new DeleteMethod(testcol);
            status = this.client.executeMethod(delete);
            assertTrue("status: " + status, status == 200 || status == 204);
        }
    }

    public void testParentSet() throws Exception {
        String testcol = this.root + "testParentSet/";
        String subcol1 = testcol + "bindtest1/";
        String testres1 = subcol1 + "res1";
        String subcol2 = testcol + "bindtest2/";
        String testres2 = subcol2 + "res2";
        int status;
        try {
            MkColMethod mkcol = new MkColMethod(testcol);
            status = this.client.executeMethod(mkcol);
            assertEquals(201, status);
            mkcol = new MkColMethod(subcol1);
            status = this.client.executeMethod(mkcol);
            assertEquals(201, status);
            mkcol = new MkColMethod(subcol2);
            status = this.client.executeMethod(mkcol);
            assertEquals(201, status);

            //create new resource R with path testSimpleBind/bindtest1/res1
            PutMethod put = new PutMethod(testres1);
            put.setRequestEntity(new StringRequestEntity("foo", "text/plain", "UTF-8"));
            status = this.client.executeMethod(put);
            assertEquals(201, status);

            //create new binding of R with path testSimpleBind/bindtest2/res2
            DavMethodBase bind = new BindMethod(subcol2, new BindInfo(testres1, "res2"));
            status = this.client.executeMethod(bind);
            assertEquals(201, status);
            //check if both bindings report the same DAV:resource-id
            assertEquals(this.getResourceId(testres1), this.getResourceId(testres2));

            //verify values of parent-set properties
            List hrefs1 = new ArrayList();
            List segments1 = new ArrayList();
            List hrefs2 = new ArrayList();
            List segments2 = new ArrayList();
            Object ps1 = this.getParentSet(testres1).getValue();
            Object ps2 = this.getParentSet(testres2).getValue();
            assertTrue(ps1 instanceof List);
            assertTrue(ps2 instanceof List);
            List plist1 = (List) ps1;
            List plist2 = (List) ps2;
            assertEquals(2, plist1.size());
            assertEquals(2, plist2.size());
            for (int k = 0; k < 2; k++) {
                Object pObj1 = plist1.get(k);
                Object pObj2 = plist2.get(k);
                assertTrue(pObj1 instanceof Element);
                assertTrue(pObj2 instanceof Element);
                ParentElement p1 = ParentElement.createFromXml((Element) pObj1);
                ParentElement p2 = ParentElement.createFromXml((Element) pObj2);
                hrefs1.add(p1.getHref());
                hrefs2.add(p2.getHref());
                segments1.add(p1.getSegment());
                segments2.add(p2.getSegment());
            }
            Collections.sort(hrefs1);
            Collections.sort(hrefs2);
            Collections.sort(segments1);
            Collections.sort(segments2);
            assertEquals(hrefs1, hrefs2);
            assertEquals(segments1, segments2);
        } finally {
            DeleteMethod delete = new DeleteMethod(testcol);
            status = this.client.executeMethod(delete);
            assertTrue("status: " + status, status == 200 || status == 204);
        }
    }

    public void testBindCollections() throws Exception {
        String testcol = this.root + "testBindCollections/";
        String a1 = testcol + "a1/";
        String b1 = a1 + "b1/";
        String c1 = b1 + "c1/";
        String x1 = c1 + "x1";
        String a2 = testcol + "a2/";
        String b2 = a2 + "b2/";
        String c2 = b2 + "c2/";
        String x2 = c2 + "x2";
        int status;
        try {
            MkColMethod mkcol = new MkColMethod(testcol);
            status = this.client.executeMethod(mkcol);
            assertEquals(201, status);
            mkcol = new MkColMethod(a1);
            status = this.client.executeMethod(mkcol);
            assertEquals(201, status);
            mkcol = new MkColMethod(a2);
            status = this.client.executeMethod(mkcol);
            assertEquals(201, status);

            //create collection resource C
            mkcol = new MkColMethod(b1);
            status = this.client.executeMethod(mkcol);
            assertEquals(201, status);
            mkcol = new MkColMethod(c1);
            status = this.client.executeMethod(mkcol);
            assertEquals(201, status);

            //create plain resource R
            PutMethod put = new PutMethod(x1);
            put.setRequestEntity(new StringRequestEntity("foo", "text/plain", "UTF-8"));
            status = this.client.executeMethod(put);
            assertEquals(201, status);

            //create new binding of C with path a2/b2
            DavMethodBase bind = new BindMethod(a2, new BindInfo(b1, "b2"));
            status = this.client.executeMethod(bind);
            assertEquals(201, status);
            //check if both bindings report the same DAV:resource-id
            assertEquals(this.getResourceId(b1), this.getResourceId(b2));

            mkcol = new MkColMethod(c2);
            status = this.client.executeMethod(mkcol);
            assertEquals(201, status);

            //create new binding of R with path a2/b2/c2/r2
            bind = new BindMethod(c2, new BindInfo(x1, "x2"));
            status = this.client.executeMethod(bind);
            assertEquals(201, status);
            //check if both bindings report the same DAV:resource-id
            assertEquals(this.getResourceId(x1), this.getResourceId(x2));

            //verify different path alternatives
            URI rid = this.getResourceId(x1);
            assertEquals(rid, this.getResourceId(x2));
            assertEquals(rid, this.getResourceId(testcol + "a2/b2/c1/x1"));
            assertEquals(rid, this.getResourceId(testcol + "a1/b1/c2/x2"));
            Object ps = this.getParentSet(x1).getValue();
            assertTrue(ps instanceof List);
            assertEquals(2, ((List) ps).size());
            ps = this.getParentSet(x2).getValue();
            assertTrue(ps instanceof List);
            assertEquals(2, ((List) ps).size());
        } finally {
            DeleteMethod delete = new DeleteMethod(testcol);
            status = this.client.executeMethod(delete);
            assertTrue("status: " + status, status == 200 || status == 204);
        }
    }

    //will fail until <https://issues.apache.org/jira/browse/JCR-1773> is fixed
    public void testUnbind() throws Exception {
        String testcol = this.root + "testUnbind/";
        String subcol1 = testcol + "bindtest1/";
        String testres1 = subcol1 + "res1";
        String subcol2 = testcol + "bindtest2/";
        String testres2 = subcol2 + "res2";
        int status;
        try {
            MkColMethod mkcol = new MkColMethod(testcol);
            status = this.client.executeMethod(mkcol);
            assertEquals(201, status);
            mkcol = new MkColMethod(subcol1);
            status = this.client.executeMethod(mkcol);
            assertEquals(201, status);
            mkcol = new MkColMethod(subcol2);
            status = this.client.executeMethod(mkcol);
            assertEquals(201, status);

            //create new resource R with path testSimpleBind/bindtest1/res1
            PutMethod put = new PutMethod(testres1);
            put.setRequestEntity(new StringRequestEntity("foo", "text/plain", "UTF-8"));
            status = this.client.executeMethod(put);
            assertEquals(201, status);

            //create new binding of R with path testSimpleBind/bindtest2/res2
            DavMethodBase bind = new BindMethod(subcol2, new BindInfo(testres1, "res2"));
            status = this.client.executeMethod(bind);
            assertEquals(201, status);
            //check if both bindings report the same DAV:resource-id
            assertEquals(this.getResourceId(testres1), this.getResourceId(testres2));

            //remove new path
            UnbindMethod unbind = new UnbindMethod(subcol2, new UnbindInfo("res2"));
            status = this.client.executeMethod(unbind);
            assertTrue("status: " + status, status == 200 || status == 204);

            //verify that the new binding is gone
            HeadMethod head = new HeadMethod(testres2);
            status = this.client.executeMethod(head);
            assertEquals(404, status);

            //verify that the initial binding is still there
            head = new HeadMethod(testres1);
            status = this.client.executeMethod(head);
            assertEquals(200, status);
        } finally {
            DeleteMethod delete = new DeleteMethod(testcol);
            status = this.client.executeMethod(delete);
            assertTrue("status: " + status, status == 200 || status == 204);
        }
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
}
