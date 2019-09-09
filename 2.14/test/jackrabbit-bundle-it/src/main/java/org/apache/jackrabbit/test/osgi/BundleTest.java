/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.test.osgi;

import javax.jcr.Repository;
import javax.jcr.Session;

import junit.framework.Assert;

import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.osgi.framework.ServiceReference;

public class BundleTest extends OSGiTestCase {

    public void testJackrabbitBundle() throws Exception {
        ServiceReference reference =
            getServiceReference(Repository.class.getName());
        Assert.assertNotNull(reference);
        Assert.assertEquals(
                "Jackrabbit",
                reference.getProperty(Repository.REP_NAME_DESC));

        Object service = getServiceObject(reference);
        assertTrue(service instanceof Repository);

        Repository repository = (Repository) service;
        Assert.assertEquals(
                "Jackrabbit",
                repository.getDescriptor(Repository.REP_NAME_DESC));

        Session session = repository.login();
        try {
            assertEquals("/", session.getRootNode().getPath());
        } finally {
            session.logout();
        }
    }

}
