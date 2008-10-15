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
package org.apache.jackrabbit.ocm.manager.enumeration;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.testmodel.enumeration.Odyssey;
import org.apache.jackrabbit.ocm.testmodel.enumeration.Planet;

/**
 * Test Simple Enumeration mappings
 *
 * @author <a href="mailto:boni.g@bioimagene.com">Boni Gopalan</a>
 */
public class SimpleEnumerationTestBase extends TestCase
{
    private final static Log logger = LogFactory.getLog(SimpleEnumerationTestBase.class);
    ObjectContentManager ocm;
    public SimpleEnumerationTestBase(ObjectContentManager ocm){
    	this.ocm = ocm;
    }

    public void testMapSimpleEnumeration()
    {
        try
        {
            // --------------------------------------------------------------------------------
            // Create and store an object graph in the repository
            // --------------------------------------------------------------------------------
            Odyssey odyssey = new Odyssey(); 
            odyssey.setPath("/odesseyToMars");
            odyssey.setGoingTo(Planet.MARS);
            odyssey.setStartingFrom(Planet.EARTH);
            odyssey.setStops(getStops());
            ocm.insert(odyssey);
            Odyssey fbOdessey = (Odyssey)ocm.getObject("/odesseyToMars");
            assertTrue("Fetched back Enum did not match the saved data", fbOdessey.getGoingTo() == Planet.MARS);
            assertTrue("Fetched back Enum did not match the saved data", fbOdessey.getStartingFrom() == Planet.EARTH);
            assertTrue("Fetched back Enum did not match the saved Enum Collection Size", fbOdessey.getStops().size() == odyssey.getStops().size());
            List<Planet> stops = getStops();
            List<Planet> fbStops = fbOdessey.getStops();
            for (Planet aStop : stops){
            	assertContains("Fetched back list did not contain :" + aStop.toString(), aStop, fbStops);
            	logger.info("Contains Enum : " + aStop.toString());
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Exception occurs during the unit test : " + e);
        }

    }

    private static void assertContains(String message, Planet value, List<Planet> aList){
    	for (Enum anObject : aList){
    		if (anObject == null){
    			if (value == null) return;
    			continue;
    		}
    		if (anObject.equals(value)) return;
    	}
    	fail(message);
    }
    
    private List<Planet> getStops(){
    	List<Planet> stops = new ArrayList<Planet>();
    	stops.add(Planet.MARS);
    	stops.add(Planet.MERCURY);
    	stops.add(Planet.JUPITER);
    	return stops;
    }

}
