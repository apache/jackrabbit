/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.extension.configuration;

import java.util.Calendar;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationComparator;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.StrictConfigurationComparator;
import org.apache.jackrabbit.extension.ExtensionFrameworkTestBase;


public class ItemConfigurationTest extends ExtensionFrameworkTestBase {

    private static final String ROOT_PATH = "/config/test";
    private static final String CFG1 = ROOT_PATH + "/config1";
    private static final String CFG2 = ROOT_PATH + "/config2";
//    private static final String CFG3 = ROOT_PATH + "/config3";

    protected void setUp() throws Exception {
        super.setUp();

        ensurePath(session, ROOT_PATH);
    }

    protected void tearDown() throws Exception {
        try {
            Item item = session.getItem(ROOT_PATH);
            javax.jcr.Node parent = item.getParent();
            item.remove();
            parent.save();
        } catch (RepositoryException re) {
            // don't care
        }

        super.tearDown();
    }

    public void testCreateAndLoad() throws RepositoryException, ConfigurationException {
        // this is the manually created
        Configuration created = createSampleConfig();
//        dumpConfiguration(created);

        // get the loaded
        javax.jcr.Node cfgNode = createSampleConfig(CFG1);
        Configuration loaded = new ItemConfiguration(cfgNode);
//        dumpConfiguration(loaded);

        // compare configurations
        ConfigurationComparator comparator = new StrictConfigurationComparator();
        assertTrue("Configurations differ", comparator.compare(created, loaded));
    }

    public void testLoadSave() throws RepositoryException, ConfigurationException {
        // load configuration
        javax.jcr.Node cfgNode = createSampleConfig(CFG1);
        ItemConfiguration loaded = new ItemConfiguration(cfgNode);
//        dumpConfiguration(loaded);

        // store somewhere else
        Node dest = ensurePath(session, CFG2);
        loaded.save(dest);

        ItemConfiguration second = new ItemConfiguration(dest);
//        dumpConfiguration(second);

        // compare configurations
        ConfigurationComparator comparator = new StrictConfigurationComparator();
        assertTrue("Configurations differ", comparator.compare(loaded, second));
    }

    public void testLoadAdditions() throws RepositoryException, ConfigurationException {
        // load configuration
        javax.jcr.Node cfgNode = createSampleConfig(CFG1);
        ItemConfiguration loaded = new ItemConfiguration(cfgNode);

        // add to the configuration
        loaded.addProperty("added.value[@data]", "Added data");
        loaded.addProperty("typed.added", "Added to typed");
        loaded.addProperty("typed[@string]", "Another string");

//        dumpConfiguration("Loaded", loaded);

        // save to the configuration
        loaded.save();

        // load the same configuration into a new object
        ItemConfiguration second = new ItemConfiguration(cfgNode);
//        dumpConfiguration("Second", second);

        // compare configurations
        ConfigurationComparator comparator = new StrictConfigurationComparator();
        assertTrue("Configurations differ", comparator.compare(loaded, second));
    }

    public void testLoadDeletions() throws RepositoryException, ConfigurationException {
        // load configuration
        javax.jcr.Node cfgNode = createSampleConfig(CFG1);
        ItemConfiguration loaded = new ItemConfiguration(cfgNode);

//        dumpConfiguration("Loaded - unmodified", loaded);

        // add to the configuration
        loaded.clearProperty("typed[@string]");
        loaded.clearProperty("multivalue[@multi](0)");
        loaded.clearProperty("sns.same(1)");

//        dumpConfiguration("Loaded - modified", loaded);

        // save to the configuration
        loaded.save();

        // load the same configuration into a new object
        ItemConfiguration second = new ItemConfiguration(cfgNode);
//        dumpConfiguration("Second", second);

        // compare configurations
        ConfigurationComparator comparator = new StrictConfigurationComparator();
        assertTrue("Configurations differ", comparator.compare(loaded, second));
    }

    public void testLoadDeletionsNotSaved() throws RepositoryException, ConfigurationException {
        // load configuration
        javax.jcr.Node cfgNode = createSampleConfig(CFG1);
        ItemConfiguration loaded = new ItemConfiguration(cfgNode);

//        dumpConfiguration("Loaded - unmodified", loaded);

        ItemConfiguration unmodified = new ItemConfiguration(cfgNode);
//        dumpConfiguration("Unmodified", unmodified);

        // add to the configuration
        loaded.clearProperty("typed[@string]");
        loaded.clearProperty("multivalue[@multi](0)");
        loaded.clearProperty("sns.same(1)");

//        dumpConfiguration("Loaded - modified", loaded);

        // load the same configuration into a new object
        ItemConfiguration second = new ItemConfiguration(cfgNode);
//        dumpConfiguration("Second", second);

        // compare configurations
        ConfigurationComparator comparator = new StrictConfigurationComparator();

        // loaded (modified) and second must be different
        assertFalse("Configurations are the same", comparator.compare(loaded, second));

        // unmodified and second must bethe same
        assertTrue("Configurations differ", comparator.compare(unmodified, second));
    }

    protected HierarchicalConfiguration createSampleConfig() {
        HierarchicalConfiguration config = new HierarchicalConfiguration();

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(0);

        config.addProperty("typed[@string]", "value1");
        config.addProperty("typed[@boolean]", Boolean.TRUE);
        config.addProperty("typed[@double]", new Double(2.5D));
        config.addProperty("typed[@long]", new Long(10L));
        config.addProperty("typed[@date]", cal);

        for (int i=0; i < 5; i++) {
            config.addProperty("multivalue[@multi]", "multi[" + i+ "]");
        }

        for (int i=0; i < 3; i++) {
            config.addProperty("sns.same", "sample" + i);
        }

        config.addProperty("nodeValue", "This is the node's value");

        return config;
    }

    protected javax.jcr.Node createSampleConfig(String cfgRoot) throws RepositoryException {
        javax.jcr.Node cfg = ensurePath(session, cfgRoot + "/rep:configuration");

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(0);

        // child node with typed properties
        javax.jcr.Node child = cfg.addNode("typed");
        child.setProperty("string", "value1");
        child.setProperty("boolean", true);
        child.setProperty("double", 2.5D);
        child.setProperty("long", 10L);
        child.setProperty("date", cal);

        // child node with multivalue property
        ValueFactory vf = session.getValueFactory();
        Value[] multi = new Value[5];
        for (int i=0; i < multi.length; i++) {
            multi[i] = vf.createValue("multi[" + i + "]");
        }
        child = cfg.addNode("multivalue");
        child.setProperty("multi", multi);

        // same name sibbling children
        child = cfg.addNode("sns");
        child.addNode("same").setProperty("__DEFAULT__", "sample0");
        child.addNode("same").setProperty("__DEFAULT__", "sample1");
        child.addNode("same").setProperty("__DEFAULT__", "sample2");

        // __DEFAULT__ value nodes
        child = cfg.addNode("nodeValue");
        child.setProperty("__DEFAULT__", "This is the node's value");

        // save everything !!
        cfg.save();

        return cfg; // .getParent();
    }

//    private void dumpConfiguration(String title, Configuration config) {
//        System.out.println();
//        System.out.println(title);
//        for (int i=0; i < title.length(); i++) {
//            System.out.print('-');
//        }
//        System.out.println();
//
//        Set keys = new TreeSet();
//        for (Iterator ki=config.getKeys(); ki.hasNext(); ) {
//            keys.add(ki.next());
//        }
//
//        for (Iterator ki=keys.iterator(); ki.hasNext(); ) {
//            String key = (String) ki.next();
//            System.out.println(key + " ==> " + config.getProperty(key));
//        }
//    }
}
