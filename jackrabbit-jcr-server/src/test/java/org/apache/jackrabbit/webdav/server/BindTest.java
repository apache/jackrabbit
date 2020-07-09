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
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.bind.BindConstants;
import org.apache.jackrabbit.webdav.bind.BindInfo;
import org.apache.jackrabbit.webdav.bind.ParentElement;
import org.apache.jackrabbit.webdav.bind.RebindInfo;
import org.apache.jackrabbit.webdav.bind.UnbindInfo;
import org.apache.jackrabbit.webdav.client.methods.HttpBind;
import org.apache.jackrabbit.webdav.client.methods.HttpMkcol;
import org.apache.jackrabbit.webdav.client.methods.HttpMove;
import org.apache.jackrabbit.webdav.client.methods.HttpOptions;
import org.apache.jackrabbit.webdav.client.methods.HttpPropfind;
import org.apache.jackrabbit.webdav.client.methods.HttpRebind;
import org.apache.jackrabbit.webdav.client.methods.HttpUnbind;
import org.apache.jackrabbit.webdav.client.methods.HttpVersionControl;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Test cases for WebDAV BIND functionality (see <a href="http://greenbytes.de/tech/webdav/rfc5842.html">RFC 5842</a>
 */
public class BindTest extends WebDAVTestBase {

    // http://greenbytes.de/tech/webdav/rfc5842.html#rfc.section.8.1
    public void testOptions() throws IOException {
        HttpOptions options = new HttpOptions(this.uri);
        HttpResponse response = this.client.execute(options, this.context);
        int status = response.getStatusLine().getStatusCode();
        assertEquals(200, status);
        Set<String> allow = options.getAllowedMethods(response);
        Set<String> complianceClasses = options.getDavComplianceClasses(response);
        assertTrue("DAV header should include 'bind' feature", complianceClasses.contains("bind"));
        assertTrue("Allow header should include BIND method", allow.contains("BIND"));
        assertTrue("Allow header should include REBIND method", allow.contains("REBIND"));
        assertTrue("Allow header should include UNBIND method", allow.contains("UNBIND"));
    }

    // create test resource, make it referenceable, check resource id, move resource, check again
    public void testResourceId() throws IOException, DavException, URISyntaxException {

        String testcol = this.root + "testResourceId/";
        String testuri1 = testcol + "bindtest1";
        String testuri2 = testcol + "bindtest2";
        int status;
        try {
            HttpMkcol mkcol = new HttpMkcol(testcol);
            status = this.client.execute(mkcol, this.context).getStatusLine().getStatusCode();
            assertEquals(201, status);

            HttpPut put = new HttpPut(testuri1);
            put.setEntity(new StringEntity("foo", ContentType.create("text/plain", "UTF-8")));
            status = this.client.execute(put, this.context).getStatusLine().getStatusCode();
            assertEquals(201, status);

            // enabling version control always makes the resource referenceable
            HttpVersionControl versioncontrol = new HttpVersionControl(testuri1);
            status = this.client.execute(versioncontrol, this.context).getStatusLine().getStatusCode();
            assertTrue("status: " + status, status == 200 || status == 201);

            URI resourceId = getResourceId(testuri1);

            HttpMove move = new HttpMove(testuri1, testuri2, true);
            status = this.client.execute(move, this.context).getStatusLine().getStatusCode();
            assertEquals(201, status);

            URI resourceId2 = getResourceId(testuri2);
            assertEquals(resourceId, resourceId2);
        } finally {
            delete(testcol);
        }
    }

    // utility methods

    // see http://greenbytes.de/tech/webdav/rfc5842.html#rfc.section.3.1
    private URI getResourceId(String uri) throws IOException, DavException, URISyntaxException {
        DavPropertyNameSet names = new DavPropertyNameSet();
        names.add(BindConstants.RESOURCEID);
        HttpPropfind propfind = new HttpPropfind(uri, names, 0);
        HttpResponse response = this.client.execute(propfind, this.context);
        int status = response.getStatusLine().getStatusCode();
        assertEquals(207, status);
        MultiStatus multistatus = propfind.getResponseBodyAsMultiStatus(response);
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
        HttpPropfind propfind = new HttpPropfind(uri, names, 0);
        HttpResponse response = this.client.execute(propfind, this.context);
        int status = response.getStatusLine().getStatusCode();
        assertEquals(207, status);
        MultiStatus multistatus = propfind.getResponseBodyAsMultiStatus(response);
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
            HttpMkcol mkcol = new HttpMkcol(testcol);
            status = this.client.execute(mkcol, this.context).getStatusLine().getStatusCode();
            assertEquals(201, status);
            mkcol = new HttpMkcol(subcol1);
            status = this.client.execute(mkcol, this.context).getStatusLine().getStatusCode();
            assertEquals(201, status);
            mkcol = new HttpMkcol(subcol2);
            status = this.client.execute(mkcol, this.context).getStatusLine().getStatusCode();
            assertEquals(201, status);

            //create new resource R with path bindtest1/res1
            HttpPut put = new HttpPut(testres1);
            put.setEntity(new StringEntity("foo", ContentType.create("text/plain", "UTF-8")));
            status = this.client.execute(put, this.context).getStatusLine().getStatusCode();
            assertEquals(201, status);

            //create new binding of R with path bindtest2/res2
            HttpBind bind = new HttpBind(subcol2, new BindInfo(testres1, "res2"));
            status = this.client.execute(bind, this.context).getStatusLine().getStatusCode();
            assertEquals(201, status);
            //check if both bindings report the same DAV:resource-id
            assertEquals(this.getResourceId(testres1), this.getResourceId(testres2));

            //compare representations retrieved with both paths
            HttpGet get = new HttpGet(testres1);
            HttpResponse resp = this.client.execute(get, this.context);
            status = resp.getStatusLine().getStatusCode();
            assertEquals(200, status);
            assertEquals("foo", EntityUtils.toString(resp.getEntity()));
            resp = this.client.execute(get, this.context);
            status = resp.getStatusLine().getStatusCode();
            assertEquals(200, status);
            assertEquals("foo", EntityUtils.toString(resp.getEntity()));

            //modify R using the new path
            put = new HttpPut(testres2);
            put.setEntity(new StringEntity("bar", ContentType.create("text/plain", "UTF-8")));
            status = this.client.execute(put, this.context).getStatusLine().getStatusCode();
            assertTrue("status: " + status, status == 200 || status == 204);

            //compare representations retrieved with both paths
            get = new HttpGet(testres1);
            resp = this.client.execute(get, this.context);
            status = resp.getStatusLine().getStatusCode();
            assertEquals(200, status);
            assertEquals("bar", EntityUtils.toString(resp.getEntity()));
            get = new HttpGet(testres2);
            resp = this.client.execute(get, this.context);
            status = resp.getStatusLine().getStatusCode();
            assertEquals(200, status);
            assertEquals("bar", EntityUtils.toString(resp.getEntity()));
        } finally {
            delete(testcol);
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
            HttpMkcol mkcol = new HttpMkcol(testcol);
            status = this.client.execute(mkcol, this.context).getStatusLine().getStatusCode();
            assertEquals(201, status);
            mkcol =  new HttpMkcol(subcol1);
            status = this.client.execute(mkcol, this.context).getStatusLine().getStatusCode();
            assertEquals(201, status);
            mkcol =  new HttpMkcol(subcol2);
            status = this.client.execute(mkcol, this.context).getStatusLine().getStatusCode();
            assertEquals(201, status);

            //create new resource R with path bindtest1/res1
            HttpPut put = new HttpPut(testres1);
            put.setEntity(new StringEntity("foo", ContentType.create("text/plain", "UTF-8")));
            status = this.client.execute(put, this.context).getStatusLine().getStatusCode();
            assertEquals(201, status);

            // enabling version control always makes the resource referenceable
            HttpVersionControl versioncontrol = new HttpVersionControl(testres1);
            status = this.client.execute(versioncontrol, this.context).getStatusLine().getStatusCode();
            assertTrue("status: " + status, status == 200 || status == 201);

            URI r1 = this.getResourceId(testres1);

            HttpGet get = new HttpGet(testres1);
            HttpResponse resp = this.client.execute(get, this.context);
            status = resp.getStatusLine().getStatusCode();
            assertEquals(200, status);
            assertEquals("foo", EntityUtils.toString(resp.getEntity()));

            //rebind R with path bindtest2/res2
            HttpRebind rebind = new HttpRebind(subcol2, new RebindInfo(testres1, "res2"));
            status = this.client.execute(rebind, this.context).getStatusLine().getStatusCode();
            assertEquals(201, status);

            URI r2 = this.getResourceId(testres2);

            get = new HttpGet(testres2);
            resp = this.client.execute(get, this.context);
            status = resp.getStatusLine().getStatusCode();
            assertEquals(200, status);
            assertEquals("foo", EntityUtils.toString(resp.getEntity()));

            //make sure that rebind did not change the resource-id
            assertEquals(r1, r2);

            //verify that the initial binding is gone
            HttpHead head = new HttpHead(testres1);
            status = this.client.execute(head, this.context).getStatusLine().getStatusCode();
            assertEquals(404, status);
        } finally {
            delete(testcol);
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
            HttpMkcol mkcol = new HttpMkcol(testcol);
            status = this.client.execute(mkcol, this.context).getStatusLine().getStatusCode();
            assertEquals(201, status);
            mkcol =  new HttpMkcol(subcol1);
            status = this.client.execute(mkcol, this.context).getStatusLine().getStatusCode();
            assertEquals(201, status);
            mkcol =  new HttpMkcol(subcol2);
            status = this.client.execute(mkcol, this.context).getStatusLine().getStatusCode();
            assertEquals(201, status);

            //create new resource R with path bindtest1/res1
            HttpPut put = new HttpPut(testres1);
            put.setEntity(new StringEntity("foo", ContentType.create("text/plain", "UTF-8")));
            status = this.client.execute(put, this.context).getStatusLine().getStatusCode();
            assertEquals(201, status);

            //create new resource R' with path bindtest2/res2
            put = new HttpPut(testres2);
            put.setEntity(new StringEntity("bar", ContentType.create("text/plain", "UTF-8")));
            status = this.client.execute(put, this.context).getStatusLine().getStatusCode();
            assertEquals(201, status);

            //try to create new binding of R with path bindtest2/res2 and Overwrite:F
            HttpBind bind = new HttpBind(subcol2, new BindInfo(testres1, "res2"));
            bind.addHeader("Overwrite", "F");
            status = this.client.execute(bind, this.context).getStatusLine().getStatusCode();
            assertEquals(412, status);

            //verify that bindtest2/res2 still points to R'
            HttpGet get = new HttpGet(testres2);
            HttpResponse resp = this.client.execute(get, this.context);
            status = resp.getStatusLine().getStatusCode();
            assertEquals(200, status);
            assertEquals("bar", EntityUtils.toString(resp.getEntity()));

            //create new binding of R with path bindtest2/res2
            bind = new HttpBind(subcol2, new BindInfo(testres1, "res2"));
            status = this.client.execute(bind, this.context).getStatusLine().getStatusCode();
            assertTrue("status: " + status, status == 200 || status == 204);

            //verify that bindtest2/res2 now points to R
            get = new HttpGet(testres2);
            resp = this.client.execute(get, this.context);
            status = resp.getStatusLine().getStatusCode();
            assertEquals(200, status);
            assertEquals("foo", EntityUtils.toString(resp.getEntity()));

            //verify that the initial binding is still there
            HttpHead head = new HttpHead(testres1);
            status = this.client.execute(head, this.context).getStatusLine().getStatusCode();
            assertEquals(200, status);
        } finally {
            delete(testcol);
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
            HttpMkcol mkcol = new HttpMkcol(testcol);
            status = this.client.execute(mkcol, this.context).getStatusLine().getStatusCode();
            assertEquals(201, status);
            mkcol =  new HttpMkcol(subcol1);
            status = this.client.execute(mkcol, this.context).getStatusLine().getStatusCode();
            assertEquals(201, status);
            mkcol =  new HttpMkcol(subcol2);
            status = this.client.execute(mkcol, this.context).getStatusLine().getStatusCode();
            assertEquals(201, status);

            //create new resource R with path testSimpleBind/bindtest1/res1
            HttpPut put = new HttpPut(testres1);
            put.setEntity(new StringEntity("foo", ContentType.create("text/plain", "UTF-8")));
            status = this.client.execute(put, this.context).getStatusLine().getStatusCode();
            assertEquals(201, status);

            // enabling version control always makes the resource referenceable
            HttpVersionControl versioncontrol = new HttpVersionControl(testres1);
            status = this.client.execute(versioncontrol, this.context).getStatusLine().getStatusCode();
            assertTrue("status: " + status, status == 200 || status == 201);

            //create new resource R' with path testSimpleBind/bindtest2/res2
            put = new HttpPut(testres2);
            put.setEntity(new StringEntity("bar", ContentType.create("text/plain", "UTF-8")));
            status = this.client.execute(put, this.context).getStatusLine().getStatusCode();
            assertEquals(201, status);

            //try rebind R with path testSimpleBind/bindtest2/res2 and Overwrite:F
            HttpRebind rebind = new HttpRebind(subcol2, new RebindInfo(testres1, "res2"));
            rebind.addHeader("Overwrite", "F");
            status = this.client.execute(rebind, this.context).getStatusLine().getStatusCode();
            assertEquals(412, status);

            //verify that testSimpleBind/bindtest2/res2 still points to R'
            HttpGet get = new HttpGet(testres2);
            HttpResponse resp = this.client.execute(get, this.context);
            status = resp.getStatusLine().getStatusCode();
            assertEquals(200, status);
            assertEquals("bar", EntityUtils.toString(resp.getEntity()));

            //rebind R with path testSimpleBind/bindtest2/res2
            rebind = new HttpRebind(subcol2, new RebindInfo(testres1, "res2"));
            status = this.client.execute(rebind, this.context).getStatusLine().getStatusCode();
            assertTrue("status: " + status, status == 200 || status == 204);

            //verify that testSimpleBind/bindtest2/res2 now points to R
            get =  new HttpGet(testres2);
            resp = this.client.execute(get, this.context);
            status = resp.getStatusLine().getStatusCode();
            assertEquals(200, status);
            assertEquals("foo", EntityUtils.toString(resp.getEntity()));

            //verify that the initial binding is gone
            HttpHead head = new HttpHead(testres1);
            status = this.client.execute(head, this.context).getStatusLine().getStatusCode();
            assertEquals(404, status);
        } finally {
            delete(testcol);
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
            HttpMkcol mkcol = new HttpMkcol(testcol);
            status = this.client.execute(mkcol, this.context).getStatusLine().getStatusCode();
            assertEquals(201, status);
            mkcol =  new HttpMkcol(subcol1);
            status = this.client.execute(mkcol, this.context).getStatusLine().getStatusCode();
            assertEquals(201, status);
            mkcol =  new HttpMkcol(subcol2);
            status = this.client.execute(mkcol, this.context).getStatusLine().getStatusCode();
            assertEquals(201, status);

            //create new resource R with path testSimpleBind/bindtest1/res1
            HttpPut put = new HttpPut(testres1);
            put.setEntity(new StringEntity("foo", ContentType.create("text/plain", "UTF-8")));
            status = this.client.execute(put, this.context).getStatusLine().getStatusCode();
            assertEquals(201, status);

            //create new binding of R with path testSimpleBind/bindtest2/res2
            HttpBind bind = new HttpBind(subcol2, new BindInfo(testres1, "res2"));
            status = this.client.execute(bind, this.context).getStatusLine().getStatusCode();
            assertEquals(201, status);
            //check if both bindings report the same DAV:resource-id
            assertEquals(this.getResourceId(testres1), this.getResourceId(testres2));

            //verify values of parent-set properties
            List<String> hrefs1 = new ArrayList<String>();
            List<String> segments1 = new ArrayList<String>();
            List<String> hrefs2 = new ArrayList<String>();
            List<String> segments2 = new ArrayList<String>();
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
            delete(testcol);
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
            HttpMkcol mkcol = new HttpMkcol(testcol);
            status = this.client.execute(mkcol, this.context).getStatusLine().getStatusCode();
            assertEquals(201, status);
            mkcol =  new HttpMkcol(a1);
            status = this.client.execute(mkcol, this.context).getStatusLine().getStatusCode();
            assertEquals(201, status);
            mkcol =  new HttpMkcol(a2);
            status = this.client.execute(mkcol, this.context).getStatusLine().getStatusCode();
            assertEquals(201, status);

            //create collection resource C
            mkcol =  new HttpMkcol(b1);
            status = this.client.execute(mkcol, this.context).getStatusLine().getStatusCode();
            assertEquals(201, status);
            mkcol =  new HttpMkcol(c1);
            status = this.client.execute(mkcol, this.context).getStatusLine().getStatusCode();
            assertEquals(201, status);

            //create plain resource R
            HttpPut put = new HttpPut(x1);
            put.setEntity(new StringEntity("foo", ContentType.create("text/plain", "UTF-8")));
            status = this.client.execute(put, this.context).getStatusLine().getStatusCode();
            assertEquals(201, status);

            //create new binding of C with path a2/b2
            HttpBind bind = new HttpBind(a2, new BindInfo(b1, "b2"));
            status = this.client.execute(bind, this.context).getStatusLine().getStatusCode();
            assertEquals(201, status);
            //check if both bindings report the same DAV:resource-id
            assertEquals(this.getResourceId(b1), this.getResourceId(b2));

            mkcol =  new HttpMkcol(c2);
            status = this.client.execute(mkcol, this.context).getStatusLine().getStatusCode();
            assertEquals(201, status);

            //create new binding of R with path a2/b2/c2/r2
            bind = new HttpBind(c2, new BindInfo(x1, "x2"));
            status = this.client.execute(bind, this.context).getStatusLine().getStatusCode();
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
            delete(testcol);
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
            HttpMkcol mkcol = new HttpMkcol(testcol);
            status = this.client.execute(mkcol, this.context).getStatusLine().getStatusCode();
            assertEquals(201, status);
            mkcol =  new HttpMkcol(subcol1);
            status = this.client.execute(mkcol, this.context).getStatusLine().getStatusCode();
            assertEquals(201, status);
            mkcol =  new HttpMkcol(subcol2);
            status = this.client.execute(mkcol, this.context).getStatusLine().getStatusCode();
            assertEquals(201, status);

            //create new resource R with path testSimpleBind/bindtest1/res1
            HttpPut put = new HttpPut(testres1);
            put.setEntity(new StringEntity("foo", ContentType.create("text/plain", "UTF-8")));
            status = this.client.execute(put, this.context).getStatusLine().getStatusCode();
            assertEquals(201, status);

            //create new binding of R with path testSimpleBind/bindtest2/res2
            HttpBind bind = new HttpBind(subcol2, new BindInfo(testres1, "res2"));
            status = this.client.execute(bind, this.context).getStatusLine().getStatusCode();
            assertEquals(201, status);
            //check if both bindings report the same DAV:resource-id
            assertEquals(this.getResourceId(testres1), this.getResourceId(testres2));

            //remove new path
            HttpUnbind unbind = new HttpUnbind(subcol2, new UnbindInfo("res2"));
            status = this.client.execute(unbind, this.context).getStatusLine().getStatusCode();
            assertTrue("status: " + status, status == 200 || status == 204);

            //verify that the new binding is gone
            HttpHead head = new HttpHead(testres2);
            status = this.client.execute(head, this.context).getStatusLine().getStatusCode();
            assertEquals(404, status);

            //verify that the initial binding is still there
            head = new HttpHead(testres1);
            status = this.client.execute(head, this.context).getStatusLine().getStatusCode();
            assertEquals(200, status);
        } finally {
            delete(testcol);
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
