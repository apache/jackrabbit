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
package org.apache.jackrabbit.spi.commons.batch;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import junit.framework.TestCase;

import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.Path.Element;
import org.apache.jackrabbit.spi.commons.identifier.IdFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.jackrabbit.spi.commons.value.QValueFactoryImpl;

public class ConsolidatedBatchTest extends TestCase {
    private final IdFactory idFactory = IdFactoryImpl.getInstance();
    private final PathFactory pathFactory = PathFactoryImpl.getInstance();
    private final NameFactory nameFactory = NameFactoryImpl.getInstance();
    private final QValueFactory valueFactory = QValueFactoryImpl.getInstance();

    private final ChangeLog[][] changeLogs;

    public ConsolidatedBatchTest() throws RepositoryException {
        changeLogs = new TestChangeLog[][] {
            { new TestChangeLog() // 1
                .addNode("/my/path/MyNode")
                .addNode("/my/path/MyNode2")
             ,new TestChangeLog()
                .addNode("/my/path/MyNode")
                .addNode("/my/path/MyNode2")
            }
            ,
            { new TestChangeLog() // 2
                .addNode("/my/path/MyNode")
                .delItem("/my/path/MyNode")
             ,new TestChangeLog()
            }
            ,
            { new TestChangeLog() // 3
                .addNode("/my")
                .addNode("/my/path")
                .addNode("/my/path/MyNode")
                .addNode("/my/path/MyNode2")
                .addNode("/my/path2")
                .addNode("/my/path2/MyNode")
                .delItem("/my")
             ,new TestChangeLog()
            }
            ,
            { new TestChangeLog() // 4
                .addNode("/my")
                .addNode("/my/path")
                .addNode("/my/path2")
                .delItem("/my/path2")
                .delItem("/my/path")
             ,new TestChangeLog()
                .addNode("/my")
            }
            ,
            { new TestChangeLog() // 5
                .addNode("/my")
                .addNode("/my/path")
                .addNode("/my/path2")
                .delItem("/my/path")
                .delItem("/my/path2")
             ,new TestChangeLog()
                .addNode("/my")
                .addNode("/my/path")
                .addNode("/my/path2")
                .delItem("/my/path")
                .delItem("/my/path2")
            }
            ,
            { new TestChangeLog() // 6
                .addNode("/my")
                .addNode("/my/path")
                .addProp("/my/path/Prop", "hello", PropertyType.STRING)
                .delItem("/my/path")
             ,new TestChangeLog()
                .addNode("/my")
            }
            ,
            { new TestChangeLog() // 7
                .addNode("/my")
                .addNode("/my/path")
                .addProp("/my/path/Prop", "hello", PropertyType.STRING)
                .delItem("/my/path/Prop")
             ,new TestChangeLog()
                .addNode("/my")
                .addNode("/my/path")
            }
            ,
            { new TestChangeLog() // 8
                .addNode("/my")
                .addNode("/my/path")
                .addNode("/my/path2")
                .addProp("/my/path/Prop", "hello", PropertyType.STRING)
                .movItem("/my/path", "my/path2")
                .delItem("/my")
             ,new TestChangeLog()
                .addNode("/my")
                .addNode("/my/path")
                .addNode("/my/path2")
                .addProp("/my/path/Prop", "hello", PropertyType.STRING)
                .movItem("/my/path", "my/path2")
                .delItem("/my")
            }
            ,
            { new TestChangeLog() // 9
                .addNode("/my")
                .addNode("/my/path")
                .addNode("/my/path2")
                .addProp("/my/path/Prop", "hello", PropertyType.STRING)
                .delItem("/my")
                .movItem("/my/path", "my/path2")
             ,new TestChangeLog()
                .movItem("/my/path", "my/path2")
            }
            ,
            { new TestChangeLog() // 10
                .ordNode("/my/path")
                .ordNode("/my/path")
                .ordNode("/my/path")
             ,new TestChangeLog()
                .ordNode("/my/path")
            }
            ,
            { new TestChangeLog() // 11
                .ordNode("/my/path")
                .delItem("/my/path")
             ,new TestChangeLog()
                .delItem("/my/path")
            }
            ,
            { new TestChangeLog() // 12
                .addNode("/my")
                .ordNode("/my/path")
                .delItem("/my")
             ,new TestChangeLog()
            }
            ,
            { new TestChangeLog() // 13
                .addNode("/my")
                .mixNode("/my", "MyMixin")
             ,new TestChangeLog()
                .addNode("/my")
                .mixNode("/my", "MyMixin")
            }
            ,
            { new TestChangeLog() // 14
                .addNode("/my")
                .mixNode("/my", "MyMixin")
                .mixNode("/my", "MyMixin")
                .mixNode("/my", "MyMixin")
             ,new TestChangeLog()
                .addNode("/my")
                .mixNode("/my", "MyMixin")
            }
            ,
            { new TestChangeLog() // 15
                .addNode("/my")
                .mixNode("/my", "MyMixin")
                .mixNode("/my", "MyMixin")
                .mixNode("/my", "MyMixin")
                .delItem("/my")
             ,new TestChangeLog()
            }
            ,
            { new TestChangeLog() // 16
                .addNode("/my")
                .addProp("/my/path/Prop", "hello", PropertyType.STRING)
                .setValu("/my/path/Prop", "hello2", PropertyType.STRING)
             ,new TestChangeLog()
                .addNode("/my")
                .addProp("/my/path/Prop", "hello", PropertyType.STRING)
                .setValu("/my/path/Prop", "hello2", PropertyType.STRING)
            }
            ,
            { new TestChangeLog() // 17
                .addNode("/my")
                .addProp("/my/path/Prop", "hello", PropertyType.STRING)
                .setValu("/my/path/Prop", "hello", PropertyType.STRING)
             ,new TestChangeLog()
                .addNode("/my")
                .addProp("/my/path/Prop", "hello", PropertyType.STRING)
            }
            ,
            { new TestChangeLog() // 18
                .addNode("/my")
                .addProp("/my/path/Prop", "hello", PropertyType.STRING)
                .setValu("/my/path/Prop", "hello2", PropertyType.STRING)
                .setValu("/my/path/Prop", "hello3", PropertyType.STRING)
             ,new TestChangeLog()
                .addNode("/my")
                .addProp("/my/path/Prop", "hello", PropertyType.STRING)
                .setValu("/my/path/Prop", "hello3", PropertyType.STRING)
            }
            ,
            { new TestChangeLog() // 19
                .addNode("/my")
                .addProp("/my/path/Prop", "hello", PropertyType.STRING)
                .setValu("/my/path/Prop", "hello2", PropertyType.STRING)
                .setValu("/my/path/Prop", "hello3", PropertyType.STRING)
                .delItem("/my/path")
             ,new TestChangeLog()
                .addNode("/my")
                .delItem("/my/path")
            }
            ,
            { new TestChangeLog() // 20
                .addNode("/my")
                .addProp("/my/path/Prop", "hello", PropertyType.STRING)
                .setValu("/my/path/Prop", "hello2", PropertyType.STRING)
                .setValu("/my/path/Prop", "hello3", PropertyType.STRING)
                .delItem("/my")
             ,new TestChangeLog()
            }
            ,
            { new TestChangeLog() // 21
                .addNode("/my")
                .ordNode("/my")
                .delItem("/my/path")
             ,new TestChangeLog()
                .addNode("/my")
                .ordNode("/my")
                .delItem("/my/path")
            }
            ,
            { new TestChangeLog() // 22
                .addNode("/my")
                .ordNode("/my")
                .ordNode("/my/path")
             ,new TestChangeLog()
                .addNode("/my")
                .ordNode("/my")
                .ordNode("/my/path")
            }
            ,
            { new TestChangeLog() // 23
                .addNode("/my")
                .ordNode("/my")
                .addNode("/my/path")
             ,new TestChangeLog()
                .addNode("/my")
                .ordNode("/my")
                .addNode("/my/path")
            }
            ,
            { new TestChangeLog() // 24
                .addNode("/my")
                .mixNode("/my", "mix")
                .delItem("/my/path")
             ,new TestChangeLog()
                .addNode("/my")
                .mixNode("/my", "mix")
                .delItem("/my/path")
            }
            ,
            { new TestChangeLog() // 25
                .addNode("/my")
                .mixNode("/my", "mix")
                .mixNode("/my", "mix2")
             ,new TestChangeLog()
                .addNode("/my")
                .mixNode("/my", "mix")
                .mixNode("/my", "mix2")
            }
            ,
            { new TestChangeLog() // 26
                .addNode("/my")
                .mixNode("/my", "mix")
                .addNode("/my2")
             ,new TestChangeLog()
                .addNode("/my")
                .mixNode("/my", "mix")
                .addNode("/my2")
            }
            ,
            { new TestChangeLog() // 27
                .setValu("/my/Prop", "value", PropertyType.STRING)
                .delItem("/my2")
             ,new TestChangeLog()
                .setValu("/my/Prop", "value", PropertyType.STRING)
                .delItem("/my2")
            }
            ,
            { new TestChangeLog() // 28
                .setValu("/my/Prop", "value", PropertyType.STRING)
                .addNode("/my2")
             ,new TestChangeLog()
                .setValu("/my/Prop", "value", PropertyType.STRING)
                .addNode("/my2")
            }
            ,
            { new TestChangeLog() // 29
                .addProp("/my/Prop", "value", PropertyType.STRING)
                .delItem("/my2")
             ,new TestChangeLog()
                .addProp("/my/Prop", "value", PropertyType.STRING)
                .delItem("/my2")
            }
            ,
            { new TestChangeLog() // 30
                .addProp("/my/Prop", "value", PropertyType.STRING)
                .setValu("/my/Prop", null, PropertyType.STRING)
             ,new TestChangeLog()
            }
            ,
        };
    }

    public void testChangeLogConsolidation() throws RepositoryException {
        for (int k = 0; k < changeLogs.length; k++) {
            ChangeLog changeLog = changeLogs[k][0];
            ChangeLog expected = changeLogs[k][1];
            assertEquals("Test no " + (k + 1), expected, changeLog.apply(new ConsolidatingChangeLog()));
        }
    }

    // -----------------------------------------------------< private >---

    private String nsPrefix(String name) {
        return name.startsWith("{")
            ? name
            : "{}" + name;
    }

    private Name createName(String name) {
        return nameFactory.create(nsPrefix(name));
    }

    private Path createPath(String path) {
        String[] names = path.split("/");
        Element[] elements = new Element[names.length];
        for (int k = 0; k < names.length; k++) {
            if ("".equals(names[k])) {
                elements[k] = pathFactory.getRootElement();
            }
            else {
                elements[k] = pathFactory.createElement(nameFactory.create(nsPrefix(names[k])));
            }
        }
        if (elements.length == 0) {
            return pathFactory.getRootPath();
        }
        return pathFactory.create(elements);
    }

    private NodeId createNodeId(Path path) {
        return idFactory.createNodeId((String) null, path);
    }

    public NodeId createNodeId(String nodeId) {
        return createNodeId(createPath(nodeId));
    }

    public PropertyId createPropertyId(String propertyId) throws RepositoryException {
        Path path = createPath(propertyId);
        return idFactory.createPropertyId(createNodeId(path.getAncestor(1)), path.getName());
    }

    private QValue createValue(String value, int type) throws RepositoryException {
        return value == null
            ? null
            : valueFactory.create(value, type);
    }

    // -----------------------------------------------------< ChangeLog >---

    private class TestChangeLog extends ChangeLogImpl {

        public TestChangeLog addNode(String nodeId) throws RepositoryException {
            Path path = createPath(nodeId);
            addNode(createNodeId(path.getAncestor(1)), path.getName(),
                    createName("anyType"), null);

            return this;
        }

        public TestChangeLog addProp(String propertyId, String value, int type) throws RepositoryException {
            Path path = createPath(propertyId);
            addProperty(createNodeId(path.getAncestor(1)), path.getName(), createValue(value, type));
            return this;
        }

        public TestChangeLog movItem(String srcNodeId, String destNodeId) throws RepositoryException {
            Path path = createPath(destNodeId);
            move(createNodeId(srcNodeId), createNodeId(path.getAncestor(1)), path.getName());
            return this;
        }

        public TestChangeLog delItem(String nodeId) throws RepositoryException {
            remove(createNodeId(nodeId));
            return this;
        }

        public TestChangeLog ordNode(String nodeId) throws RepositoryException {
            NodeId srcNodeId = createNodeId(nodeId);
            NodeId parentId = createNodeId(srcNodeId.getPath());
            reorderNodes(parentId, srcNodeId, createNodeId("/any/path"));
            return this;
        }

        public TestChangeLog mixNode(String nodeId, String mixinName) throws RepositoryException {
            setMixins(createNodeId(nodeId), new Name[] { createName(mixinName) });
            return this;
        }

        public TestChangeLog setValu(String propertyId, String value, int type) throws RepositoryException {
            setValue(createPropertyId(propertyId), createValue(value, type));
            return this;
        }

    }

}

