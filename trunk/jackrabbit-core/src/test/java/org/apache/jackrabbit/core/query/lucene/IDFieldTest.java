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
package org.apache.jackrabbit.core.query.lucene;

import junit.framework.TestCase;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.TermVector;

/**
 * <code>IDFieldTest</code>...
 */
public class IDFieldTest extends TestCase {

    public void testPerformance() {
        NodeId id = NodeId.randomId();
        long time = System.currentTimeMillis();
        for (int i = 0; i < 1000 * 1000; i++) {
            new IDField(id);
        }
        time = System.currentTimeMillis() - time;
        System.out.println("IDField: " + time + " ms.");

        for (int i = 0; i < 50; i++) {
            createNodes(id.toString(), i % 2 == 0);
        }
    }

    private void createNodes(String id, boolean useNewWay) {

        long time = System.currentTimeMillis();
        for (int i = 0; i < 1000 * 1000; i++) {
            if (useNewWay) {
                new Field(FieldNames.UUID, false, id, Field.Store.YES,
                        Field.Index.NOT_ANALYZED_NO_NORMS, TermVector.NO);
            } else {
                new Field(FieldNames.UUID, id, Field.Store.YES,
                        Field.Index.NOT_ANALYZED_NO_NORMS, TermVector.NO);
            }

        }
        time = System.currentTimeMillis() - time;
        System.out.println(String.format("Field: %2s ms. new way? %b", time,
                useNewWay));

    }
}
