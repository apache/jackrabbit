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

import org.apache.jackrabbit.ocm.manager.collectionconverter.impl.MapReferenceValueEncoder;
import static junit.framework.Assert.assertEquals;import static junit.framework.Assert.assertTrue;
import junit.framework.TestCase;

/**
 * TODO: JAVADOC
 * <p/>
 * Created by Vincent Giguere
 * Date: Jun 9, 2008
 * Time: 3:08:28 PM
 */
public class MapReferenceValueEncoderTest extends TestCase{


    public void testEncode_key_and_reference()
    {
        String key = "value1";
        String reference = "aReference";
        assertTrue(MapReferenceValueEncoder.encodeKeyAndReference(key, reference).contains(key));
        assertTrue(MapReferenceValueEncoder.encodeKeyAndReference(key, reference).contains(reference));

    }

    
    public void testDecode_key()
    {
        String key = "value1";
        String reference = "aReference";
        assertEquals(key , MapReferenceValueEncoder.decodeKey(MapReferenceValueEncoder.encodeKeyAndReference(key, reference)));
    }


    public void testDecode_reference()
    {
        String key = "value1";
        String reference = "aReference";
        assertEquals(reference , MapReferenceValueEncoder.decodeReference(MapReferenceValueEncoder.encodeKeyAndReference(key, reference)));
    }


}
