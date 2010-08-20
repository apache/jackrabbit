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
package org.apache.jackrabbit.core.security.user;

import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.spi.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.NodeIterator;

/** <code>IndexNodeResolver</code>... */
public class IndexNodeResolverTest extends NodeResolverTest {

    private static Logger log = LoggerFactory.getLogger(IndexNodeResolver.class);

    @Override
    protected NodeResolver createNodeResolver(SessionImpl session) throws RepositoryException, NotExecutableException {
        return new IndexNodeResolver(session, session);
    }


    /**
     * If query value contains backslash the non-exact findNodes method should
     * return the desired result.
     *
     * @throws NotExecutableException
     * @throws RepositoryException
     */
    public void testFindNodesNonExact() throws NotExecutableException, RepositoryException {
        UserImpl currentUser = getCurrentUser();
        Value vs = superuser.getValueFactory().createValue("value \\, containing backslash");
        currentUser.setProperty(propertyName1, vs);
        save();

        Name propName = ((SessionImpl) superuser).getQName(propertyName1);
        try {
            NodeResolver nr = createNodeResolver(currentUser.getNode().getSession());

            NodeIterator result = nr.findNodes(propName, "value \\, containing backslash", UserConstants.NT_REP_USER, false);
            assertTrue("expected result", result.hasNext());
            assertEquals(currentUser.getNode().getPath(), result.nextNode().getPath());
            assertFalse("expected no more results", result.hasNext());
        } finally {
            currentUser.removeProperty(propertyName1);
            save();
        }
    }

    public void testFindNodesNonExactWithApostrophe()
            throws NotExecutableException, RepositoryException {
        UserImpl currentUser = getCurrentUser();
        Value vs = superuser.getValueFactory().createValue("value ' with apostrophe");
        currentUser.setProperty(propertyName1, vs);
        save();

        Name propName = ((SessionImpl) superuser).getQName(propertyName1);
        try {
            NodeResolver nr = createNodeResolver(currentUser.getNode().getSession());

            NodeIterator result = nr.findNodes(propName, "value ' with apostrophe", UserConstants.NT_REP_USER, false);
            assertTrue("expected result", result.hasNext());
            assertEquals(currentUser.getNode().getPath(), result.nextNode().getPath());
            assertFalse("expected no more results", result.hasNext());
        } finally {
            currentUser.removeProperty(propertyName1);
            save();
        }
    }


    public void testFindNodesExactWithApostrophe()
            throws NotExecutableException, RepositoryException {
        UserImpl currentUser = getCurrentUser();
        Value vs = superuser.getValueFactory().createValue("value ' with apostrophe");
        currentUser.setProperty(propertyName1, vs);
        save();

        Name propName = ((SessionImpl) superuser).getQName(propertyName1);
        try {
            NodeResolver nr = createNodeResolver(currentUser.getNode().getSession());

            NodeIterator result = nr.findNodes(propName, "value ' with apostrophe", UserConstants.NT_REP_USER, true);
            assertTrue("expected result", result.hasNext());
            assertEquals(currentUser.getNode().getPath(), result.nextNode().getPath());
            assertFalse("expected no more results", result.hasNext());
        } finally {
            currentUser.removeProperty(propertyName1);
            save();
        }
    }
}