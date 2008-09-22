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
package org.apache.jackrabbit.ocm.manager.collectionconverter;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.ocm.AnnotationTestBase;
import org.apache.jackrabbit.ocm.RepositoryLifecycleTestSetup;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.testmodel.collection.Main;
import org.apache.jackrabbit.ocm.testmodel.uuid.A;

import javax.jcr.RepositoryException;


/**
 *
 * This test validates that the BeanReferenceMapConverterImpl can be used to annotate java.util.Map
 * that contain beans stored as references.
 *
 *
 * Map converter used to map reference/uuid property by key into a java.util.Map.
 *
 *
 * @author <a href="mailto:vincent.giguere@gmail.com">Vincent Giguère</a>
 *
 */
public class BeanReferenceMapConverterImplTest extends AnnotationTestBase {
    private final static Log log = LogFactory.getLog(BeanReferenceMapConverterImplTest.class);

    /**
     * <p>Defines the test case name for junit.</p>
     *
     * @param testName The test case name.
     */
    public BeanReferenceMapConverterImplTest(String testName) throws Exception {
        super(testName);
    }

    public static Test suite() {
        // All methods starting with "test" will be executed in the test suite.
        return new RepositoryLifecycleTestSetup(
                new TestSuite(BeanReferenceMapConverterImplTest.class));
    }


    public void test_map_of_referenced_nodes_is_persisted_and_reloaded_properly() throws RepositoryException {
        ObjectContentManager ocm = getObjectContentManager();

        this.getSession().getRootNode().addNode("test");
        this.getSession().getRootNode().addNode("references");
        this.getSession().save();

        A firstA = new A();
        A secondA = new A();
        A thirdA = new A();

        firstA.setPath("/references/a1");
        secondA.setPath("/references/a2");
        thirdA.setPath("/references/a3");

        firstA.setStringData("the first");
        secondA.setStringData("the second");
        thirdA.setStringData("the third");

        ocm.insert(firstA);
        ocm.insert(secondA);
        ocm.insert(thirdA);

        assertNotNull(firstA.getPath());
        assertNotNull(secondA.getPath());
        assertNotNull(thirdA.getPath());

        firstA = (A) ocm.getObject(firstA.getPath());
        secondA = (A) ocm.getObject(secondA.getPath());
        thirdA = (A) ocm.getObject(thirdA.getPath());

        assertNotNull(firstA.getUuid());
        assertNotNull(secondA.getUuid());
        assertNotNull(thirdA.getUuid());


        Main main = new Main();
        main.setPath("/test/1");
        main.getReferenceMap().put("keyFirst", firstA);
        main.getReferenceMap().put("keySecond", secondA);
        main.getReferenceMap().put("keyThird", thirdA);


        ocm.insert(main);
        main = (Main) ocm.getObject(main.getPath());

        assertEquals("Referenced objects in store were not retrieved.", 3, main.getReferenceMap().size());
        assertNotNull("Reference could not be retrieved by its original key", main.getReferenceMap().get("keyFirst"));
        assertNotNull("Reference could not be retrieved by its original key", main.getReferenceMap().get("keySecond"));
        assertNotNull("Reference could not be retrieved by its original key", main.getReferenceMap().get("keyThird"));


        assertEquals("the first", main.getReferenceMap().get("keyFirst").getStringData());
        assertEquals("the second", main.getReferenceMap().get("keySecond").getStringData());
        assertEquals("the third", main.getReferenceMap().get("keyThird").getStringData());

    }

    public void test_map_can_persist_and_restore_same_node_reference_under_multiple_keys() throws RepositoryException {
        ObjectContentManager ocm = getObjectContentManager();

        this.getSession().getRootNode().addNode("test");
        this.getSession().getRootNode().addNode("references");
        this.getSession().save();

        A firstA = new A();

        firstA.setPath("/references/a1");

        firstA.setStringData("the data");

        ocm.insert(firstA);

        assertNotNull(firstA.getPath());

        firstA = (A) ocm.getObject(firstA.getPath());

        assertNotNull(firstA.getUuid());

        Main main = new Main();
        main.setPath("/test/1");
        main.getReferenceMap().put("keyFirst", firstA);
        main.getReferenceMap().put("keySecond", firstA);
        main.getReferenceMap().put("keyThird", firstA);


        ocm.insert(main);
        main = (Main) ocm.getObject(main.getPath());

        assertEquals("Referenced objects in store were not retrieved.", 3, main.getReferenceMap().size());
        assertNotNull("Reference could not be retrieved by its original key", main.getReferenceMap().get("keyFirst"));
        assertNotNull("Reference could not be retrieved by its original key", main.getReferenceMap().get("keySecond"));
        assertNotNull("Reference could not be retrieved by its original key", main.getReferenceMap().get("keyThird"));


        assertEquals("the data", main.getReferenceMap().get("keyFirst").getStringData());
        assertEquals("the data", main.getReferenceMap().get("keySecond").getStringData());
        assertEquals("the data", main.getReferenceMap().get("keyThird").getStringData());


    }


    public void test_map_keys_are_stored_in_relation_to_referenced_node() throws RepositoryException {

        /**
         * Make sure that the key to the map is not part of the referenced node.
         * In the child node realm, using the @Field(id=true) works, but not with referenced nodes, as they can be referenced by many nodes.
         */

        ObjectContentManager ocm = getObjectContentManager();

        this.getSession().getRootNode().addNode("test");
        this.getSession().getRootNode().addNode("references");
        this.getSession().save();

        A firstA = new A();
        A secondA = new A();
        A thirdA = new A();

        firstA.setPath("/references/a1");
        secondA.setPath("/references/a2");
        thirdA.setPath("/references/a3");

        firstA.setStringData("the first");
        secondA.setStringData("the second");
        thirdA.setStringData("the third");

        ocm.insert(firstA);
        ocm.insert(secondA);
        ocm.insert(thirdA);

        assertNotNull(firstA.getPath());
        assertNotNull(secondA.getPath());
        assertNotNull(thirdA.getPath());

        firstA = (A) ocm.getObject(firstA.getPath());
        secondA = (A) ocm.getObject(secondA.getPath());
        thirdA = (A) ocm.getObject(thirdA.getPath());

        assertNotNull(firstA.getUuid());
        assertNotNull(secondA.getUuid());
        assertNotNull(thirdA.getUuid());


        Main main = new Main();
        main.setPath("/test/1");
        main.getReferenceMap().put("keyFirst", firstA);
        main.getReferenceMap().put("keySecond", secondA);
        main.getReferenceMap().put("keyThird", thirdA);


        Main main2 = new Main();
        main2.setPath("/test/2");
        main2.getReferenceMap().put("AnotherkeyFirst", firstA);
        main2.getReferenceMap().put("AnotherkeySecond", secondA);
        main2.getReferenceMap().put("AnotherkeyThird", thirdA);


        ocm.insert(main);
        ocm.insert(main2);
        main = (Main) ocm.getObject(main.getPath());
        main2 = (Main) ocm.getObject(main2.getPath());

        assertEquals("Referenced objects in store were not retrieved.", 3, main.getReferenceMap().size());
        assertNotNull("Reference could not be retrieved by its original key", main.getReferenceMap().get("keyFirst"));
        assertNotNull("Reference could not be retrieved by its original key", main.getReferenceMap().get("keySecond"));
        assertNotNull("Reference could not be retrieved by its original key", main.getReferenceMap().get("keyThird"));


        assertEquals("Referenced objects in store were not retrieved.", 3, main2.getReferenceMap().size());
        assertNotNull("Reference could not be retrieved by its original key", main2.getReferenceMap().get("AnotherkeyFirst"));
        assertNotNull("Reference could not be retrieved by its original key", main2.getReferenceMap().get("AnotherkeySecond"));
        assertNotNull("Reference could not be retrieved by its original key", main2.getReferenceMap().get("AnotherkeyThird"));

        assertEquals("the first", main.getReferenceMap().get("keyFirst").getStringData());
        assertEquals("the second", main.getReferenceMap().get("keySecond").getStringData());
        assertEquals("the third", main.getReferenceMap().get("keyThird").getStringData());

        assertEquals("the first", main2.getReferenceMap().get("AnotherkeyFirst").getStringData());
        assertEquals("the second", main2.getReferenceMap().get("AnotherkeySecond").getStringData());
        assertEquals("the third", main2.getReferenceMap().get("AnotherkeyThird").getStringData());

    }

    public void test_converter_removes_deleted_nodes_when_updating() throws RepositoryException {

        ObjectContentManager ocm = getObjectContentManager();

        this.getSession().getRootNode().addNode("test");
        this.getSession().getRootNode().addNode("references");
        this.getSession().save();

        A firstA = new A();
        A secondA = new A();
        A thirdA = new A();

        firstA.setPath("/references/a1");
        secondA.setPath("/references/a2");
        thirdA.setPath("/references/a3");

        firstA.setStringData("the first");
        secondA.setStringData("the second");
        thirdA.setStringData("the third");

        ocm.insert(firstA);
        ocm.insert(secondA);
        ocm.insert(thirdA);

        assertNotNull(firstA.getPath());
        assertNotNull(secondA.getPath());
        assertNotNull(thirdA.getPath());

        firstA = (A) ocm.getObject(firstA.getPath());
        secondA = (A) ocm.getObject(secondA.getPath());
        thirdA = (A) ocm.getObject(thirdA.getPath());

        assertNotNull(firstA.getUuid());
        assertNotNull(secondA.getUuid());
        assertNotNull(thirdA.getUuid());


        Main main = new Main();
        main.setPath("/test/1");
        main.getReferenceMap().put("keyFirst", firstA);
        main.getReferenceMap().put("keySecond", secondA);
        main.getReferenceMap().put("keyThird", thirdA);


        ocm.insert(main);
        main = (Main) ocm.getObject(main.getPath());

        assertEquals("DefaultMapConverterImpl failed to store or reload objects in map property: referencedMap", 3, main.getReferenceMap().size());
        assertNotNull("DefaultMapConverterImpl failed to store objects in map", main.getReferenceMap().get("keyFirst"));
        assertNotNull("DefaultMapConverterImpl failed to store objects in map", main.getReferenceMap().get("keySecond"));
        assertNotNull("DefaultMapConverterImpl failed to store objects in map", main.getReferenceMap().get("keyThird"));


        assertEquals("DefaultMapConverterImpl failed to store objects in map", "the first", main.getReferenceMap().get("keyFirst").getStringData());
        assertEquals("DefaultMapConverterImpl failed to store objects in map", "the second", main.getReferenceMap().get("keySecond").getStringData());
        assertEquals("DefaultMapConverterImpl failed to store objects in map", "the third", main.getReferenceMap().get("keyThird").getStringData());


        main.getReferenceMap().remove("keyFirst");
        ocm.update(main);
        main = (Main) ocm.getObject(main.getPath());
        assertEquals("DefaultMapConverterImpl failed to store or reload objects in map property: referencedMap", 2, main.getReferenceMap().size());
        assertNull("DefaultMapConverterImpl failed to store objects in map", main.getReferenceMap().get("keyFirst"));
        assertNotNull("DefaultMapConverterImpl failed to store objects in map", main.getReferenceMap().get("keySecond"));
        assertNotNull("DefaultMapConverterImpl failed to store objects in map", main.getReferenceMap().get("keyThird"));

    }

    public void test_converter_adds_new_nodes_when_updating()  throws RepositoryException {

        ObjectContentManager ocm = getObjectContentManager();

        this.getSession().getRootNode().addNode("test");
        this.getSession().getRootNode().addNode("references");
        this.getSession().save();

        A firstA = new A();
        A secondA = new A();
        A thirdA = new A();

        firstA.setPath("/references/a1");
        secondA.setPath("/references/a2");
        thirdA.setPath("/references/a3");

        firstA.setStringData("the first");
        secondA.setStringData("the second");
        thirdA.setStringData("the third");

        ocm.insert(firstA);
        ocm.insert(secondA);
        ocm.insert(thirdA);

        assertNotNull(firstA.getPath());
        assertNotNull(secondA.getPath());
        assertNotNull(thirdA.getPath());

        firstA = (A) ocm.getObject(firstA.getPath());
        secondA = (A) ocm.getObject(secondA.getPath());
        thirdA = (A) ocm.getObject(thirdA.getPath());

        assertNotNull(firstA.getUuid());
        assertNotNull(secondA.getUuid());
        assertNotNull(thirdA.getUuid());


        Main main = new Main();
        main.setPath("/test/1");
        main.getReferenceMap().put("keyFirst", firstA);
        main.getReferenceMap().put("keySecond", secondA);
        


        ocm.insert(main);
        main = (Main) ocm.getObject(main.getPath());

        assertEquals("DefaultMapConverterImpl failed to store or reload objects in map property: referencedMap", 2, main.getReferenceMap().size());
        assertNotNull("DefaultMapConverterImpl failed to store objects in map", main.getReferenceMap().get("keyFirst"));
        assertNotNull("DefaultMapConverterImpl failed to store objects in map", main.getReferenceMap().get("keySecond"));
        assertNull("DefaultMapConverterImpl failed to store objects in map", main.getReferenceMap().get("keyThird"));


        main.getReferenceMap().put("keyThird", thirdA);
        ocm.update(main);
        main = (Main) ocm.getObject(main.getPath());
        assertEquals("DefaultMapConverterImpl failed to store or reload objects in map property: referencedMap", 3, main.getReferenceMap().size());
        assertNotNull("DefaultMapConverterImpl failed to store objects in map", main.getReferenceMap().get("keyFirst"));
        assertNotNull("DefaultMapConverterImpl failed to store objects in map", main.getReferenceMap().get("keySecond"));
        assertNotNull("DefaultMapConverterImpl failed to store objects in map", main.getReferenceMap().get("keyThird"));
    }

    public void test_converter_can_add_and_remove_nodes_simultaneously_when_updating() throws RepositoryException {

          ObjectContentManager ocm = getObjectContentManager();

        this.getSession().getRootNode().addNode("test");
        this.getSession().getRootNode().addNode("references");
        this.getSession().save();

        A firstA = new A();
        A secondA = new A();
        A thirdA = new A();
        A fourthA = new A();

        firstA.setPath("/references/a1");
        secondA.setPath("/references/a2");
        thirdA.setPath("/references/a3");
        fourthA.setPath("/references/a4");

        firstA.setStringData("the first");
        secondA.setStringData("the second");
        thirdA.setStringData("the third");

        ocm.insert(firstA);
        ocm.insert(secondA);
        ocm.insert(thirdA);
        ocm.insert(fourthA);

        assertNotNull(firstA.getPath());
        assertNotNull(secondA.getPath());
        assertNotNull(thirdA.getPath());
        assertNotNull(fourthA.getPath());

        firstA = (A) ocm.getObject(firstA.getPath());
        secondA = (A) ocm.getObject(secondA.getPath());
        thirdA = (A) ocm.getObject(thirdA.getPath());
        fourthA = (A) ocm.getObject(fourthA.getPath());

        assertNotNull(firstA.getUuid());
        assertNotNull(secondA.getUuid());
        assertNotNull(thirdA.getUuid());
        assertNotNull(fourthA.getUuid());


        Main main = new Main();
        main.setPath("/test/1");
        main.getReferenceMap().put("keyFirst", firstA);
        main.getReferenceMap().put("keySecond", secondA);
        main.getReferenceMap().put("keyThird", thirdA);



        ocm.insert(main);
        main = (Main) ocm.getObject(main.getPath());

        assertEquals("DefaultMapConverterImpl failed to store or reload objects in map property: referencedMap", 3, main.getReferenceMap().size());
        assertNotNull("DefaultMapConverterImpl failed to store objects in map", main.getReferenceMap().get("keyFirst"));
        assertNotNull("DefaultMapConverterImpl failed to store objects in map", main.getReferenceMap().get("keySecond"));
        assertNotNull("DefaultMapConverterImpl failed to store objects in map", main.getReferenceMap().get("keyThird"));
        assertNull("DefaultMapConverterImpl failed to store objects in map", main.getReferenceMap().get("keyFourth"));


        main.getReferenceMap().put("keyFourth", fourthA);
        main.getReferenceMap().remove("keyFirst");
        ocm.update(main);
        main = (Main) ocm.getObject(main.getPath());
        
        assertEquals("DefaultMapConverterImpl failed to store or reload objects in map property: referencedMap", 3, main.getReferenceMap().size());
        assertNull("DefaultMapConverterImpl failed to store objects in map", main.getReferenceMap().get("keyFirst"));
        assertNotNull("DefaultMapConverterImpl failed to store objects in map", main.getReferenceMap().get("keySecond"));
        assertNotNull("DefaultMapConverterImpl failed to store objects in map", main.getReferenceMap().get("keyThird"));
        assertNotNull("DefaultMapConverterImpl failed to store objects in map", main.getReferenceMap().get("keyFourth"));
    }
}
