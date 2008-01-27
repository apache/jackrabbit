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
package org.apache.jackrabbit.ocm.nodemanagement;

import javax.jcr.Session;
import javax.jcr.nodetype.PropertyDefinition;

import junit.framework.TestCase;

import org.apache.jackrabbit.ocm.nodemanagement.impl.RepositoryConfiguration;
import org.apache.jackrabbit.ocm.nodemanagement.impl.RepositorySessionFactory;

/** Base class of JUnit test cases.
 *
 * @author Oliver Kiessler
 */
public class TestBase  extends TestCase
{
    protected static Session session;

    /**
     * Setting up the testcase.
     *
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        if (session == null)
        {
            RepositoryConfiguration configuration = new RepositoryConfiguration();
            configuration.setConfigurationFile("./src/test/config/jackrabbit/repository.xml");
            configuration.setRepositoryName("repositoryTest");
            configuration.setRepositoryPath("./target/repository");
            session = RepositorySessionFactory.getSession(RepositorySessionFactory.JACKRABBIT, "superuser", "superuser", configuration);

            /*RepositoryConfiguration configuration = new RepositoryConfiguration();
            configuration.setConfigurationFile("./src/config/jeceria/jeceira.xml");
            configuration.setRepositoryName("test");
            session = RepositorySessionFactory.getSession(RepositorySessionFactory.JECEIRA, null, null, configuration);*/
        }
    }

    /**
     * @see junit.framework.TestCase#tearDown()
     */
    public void tearDown() throws Exception
    {
    }

    /** Returns true if a property was found in an array of property defintions.
     *
     * @param defintions PropertyDefinition[]
     * @param propertyName Name of property to find
     * @return true/false
     */
    public boolean containsPropertyDefintion(PropertyDefinition[] definitions,
            String propertyName)
    {
        boolean found = false;

        if (definitions != null && definitions.length > 0)
        {
            for (int i = 0; i < definitions.length; i++)
            {
                if (definitions[i].getName().equals(propertyName))
                {
                    found = true;
                }
            }
        }

        return found;
    }
}
