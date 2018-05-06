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
package org.apache.jackrabbit.core.persistence.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import javax.jcr.PropertyType;

import junit.framework.TestCase;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.persistence.util.NodePropBundle.PropertyEntry;
import org.apache.jackrabbit.core.util.StringIndex;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;

/**
 * A randomized test for the BundleBinding writer and reader classes.
 */
public class BundleBindingRandomizedTest extends TestCase {

    private static final NameFactory factory = NameFactoryImpl.getInstance();

    /**
     * Tests serialization of a complex bundle.
     */
    public void testRandomBundle() throws Exception {
        int seed = 0;
        for (int i=0; i<100;) {
            Random r = new Random(seed++);
            NodePropBundle bundle;
            try {
                bundle = randomBundle(r);
            } catch (IllegalArgumentException e) {
                continue;
            }
            try {
                if (tryBundleRoundtrip(bundle)) {
                    i++;
                }
            } catch (Exception e) {
                throw new Exception(
                        "Error round-tripping bundle with seed " + seed, e);
                
            }
        }
    }
    
    private static NodePropBundle randomBundle(Random r) {
        NodeId id = randomNodeId(r);
        NodePropBundle bundle = new NodePropBundle(id);
        bundle.setModCount((short) randomSize(r)); 
        if (r.nextInt(10) > 0) {
            bundle.setParentId(randomNodeId(r));
        }
        if (r.nextInt(100) > 0) {
            bundle.setNodeTypeName(randomName(r));
        }
        if (r.nextInt(100) > 0) {
            bundle.setMixinTypeNames(randomNameSet(r));
        }
        if (r.nextInt(10) > 0) {
            bundle.setReferenceable(r.nextBoolean());
        }
        if (r.nextInt(10) > 0) {
            bundle.setSharedSet(randomNodeIdSet(r));
        }
        int count = randomSize(r);
        for (int i=0; i<count; i++) {
            PropertyEntry p = randomProperty(id, r);
            bundle.addProperty(p);
        }
        count = randomSize(r);
        for (int i=0; i<count; i++) {
            bundle.addChildNodeEntry(randomName(r), randomNodeId(r));
        }
        return bundle;
    }
    
    private static boolean tryBundleRoundtrip(NodePropBundle bundle)
            throws Exception {
        BundleBinding binding;
        StringIndex index = new StringIndex() {
            @Override
            public int stringToIndex(String string) {
                return 0;
            }
            @Override
            public String indexToString(int idx) {
                return "";
            }
        };
        binding = new BundleBinding(null, null, index, index, null);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            binding.writeBundle(buffer, bundle);
        } catch (Exception e) {
            // ignore - error found
            return false;
        }
        byte[] bytes = buffer.toByteArray();
        NodePropBundle b2 = binding.readBundle(
                new ByteArrayInputStream(bytes), bundle.getId());
        if (!bundle.equals(b2)) {
            // if debugging is needed:
            // buffer = new ByteArrayOutputStream();
            // binding.writeBundle(buffer, bundle);
            // bytes = buffer.toByteArray();
            // b2 = binding.readBundle(
            // new ByteArrayInputStream(bytes), bundle.getId());
            assertEquals(bundle.toString(), b2.toString());
        }
        return true;
    }
    
    private static PropertyEntry randomProperty(NodeId nodeId, Random r) {
        PropertyEntry p = new PropertyEntry(
                new PropertyId(nodeId, randomName(r)));        
        int type = PropertyType.STRING;
        if (r.nextInt(10) == 0) {
            type = r.nextInt() + 15;
        }
        p.setType(type);
        p.setModCount((short) randomSize(r)); 
        boolean multiValued = r.nextBoolean();
        p.setMultiValued(multiValued);
        int size;
        if (multiValued && r.nextInt(10) > 0) {
            size = 1;
        } else {
            size = randomSize(r);
        }
        InternalValue[] values = new InternalValue[size];
        for (int i = 0; i < size; i++) {
            values[i] = randomValue(r);
        }
        p.setValues(values);
        return p;
    }
    
    private static InternalValue randomValue(Random r) {
        if (r.nextInt(50) == 0) {
            return null;
        }
        // TODO currently only string values
        return InternalValue.create(randomString(r));
    }

    private static int randomSize(Random r) {
        if (r.nextInt(20) == 0) {
            return 0;
        } else if (r.nextInt(20) == 0) {
            return 1;
        } else if (r.nextInt(20) == 0) {
            return r.nextInt(10000);
        }
        return r.nextInt(5) + 1;
    }
    
    private static Set<Name> randomNameSet(Random r) {
        if (r.nextInt(100) == 0) {
            return null;
        }
        int size = randomSize(r);
        HashSet<Name> set = new HashSet<Name>();
        for(int i=0; i<size; i++) {
            set.add(randomName(r));
        }
        return set;
    }
    
    private static Set<NodeId> randomNodeIdSet(Random r) {
        if (r.nextInt(100) == 0) {
            return null;
        } else if (r.nextInt(10) == 0) {
            return Collections.emptySet();
        }
        int size = randomSize(r);
        HashSet<NodeId> set = new HashSet<NodeId>();
        for(int i=0; i<size; i++) {
            set.add(randomNodeId(r));
        }
        return set;
    }
    
    private static NodeId randomNodeId(Random r) {
        if (r.nextInt(100) == 0) {
            return null;
        }
        return new NodeId(r.nextLong(), r.nextLong());
    }
    
    private static Name randomName(Random r) {
        if (r.nextInt(100) == 0) {
            return null;
        }
        return factory.create(randomString(r), randomString(r));
    }
    
    private static String randomString(Random r) {
        int size = randomSize(r);
        StringBuilder buff = new StringBuilder();
        for (int i=0; i<size; i++) {
            buff.append("abcd".charAt(r.nextInt(4)));
        }
        return buff.toString();
    }
    
}
