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
package org.apache.jackrabbit.spi.commons.identifier;

import junit.framework.TestCase;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.NameConstants;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

/**
 * <code>SerializationTest</code> checks that PropertyIds and NodeIds are
 * serializable.
 */
public class SerializationTest extends TestCase {

    private static final IdFactory ID_FACTORY = IdFactoryImpl.getInstance();

    private static final PathFactory PATH_FACTORY = PathFactoryImpl.getInstance();

    private static final Path REL_PATH = PATH_FACTORY.create(NameConstants.JCR_CONTENT);

    private static final NodeId PARENT_ID = ID_FACTORY.createNodeId("unique-id", REL_PATH);

    public void testPropertyId() throws Exception {
        checkSerializable(ID_FACTORY.createPropertyId(
                PARENT_ID, NameConstants.JCR_PRIMARYTYPE));
    }

    public void testNodeId() throws Exception {
        checkSerializable(ID_FACTORY.createNodeId(PARENT_ID, REL_PATH));

        checkSerializable(ID_FACTORY.createNodeId(PARENT_ID.getUniqueID(), REL_PATH));

        checkSerializable(ID_FACTORY.createNodeId(PARENT_ID.getUniqueID()));
    }

    private void checkSerializable(ItemId id) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(out);
        oos.writeObject(id);
        oos.close();
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(in);
        ItemId clone = (ItemId) ois.readObject();
        assertEquals("Deserialized ItemId not equal to original", id, clone);
    }
}
