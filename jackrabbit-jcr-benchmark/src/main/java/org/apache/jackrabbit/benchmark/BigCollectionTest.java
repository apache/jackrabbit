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

package org.apache.jackrabbit.benchmark;

import java.io.BufferedInputStream;
import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import java.util.Calendar;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Several tests for benchmarking the performance when iterating over
 * "big" collections. 
 * <p>
 * Assumes the store supports nt:folder/nt:file/nt:resource below
 * the test root node.
 */
public class BigCollectionTest extends AbstractJCRTest {

  private static final Logger LOG = LoggerFactory.getLogger(BigCollectionTest.class);

  private static int MEMBERS = 500;
  private static int MEMBERSIZE = 1024;
  private static String MIMETYPE = "application/octet-stream";
  private static int MINTIME = 1000;
  private static int MINCOUNT = 5;

  protected void setUp() throws Exception {
      super.setUp();

      Session session = testRootNode.getSession();
      Node folder = null;
      try {
          folder = testRootNode.getNode("bigcoll");
      }
      catch (RepositoryException ex) {
        // nothing to do
      }
        
      // delete when needed
      if (folder != null) {
          folder.remove();
          session.save();
      }
        
      folder = testRootNode.addNode("bigcoll", "nt:folder");

      long cnt = 0;

      while (cnt < MEMBERS) {
          InputStream is = new BufferedInputStream(new ContentGenerator(MEMBERSIZE), MEMBERSIZE);
          Node l_new = folder.addNode("tst" + cnt, "nt:file");
          Node l_cnew = l_new.addNode("jcr:content", "nt:resource");
          l_cnew.setProperty("jcr:data", is);
          l_cnew.setProperty("jcr:mimeType", MIMETYPE);
          l_cnew.setProperty("jcr:lastModified", Calendar.getInstance());
          cnt += 1;
      }
      session.save();
  }

  protected void tearDown() throws Exception {
      try {
          Node folder = testRootNode.getNode("bigcoll");
          folder.remove();
          folder.getSession().save();
      }
      catch (RepositoryException ex) {
          // nothing to do
      }
      super.tearDown();
  }
  
  private void performTest(String testName, boolean getContentNode, boolean getLength) throws RepositoryException {
      Session session = testRootNode.getSession();
      
      long start = System.currentTimeMillis();
      long cnt = 0;
  
      while (System.currentTimeMillis() - start < MINTIME || cnt < MINCOUNT) {
          Node dir = testRootNode.getNode("bigcoll");
          int members = 0;
          for (NodeIterator it = dir.getNodes(); it.hasNext(); ) {
              Node child = it.nextNode();
              Node content = getContentNode ? child.getNode("jcr:content") : null;
              String type = getContentNode ? content.getProperty("jcr:mimeType").getString() : null;
              long length = getLength ? content.getProperty("jcr:data").getLength() : -1;
              assertTrue(child.isNode());
              if (getContentNode) {
                  assertEquals(MIMETYPE, type);
              }
              if (getLength) {
                  assertEquals(MEMBERSIZE, length);
              }
              members += 1;
          }
          assertEquals(MEMBERS, members);
          session.refresh(false);
          cnt += 1;
      }
      
      long elapsed = System.currentTimeMillis() - start;
      
      LOG.info(testName + ": " +  (double)elapsed / cnt + "ms per call (" + cnt + " iterations)");
  }
  
  /**
   * Get all children, but do not visit jcr:content child nodes
   */
  public void testGetChildren() throws RepositoryException {
      performTest("testGetChildren", false, false);
  } 

  /**
   * Get all children and their jcr:content child nodes, but
   * do not visit jcr:data.
   */
  public void testBrowseMinusJcrData() throws RepositoryException {
      performTest("testBrowseMinusJcrData", true, false);
  }

  /**
   * Simulate what a UI usually does on a collection of files:
   * obtain type and length of the files.
   */
  public void testBrowse() throws RepositoryException {
      performTest("testBrowse", true, true);
  }

  /**
   * Generator for test content of a specific length.
   */
  private class ContentGenerator extends InputStream {

      private long length;
      private long position;
  
      public ContentGenerator(long length) {
          this.length = length;
          this.position = 0;
      }
  
      public int read() {
          if (this.position++ < this.length) {
              return 0;
          }
          else {
              return -1;
          }
      }
  }

}
