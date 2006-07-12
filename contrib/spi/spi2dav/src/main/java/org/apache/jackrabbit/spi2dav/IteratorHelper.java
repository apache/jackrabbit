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
package org.apache.jackrabbit.spi2dav;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.spi.IdIterator;
import org.apache.jackrabbit.spi.QNodeTypeDefinitionIterator;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;

import java.util.Collection;
import java.util.Iterator;

/**
 * <code>org.apache.jackrabbit.spi2dav.IteratorHelper</code>...
 */
public class IteratorHelper extends org.apache.jackrabbit.util.IteratorHelper
    implements IdIterator, QNodeTypeDefinitionIterator {

    private static Logger log = LoggerFactory.getLogger(IteratorHelper.class);

    public IteratorHelper(Collection c) {
        super(c);
    }

    public IteratorHelper(Iterator iter) {
        super(iter);
    }

    /**
     * {@inheritDoc}
     */
    public ItemId nextId() {
        return (ItemId) next();
    }

    /**
     * {@inheritDoc}
     */
    public QNodeTypeDefinition nextDefinition() {
        return (QNodeTypeDefinition) next();
    }
}