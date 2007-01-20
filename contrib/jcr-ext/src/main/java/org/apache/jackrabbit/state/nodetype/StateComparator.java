/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.state.nodetype;

import java.util.Comparator;

import org.apache.jackrabbit.name.QName;

/**
 * TODO
 */
class StateComparator implements Comparator {

    public int compare(Object a, Object b) {
        if (a instanceof NodeTypeState) {
            return compareNodeTypeStates((NodeTypeState) a, (NodeTypeState) b);
        } else if (a instanceof NodeDefinitionState) {
            return compareNodeDefinitionStates((NodeDefinitionState) a, (NodeDefinitionState) b);
        } else if (a instanceof PropertyDefinitionState) {
            return comparePropertyDefinitionStates((PropertyDefinitionState) a, (PropertyDefinitionState) b);
        } else if (a instanceof QName) {
            return compareQNames((QName) a, (QName) b);
        } else {
            throw new ClassCastException("Unknown class: " + a.getClass());
        }
    }

    private int compareNodeTypeStates(NodeTypeState a, NodeTypeState b) {
        if (compareQNames(a.getName(), b.getName()) != 0) {
            return compareQNames(a.getName(), b.getName());
        } else if (a.isMixin() != b.isMixin()) {
            return a.isMixin() ? -1 : 1;
        } else if (a.hasOrderableChildNodes() != b.hasOrderableChildNodes()) {
            return a.hasOrderableChildNodes() ? -1 : 1;
        } else if (compareQNames(a.getPrimaryItemName(), b.getPrimaryItemName()) != 0) {
            return compareQNames(a.getPrimaryItemName(), b.getPrimaryItemName());
        } else if (compareQNameArrays(a.getSupertypeNames(), b.getSupertypeNames()) != 0) {
            return compareQNameArrays(a.getSupertypeNames(), b.getSupertypeNames());
        } else if (comparePropertyDefinitionStateArrays(a.getPropertyDefinitionStates(), b.getPropertyDefinitionStates()) != 0) {
            return comparePropertyDefinitionStateArrays(a.getPropertyDefinitionStates(), b.getPropertyDefinitionStates());
        } else {
            return compareNodeDefinitionStateArrays(a.getChildNodeDefinitionStates(), b.getChildNodeDefinitionStates());
        }
    }

    private int compareNodeDefinitionStateArrays(
            NodeDefinitionState[] a, NodeDefinitionState[] b) {
        for (int i = 0; i < a.length && i < b.length; i++) {
            if (compareNodeDefinitionStates(a[i], b[i]) != 0) {
                return compareNodeDefinitionStates(a[i], b[i]);
            }
        }
        return a.length - b.length;
    }

    private int compareNodeDefinitionStates(
            NodeDefinitionState a, NodeDefinitionState b) {
        if (compareItemDefinitionStates(a, b) != 0) {
            return compareItemDefinitionStates(a, b);
        } else if (a.allowsSameNameSiblings() != b.allowsSameNameSiblings()) {
            return a.allowsSameNameSiblings() ? -1 : 1;
        } else if (compareQNames(a.getDefaultPrimaryTypeName(), b.getDefaultPrimaryTypeName()) != 0) {
            return compareQNames(a.getDefaultPrimaryTypeName(), b.getDefaultPrimaryTypeName());
        } else {
            return compareQNameArrays(a.getRequiredPrimaryTypeNames(), b.getRequiredPrimaryTypeNames());
        }
    }

    private int comparePropertyDefinitionStateArrays(
            PropertyDefinitionState[] a, PropertyDefinitionState[] b) {
        for (int i = 0; i < a.length && i < b.length; i++) {
            if (comparePropertyDefinitionStates(a[i], b[i]) != 0) {
                return comparePropertyDefinitionStates(a[i], b[i]);
            }
        }
        return a.length - b.length;
    }
    private int comparePropertyDefinitionStates(
            PropertyDefinitionState a, PropertyDefinitionState b) {
        if (compareItemDefinitionStates(a, b) != 0) {
            return compareItemDefinitionStates(a, b);
        } else if (a.isMultiple() != b.isMultiple()) {
            return a.isMultiple() ? -1 : 1;
        } else {
            return a.getRequiredType() - b.getRequiredType();
        }
    }

    private int compareItemDefinitionStates(
            ItemDefinitionState a, ItemDefinitionState b) {
        if (compareQNames(a.getName(), b.getName()) != 0) {
            return compareQNames(a.getName(), b.getName());
        } else if (a.isAutoCreated() != b.isAutoCreated()) {
            return a.isAutoCreated() ? -1 : 1;
        } else if (a.isMandatory() != b.isMandatory()) {
            return a.isMandatory() ? -1 : 1;
        } else if (a.isProtected() != b.isProtected()) {
            return b.isProtected() ? -1 : 1;
        } else {
            return a.getOnParentVersion() - b.getOnParentVersion();
        }
    }

    private int compareQNameArrays(QName[] a, QName[] b) {
        for (int i = 0; i < a.length && i < b.length; i++) {
            if (compareQNames(a[i], b[i]) != 0) {
                return compareQNames(a[i], b[i]);
            }
        }
        return a.length - b.length;
    }

    private int compareQNames(QName a, QName b) {
        if (a != null && b != null) {
            return a.compareTo(b);
        } else if (a != null) {
            return -1;
        } else if (b != null) {
            return 1;
        } else {
            return 0;
        }
    }

}
