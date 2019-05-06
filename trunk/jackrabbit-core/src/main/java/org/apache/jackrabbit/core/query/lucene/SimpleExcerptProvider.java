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

import org.apache.lucene.search.Query;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.spi.Name;

import javax.jcr.PropertyType;
import java.io.IOException;
import java.util.Iterator;

/**
 * <code>SimpleExcerptProvider</code> is a <b>very</b> simple excerpt provider.
 * It does not do any highlighting and simply returns up to
 * <code>maxFragmentSize</code> characters of string properties for a given
 * node.
 * @see #getExcerpt(org.apache.jackrabbit.core.NodeId, int, int)
 */
public class SimpleExcerptProvider implements ExcerptProvider {

    /**
     * The item state manager.
     */
    private ItemStateManager ism;

    /**
     * {@inheritDoc}
     */
    public void init(Query query, SearchIndex index) throws IOException {
        ism = index.getContext().getItemStateManager();
    }

    /**
     * {@inheritDoc}
     */
    public String getExcerpt(NodeId id, int maxFragments, int maxFragmentSize)
            throws IOException {
        StringBuffer text = new StringBuffer();
        try {
            NodeState nodeState = (NodeState) ism.getItemState(id);
            String separator = "";
            Iterator<Name> it = nodeState.getPropertyNames().iterator();
            while (it.hasNext() && text.length() < maxFragmentSize) {
                PropertyId propId = new PropertyId(id, it.next());
                PropertyState propState = (PropertyState) ism.getItemState(propId);
                if (propState.getType() == PropertyType.STRING) {
                    text.append(separator);
                    separator = " ... ";
                    InternalValue[] values = propState.getValues();
                    for (InternalValue value : values) {
                        text.append(value.toString());
                    }
                }
            }
        } catch (ItemStateException e) {
            // ignore
        }
        if (text.length() > maxFragmentSize) {
            int lastSpace = text.lastIndexOf(" ", maxFragmentSize);
            if (lastSpace != -1) {
                text.setLength(lastSpace);
            } else {
                text.setLength(maxFragmentSize);
            }
            text.append(" ...");
        }
        return "<excerpt><fragment>" + text.toString() + "</fragment></excerpt>";
    }
}
