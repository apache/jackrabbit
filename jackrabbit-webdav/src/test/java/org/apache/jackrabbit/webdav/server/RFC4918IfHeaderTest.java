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

import junit.framework.TestCase;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.client.methods.DeleteMethod;
import org.apache.jackrabbit.webdav.client.methods.LockMethod;
import org.apache.jackrabbit.webdav.lock.LockInfo;
import org.apache.jackrabbit.webdav.lock.Scope;
import org.apache.jackrabbit.webdav.lock.Type;

/**
 * Test cases for RFC 4918 If header functionality
 * (see <a href="http://www.webdav.org/specs/rfc4918.html#rfc.section.10.4">RFC 4918, Section 10.4</a>
 * <p>
 * Required system properties:
 * <ul>
 *   <li>webdav.test.url</li>
 *   <li>webdav.test.username</li>
 *   <li>webdav.test.password</li>
 * </ul>
 */

public class RFC4918IfHeaderTest extends TestCase {

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
    
    public void testPutIfEtag() throws HttpException, IOException, DavException, URISyntaxException {
  
        String testuri = this.root + "iftest";
    
        int status;
        try {
            PutMethod put = new PutMethod(testuri);
            String condition = "<" + testuri + "> ([" + "\"an-etag-this-testcase-invented\"" + "])";
            put.setRequestEntity(new StringRequestEntity("1"));
            put.setRequestHeader("If", condition);
            status = this.client.executeMethod(put);
            assertEquals("status: " + status, 412, status);
        }
        finally {
            DeleteMethod delete = new DeleteMethod(testuri);
            status = this.client.executeMethod(delete);
            assertTrue("status: " + status, status == 200 || status == 204 || status == 404);
        }
    }

    public void testPutIfLockToken() throws HttpException, IOException, DavException, URISyntaxException {
      
      String testuri = this.root + "iflocktest";
      String locktoken = null;
      
      int status;
      try {
          PutMethod put = new PutMethod(testuri);
          put.setRequestEntity(new StringRequestEntity("1"));
          status = this.client.executeMethod(put);
          assertTrue("status: " + status, status == 200 || status == 201 || status == 204);

          LockMethod lock = new LockMethod(testuri, new LockInfo(Scope.EXCLUSIVE, Type.WRITE, "testcase", 1800, true));
          status = this.client.executeMethod(lock);
          assertEquals("status", 200, status);
          locktoken = lock.getLockToken();
          assertNotNull(locktoken);
          
          // try to overwrite without lock token
          put = new PutMethod(testuri);
          put.setRequestEntity(new StringRequestEntity("2"));
          status = this.client.executeMethod(put);
          assertEquals("status: " + status, 423, status);
          
          // try to overwrite using bad lock token
          put = new PutMethod(testuri);
          put.setRequestEntity(new StringRequestEntity("2"));
          put.setRequestHeader("If", "(<" + "DAV:foobar" + ">)");
          status = this.client.executeMethod(put);
          assertEquals("status: " + status, 412, status);
          
          // try to overwrite using correct lock token, using  No-Tag-list format
          put = new PutMethod(testuri);
          put.setRequestEntity(new StringRequestEntity("2"));
          put.setRequestHeader("If", "(<" + locktoken + ">)");
          status = this.client.executeMethod(put);
          assertTrue("status: " + status, status == 200 || status == 204);

          // try to overwrite using correct lock token, using Tagged-list format
          // and full URI
          put = new PutMethod(testuri);
          put.setRequestEntity(new StringRequestEntity("3"));
          put.setRequestHeader("If", "<" + testuri + ">" + "(<" + locktoken + ">)");
          status = this.client.executeMethod(put);
          assertTrue("status: " + status, status == 200 || status == 204);

          // try to overwrite using correct lock token, using Tagged-list format
          // and absolute path only
          put = new PutMethod(testuri);
          put.setRequestEntity(new StringRequestEntity("4"));
          put.setRequestHeader("If", "<" + new URI(testuri).getRawPath() + ">" + "(<" + locktoken + ">)");
          status = this.client.executeMethod(put);
          assertTrue("status: " + status, status == 200 || status == 204);

          // try to overwrite using correct lock token, using Tagged-list format
          // and bad path
          put = new PutMethod(testuri);
          put.setRequestEntity(new StringRequestEntity("5"));
          put.setRequestHeader("If", "</foobar>" + "(<" + locktoken + ">)");
          status = this.client.executeMethod(put);
          assertTrue("status: " + status, status == 404 || status == 412);
      }
      finally {
          DeleteMethod delete = new DeleteMethod(testuri);
          if (locktoken != null) {
              delete.setRequestHeader("If", "(<" + locktoken + ">)");
          }
          status = this.client.executeMethod(delete);
          assertTrue("status: " + status, status == 200 || status == 204 || status == 404);
      }
  }
  
}
