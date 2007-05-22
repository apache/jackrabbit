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
package org.apache.jackrabbit.jcrlog.doodle;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.util.TraversingItemVisitor;

import org.apache.jackrabbit.jcrlog.test.unit.TestAPI;

/**
 * Template item visitor.
 *
 * @author Thomas Mueller
 *
 */
public class MyItemVisitor extends TraversingItemVisitor {

    protected void entering(Property property, int level)
            throws RepositoryException {
        TestAPI.println("entering property " + property.getString() + " level "
                + level);
    }

    protected void entering(Node node, int level) throws RepositoryException {
        TestAPI.println("entering node " + node.getName() + " level " + level);
    }

    protected void leaving(Property property, int level)
            throws RepositoryException {
        TestAPI.println("leaving property " + property.getString() + " level "
                + level);
    }

    protected void leaving(Node node, int level) throws RepositoryException {
        TestAPI.println("leaving node " + node.getName() + " level " + level);
    }

}
