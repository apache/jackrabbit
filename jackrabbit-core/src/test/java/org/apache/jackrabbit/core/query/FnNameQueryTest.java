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
package org.apache.jackrabbit.core.query;

import javax.jcr.RepositoryException;
import javax.jcr.Node;

/**
 * <code>FnNameQueryTest</code> tests queries with fn:name() functions.
 */
public class FnNameQueryTest extends AbstractQueryTest {

    public void testSimple() throws RepositoryException {
        Node n1 = testRootNode.addNode(nodeName1);
        n1.setProperty(propertyName1, 1);
        Node n2 = testRootNode.addNode(nodeName2);
        n2.setProperty(propertyName1, 2);
        Node n3 = testRootNode.addNode(nodeName3);
        n3.setProperty(propertyName1, 3);

        superuser.save();

        String base = testPath + "/*[@" + propertyName1;
        executeXPathQuery(base + " = 1 and fn:name() = '" + nodeName1 + "']",
                new Node[]{n1});
        executeXPathQuery(base + " = 1 and fn:name() = '" + nodeName2 + "']",
                new Node[]{});
        executeXPathQuery(base + " > 0 and fn:name() = '" + nodeName2 + "']",
                new Node[]{n2});
        executeXPathQuery(base + " > 0 and (fn:name() = '" + nodeName1 +
                "' or fn:name() = '" + nodeName2 + "')]", new Node[]{n1, n2});
        executeXPathQuery(base + " > 0 and not(fn:name() = '" + nodeName1 + "')]",
                new Node[]{n2, n3});
    }

    public void testWithSpace() throws RepositoryException {
        Node n1 = testRootNode.addNode("My Documents");
        n1.setProperty(propertyName1, 1);

        superuser.save();

        String base = testPath + "/*[@" + propertyName1;
        executeXPathQuery(base + " = 1 and fn:name() = 'My Documents']",
                new Node[]{});
        executeXPathQuery(base + " = 1 and fn:name() = 'My_x0020_Documents']",
                new Node[]{n1});
    }

    public void testLikeWithWildcard() throws RepositoryException {
        Node n1 = testRootNode.addNode("Foo");
        n1.setProperty(propertyName1, 1);

        superuser.save();

        String prefix = testPath + "/*[@" + propertyName1 + " = 1 and jcr:like(fn:name(), '";
        String suffix = "')]";
        executeXPathQuery(prefix + "F%" + suffix, new Node[]{n1});
        executeXPathQuery(prefix + "Fo%" + suffix, new Node[]{n1});
        executeXPathQuery(prefix + "Foo%" + suffix, new Node[]{n1});
        executeXPathQuery(prefix + "Fooo%" + suffix, new Node[]{});
        executeXPathQuery(prefix + "%o" + suffix, new Node[]{n1});
        executeXPathQuery(prefix + "%oo" + suffix, new Node[]{n1});
        executeXPathQuery(prefix + "%Foo" + suffix, new Node[]{n1});
        executeXPathQuery(prefix + "%Foo%" + suffix, new Node[]{n1});
        executeXPathQuery(prefix + "%oo%" + suffix, new Node[]{n1});
        executeXPathQuery(prefix + "%" + suffix, new Node[]{n1});

        executeXPathQuery(prefix + "F__" + suffix, new Node[]{n1});
        executeXPathQuery(prefix + "Fo_" + suffix, new Node[]{n1});
        executeXPathQuery(prefix + "F_o" + suffix, new Node[]{n1});
        executeXPathQuery(prefix + "Foo_" + suffix, new Node[]{});
    }

    public void testLikeWithWildcardAndLowerCase()
            throws RepositoryException {
        Node n1 = testRootNode.addNode("Foo");
        n1.setProperty(propertyName1, 1);

        superuser.save();

        String prefix = testPath + "/*[@" + propertyName1 + " = 1 and jcr:like(fn:lower-case(fn:name()), '";
        String suffix = "')]";

        executeXPathQuery(prefix + "f%" + suffix, new Node[]{n1});
        executeXPathQuery(prefix + "fo%" + suffix, new Node[]{n1});
        executeXPathQuery(prefix + "foo%" + suffix, new Node[]{n1});
        executeXPathQuery(prefix + "fooo%" + suffix, new Node[]{});
        executeXPathQuery(prefix + "%o" + suffix, new Node[]{n1});
        executeXPathQuery(prefix + "%oo" + suffix, new Node[]{n1});
        executeXPathQuery(prefix + "%foo" + suffix, new Node[]{n1});
        executeXPathQuery(prefix + "%foo%" + suffix, new Node[]{n1});
        executeXPathQuery(prefix + "%oo%" + suffix, new Node[]{n1});

        executeXPathQuery(prefix + "f__" + suffix, new Node[]{n1});
        executeXPathQuery(prefix + "fo_" + suffix, new Node[]{n1});
        executeXPathQuery(prefix + "f_o" + suffix, new Node[]{n1});
        executeXPathQuery(prefix + "_oo" + suffix, new Node[]{n1});
        executeXPathQuery(prefix + "foo_" + suffix, new Node[]{});

        // all non-matching
        executeXPathQuery(prefix + "F%" + suffix, new Node[]{});
        executeXPathQuery(prefix + "fO%" + suffix, new Node[]{});
        executeXPathQuery(prefix + "foO%" + suffix, new Node[]{});
        executeXPathQuery(prefix + "%O" + suffix, new Node[]{});
        executeXPathQuery(prefix + "%Oo" + suffix, new Node[]{});
        executeXPathQuery(prefix + "%Foo" + suffix, new Node[]{});
        executeXPathQuery(prefix + "%FOO%" + suffix, new Node[]{});
        executeXPathQuery(prefix + "%oO%" + suffix, new Node[]{});

        executeXPathQuery(prefix + "F__" + suffix, new Node[]{});
        executeXPathQuery(prefix + "fO_" + suffix, new Node[]{});
        executeXPathQuery(prefix + "F_o" + suffix, new Node[]{});
        executeXPathQuery(prefix + "_oO" + suffix, new Node[]{});
    }

    public void testLikeWithPrefix() throws RepositoryException {
        Node n1 = testRootNode.addNode("jcr:content");
        n1.setProperty(propertyName1, 1);

        superuser.save();

        String prefix = testPath + "/*[@" + propertyName1 + " = 1 and jcr:like(fn:name(), '";
        String suffix = "')]";

        executeXPathQuery(prefix + "jcr:%" + suffix, new Node[]{n1});
        executeXPathQuery(prefix + "jcr:c%" + suffix, new Node[]{n1});
        executeXPathQuery(prefix + "jcr:%ten%" + suffix, new Node[]{n1});
        executeXPathQuery(prefix + "jcr:c_nt%" + suffix, new Node[]{n1});
        executeXPathQuery(prefix + "jcr:%nt" + suffix, new Node[]{n1});

        // non-matching
        executeXPathQuery(prefix + "invalid:content" + suffix, new Node[]{});
        executeXPathQuery(prefix + "%:content" + suffix, new Node[]{});
        executeXPathQuery(prefix + "%:%" + suffix, new Node[]{});
    }
}
