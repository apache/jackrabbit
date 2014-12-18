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
package org.apache.jackrabbit.spi2davex;

import java.util.ArrayList;
import java.util.List;
import javax.jcr.RepositoryException;

import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.json.JsonUtil;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.Tree;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.tree.AbstractTree;

class JsonTree extends AbstractTree {

    private final StringBuilder properties = new StringBuilder();
    private final List<Part> parts = new ArrayList<Part>();

    JsonTree(Name nodeName, Name ntName, String uniqueId, NamePathResolver resolver) {
        super(nodeName, ntName, uniqueId, resolver);
    }

    //-------------------------------------------------------< AbstractTree >---
    @Override
    protected Tree createChild(Name name, Name primaryTypeName, String uniqueId) {
        return new JsonTree(name, primaryTypeName, uniqueId, getResolver());
    }

    //---------------------------------------------------------------< Tree >---
    @Override
    public void addProperty(Name propertyName, int propertyType, QValue value) throws RepositoryException {
        properties.append(Utils.getJsonKey(getResolver().getJCRName(propertyName)));
        properties.append(Utils.getJsonString(value));
    }

    @Override
    public void addProperty(Name propertyName, int propertyType, QValue[] values) throws RepositoryException {
        String name = getResolver().getJCRName(propertyName);
        properties.append(',');
        properties.append(Utils.getJsonKey(name));
        int index = 0;
        properties.append('[');
        for (QValue value : values) {
            String valueStr = Utils.getJsonString(value);
            if (valueStr == null) {
                Utils.addPart(name, value, getResolver(), parts);
            } else {
                String delim = (index++ == 0) ? "" : ",";
                properties.append(delim).append('"').append(valueStr).append('"');
            }
        }
        properties.append(']');
    }

    //--------------------------------------------------------------------------
    String toJsonString(List<Part> batchParts) throws RepositoryException {
        batchParts.addAll(this.parts);

        StringBuilder json = new StringBuilder();
        createJsonNodeFragment(json, this, true);
        return json.toString();
    }

    //--------------------------------------------------------------------------
    private String createJsonNodeFragment(StringBuilder json, JsonTree tree, boolean start) throws RepositoryException {
        if (!start) {
            json.append(',');
            json.append(Utils.getJsonKey(getResolver().getJCRName(tree.getName())));
        }
        json.append('{');
        json.append(Utils.getJsonKey(JcrConstants.JCR_PRIMARYTYPE));
        json.append(JsonUtil.getJsonString(getResolver().getJCRName(tree.getPrimaryTypeName())));
        String uuid = tree.getUniqueId();
        if (uuid != null) {
            json.append(',');
            json.append(Utils.getJsonKey(JcrConstants.JCR_UUID));
            json.append(JsonUtil.getJsonString(uuid));
        }
        // write all the properties.
        json.append(properties);
        for (Tree child : tree.getChildren()) {
            createJsonNodeFragment(json, (JsonTree) child, false);
        }
        json.append('}');
        return json.toString();
    }
}