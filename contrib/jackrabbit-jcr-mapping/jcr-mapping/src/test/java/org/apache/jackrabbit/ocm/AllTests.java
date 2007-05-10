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
package org.apache.jackrabbit.ocm;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.jackrabbit.ocm.mapper.DigesterMapperImplTest;
import org.apache.jackrabbit.ocm.querymanager.QueryManagerTest;
import org.apache.jackrabbit.ocm.repository.RepositoryUtilTest;


/**
 * OCM suite definition. Bundles together all independent and package level test suites.
 * 
 * @author <a href='mailto:the_mindstorm[at]evolva[dot]ro'>Alexandru Popescu</a>
 */
public class AllTests {

    public static Test suite() throws Exception {
        return new RepositoryLifecycleTestSetup(buildSuite());
    }

    public static Test buildSuite() throws Exception {
        TestSuite suite= new TestSuite("Graffito OCM Tests");
        suite.addTest(org.apache.jackrabbit.ocm.mapper.AllTests.buildSuite());     
        suite.addTest(org.apache.jackrabbit.ocm.persistence.atomic.AllTests.buildSuite());
        suite.addTest(org.apache.jackrabbit.ocm.persistence.auto.AllTests.buildSuite());
        suite.addTest(org.apache.jackrabbit.ocm.persistence.basic.AllTests.buildSuite());
        suite.addTest(org.apache.jackrabbit.ocm.persistence.beanconverter.AllTests.buildSuite());
        suite.addTest(org.apache.jackrabbit.ocm.persistence.collectionconverter.AllTests.buildSuite());
        suite.addTest(org.apache.jackrabbit.ocm.persistence.inheritance.AllTests.buildSuite());
        suite.addTest(org.apache.jackrabbit.ocm.persistence.interfaces.AllTests.buildSuite());
        suite.addTest(org.apache.jackrabbit.ocm.persistence.jcrnodetype.AllTests.buildSuite());        
        suite.addTest(org.apache.jackrabbit.ocm.persistence.lock.AllTests.buildSuite());        
        suite.addTest(org.apache.jackrabbit.ocm.persistence.proxy.AllTests.buildSuite());
        suite.addTest(org.apache.jackrabbit.ocm.persistence.query.AllTests.buildSuite());
        suite.addTest(org.apache.jackrabbit.ocm.persistence.uuid.AllTests.buildSuite());        
        suite.addTest(org.apache.jackrabbit.ocm.persistence.version.AllTests.buildSuite());        
        suite.addTest(org.apache.jackrabbit.ocm.querymanager.AllTests.buildSuite());
        suite.addTest(org.apache.jackrabbit.ocm.repository.AllTests.buildSuite());
        
        return suite;
    }
}

