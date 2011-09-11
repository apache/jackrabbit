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
package org.apache.jackrabbit.spi.commons.nodetype;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QItemDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QValueConstraint;
import org.apache.jackrabbit.spi.commons.name.NameConstants;

import javax.jcr.PropertyType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A <code>NodeTypeDefDiff</code> represents the result of the comparison of
 * two node type definitions.
 * <p/>
 * The result of the comparison can be categorized as one of the following types:
 * <p/>
 * <b><code>NONE</code></b> indicates that there is no modification at all.
 * <p/>
 * A <b><code>TRIVIAL</code></b> modification has no impact on the consistency
 * of existing content. The following modifications are considered
 * <code>TRIVIAL</code>:
 * <ul>
 * <li>changing node type <code>orderableChildNodes</code> flag
 * <li>changing node type <code>primaryItemName</code> value
 * <li>adding non-<code>mandatory</code> property/child node
 * <li>changing property/child node <code>protected</code> flag
 * <li>changing property/child node <code>onParentVersion</code> value
 * <li>changing property/child node <code>mandatory</code> flag to <code>false</code>
 * <li>changing property/child node <code>autoCreated</code> flag
 * <li>changing specific property/child node <code>name</code> to <code>*</code>
 * <li>changing child node <code>defaultPrimaryType</code>
 * <li>changing child node <code>sameNameSiblings</code> flag to <code>true</code>
 * <li>weaken child node <code>requiredPrimaryTypes</code> (e.g. by removing)
 * <li>weaken property <code>valueConstraints</code> (e.g. by removing a constraint
 * or by making a specific constraint less restrictive)
 * <li>changing property <code>defaultValues</code>
 * <li>changing specific property <code>requiredType</code> to <code>undefined</code>
 * <li>changing property <code>multiple</code> flag to <code>true</code>
 * </ul>
 * <p/>
 * A <b><code>MAJOR</code></b> modification potentially <i>affects</i> the
 * consistency of existing content.
 *
 * All modifications that are not <b><code>TRIVIAL</code></b> are considered
 * <b><code>MAJOR</code></b>.
 *
 * @see #getType()
 */
public class NodeTypeDefDiff {

    /**
     * no modification
     */
    public static final int NONE = 0;
    /**
     * trivial modification: does not affect consistency of existing content
     */
    public static final int TRIVIAL = 1;
    /**
     * major modification: <i>does</i> affect consistency of existing content
     */
    public static final int MAJOR = 2;

    private final QNodeTypeDefinition oldDef;
    private final QNodeTypeDefinition newDef;
    private int type;

    private final List<PropDefDiff> propDefDiffs = new ArrayList<PropDefDiff>();
    private final List<ChildNodeDefDiff> childNodeDefDiffs = new ArrayList<ChildNodeDefDiff>();

    /**
     * Constructor
     * @param oldDef old definition
     * @param newDef new definition
     */
    private NodeTypeDefDiff(QNodeTypeDefinition oldDef, QNodeTypeDefinition newDef) {
        this.oldDef = oldDef;
        this.newDef = newDef;
        init();
    }

    /**
     *
     */
    private void init() {
        if (oldDef.equals(newDef)) {
            // definitions are identical
            type = NONE;
        } else {
            // definitions are not identical, determine type of modification

            // assume TRIVIAL change by default
            type = TRIVIAL;

            // check supertypes
            int tmpType = supertypesDiff();
            if (tmpType > type) {
                type = tmpType;
            }

            // check mixin flag (MAJOR modification)
            tmpType = mixinFlagDiff();
            if (tmpType > type) {
                type = tmpType;
            }

            // check abstract flag (MAJOR modification)
            tmpType = abstractFlagDiff();
            if (tmpType > type) {
                type = tmpType;
            }

            // no need to check orderableChildNodes flag (TRIVIAL modification)
            // no need to check queryable flag (TRIVIAL modification)

            // check property definitions
            tmpType = buildPropDefDiffs();
            if (tmpType > type) {
                type = tmpType;
            }

            // check child node definitions
            tmpType = buildChildNodeDefDiffs();
            if (tmpType > type) {
                type = tmpType;
            }
        }
    }

    /**
     * @param oldDef old definition
     * @param newDef new definition
     * @return the diff
     */
    public static NodeTypeDefDiff create(QNodeTypeDefinition oldDef, QNodeTypeDefinition newDef) {
        if (oldDef == null || newDef == null) {
            throw new IllegalArgumentException("arguments can not be null");
        }
        if (!oldDef.getName().equals(newDef.getName())) {
            throw new IllegalArgumentException("at least node type names must be matching");
        }
        return new NodeTypeDefDiff(oldDef, newDef);
    }

    /**
     * @return <code>true</code> if modified
     */
    public boolean isModified() {
        return type != NONE;
    }

    /**
     * @return <code>true</code> if trivial
     */
    public boolean isTrivial() {
        return type == TRIVIAL;
    }

    /**
     * @return <code>true</code> if major
     */
    public boolean isMajor() {
        return type == MAJOR;
    }

    /**
     * Returns the type of modification as expressed by the following constants:
     * <ul>
     * <li><b><code>NONE</code></b>: no modification at all
     * <li><b><code>TRIVIAL</code></b>: does not affect consistency of
     * existing content
     * <li><b><code>MAJOR</code></b>: <i>does</i> affect consistency of existing
     * content
     * </ul>
     *
     * @return the type of modification
     */
    public int getType() {
        return type;
    }

    /**
     * @return <code>true</code> if mixin flag diff
     */
    public int mixinFlagDiff() {
        return oldDef.isMixin() != newDef.isMixin() ? MAJOR : NONE;
    }

    /**
     * @return <code>true</code> if abstract flag diff
     */
    public int abstractFlagDiff() {
        return oldDef.isAbstract() && !newDef.isAbstract() ? MAJOR : NONE;
    }

    /**
     * @return <code>true</code> if supertypes diff
     */
    public int supertypesDiff() {
        Set<Name> set1 = new HashSet<Name>(Arrays.asList(oldDef.getSupertypes()));
        Set<Name> set2 = new HashSet<Name>(Arrays.asList(newDef.getSupertypes()));
        return !set1.equals(set2) ? MAJOR : NONE;
    }

    /**
     * @return diff type
     */
    private int buildPropDefDiffs() {
        int maxType = NONE;
        Map<QPropertyDefinitionId, QPropertyDefinition> oldDefs = new HashMap<QPropertyDefinitionId, QPropertyDefinition>();
        for (QPropertyDefinition def : oldDef.getPropertyDefs()) {
            oldDefs.put(new QPropertyDefinitionId(def), def);
        }

        Map<QPropertyDefinitionId, QPropertyDefinition> newDefs = new HashMap<QPropertyDefinitionId, QPropertyDefinition>();
        for (QPropertyDefinition def : newDef.getPropertyDefs()) {
            newDefs.put(new QPropertyDefinitionId(def), def);
        }

        /**
         * walk through defs1 and process all entries found in
         * both defs1 & defs2 and those found only in defs1
         */
        for (Map.Entry<QPropertyDefinitionId, QPropertyDefinition> entry : oldDefs.entrySet()) {
            QPropertyDefinitionId id = entry.getKey();
            QPropertyDefinition def1 = entry.getValue();
            QPropertyDefinition def2 = newDefs.get(id);
            PropDefDiff diff = new PropDefDiff(def1, def2);
            if (diff.getType() > maxType) {
                maxType = diff.getType();
            }
            propDefDiffs.add(diff);
            newDefs.remove(id);
        }

        /**
         * defs2 by now only contains entries found in defs2 only;
         * walk through defs2 and process all remaining entries
         */
        for (Map.Entry<QPropertyDefinitionId, QPropertyDefinition> entry : newDefs.entrySet()) {
            QPropertyDefinition def = entry.getValue();
            PropDefDiff diff = new PropDefDiff(null, def);
            if (diff.getType() > maxType) {
                maxType = diff.getType();
            }
            propDefDiffs.add(diff);
        }

        return maxType;
    }

    /**
     * @return diff type
     */
    private int buildChildNodeDefDiffs() {
        int maxType = NONE;
        QNodeDefinition[] cnda1 = oldDef.getChildNodeDefs();
        Map<QNodeDefinitionId, QNodeDefinition> defs1 = new HashMap<QNodeDefinitionId, QNodeDefinition>();
        for (QNodeDefinition def1 : cnda1) {
            defs1.put(new QNodeDefinitionId(def1), def1);
        }

        QNodeDefinition[] cnda2 = newDef.getChildNodeDefs();
        Map<QNodeDefinitionId, QNodeDefinition> defs2 = new HashMap<QNodeDefinitionId, QNodeDefinition>();
        for (QNodeDefinition def2 : cnda2) {
            defs2.put(new QNodeDefinitionId(def2), def2);
        }

        /**
         * walk through defs1 and process all entries found in
         * both defs1 & defs2 and those found only in defs1
         */
        for (Map.Entry<QNodeDefinitionId, QNodeDefinition> entry1 : defs1.entrySet()) {
            QNodeDefinitionId id = entry1.getKey();
            QNodeDefinition def1 = entry1.getValue();
            QNodeDefinition def2 = defs2.get(id);
            ChildNodeDefDiff diff = new ChildNodeDefDiff(def1, def2);
            if (diff.getType() > maxType) {
                maxType = diff.getType();
            }
            childNodeDefDiffs.add(diff);
            defs2.remove(id);
        }

        /**
         * defs2 by now only contains entries found in defs2 only;
         * walk through defs2 and process all remaining entries
         */
        for (Map.Entry<QNodeDefinitionId, QNodeDefinition> entry2 : defs2.entrySet()) {
            QNodeDefinition def2 = entry2.getValue();
            ChildNodeDefDiff diff = new ChildNodeDefDiff(null, def2);
            if (diff.getType() > maxType) {
                maxType = diff.getType();
            }
            childNodeDefDiffs.add(diff);
        }

        return maxType;
    }

    @Override
    public String toString() {
        String result = getClass().getName() + "[\n\tnodeTypeName="
                + oldDef.getName();

        result += ",\n\tmixinFlagDiff=" + modificationTypeToString(mixinFlagDiff());
        result += ",\n\tsupertypesDiff=" + modificationTypeToString(supertypesDiff());

        result += ",\n\tpropertyDifferences=[\n";
        result += toString(propDefDiffs);
        result += "\t]";

        result += ",\n\tchildNodeDifferences=[\n";
        result += toString(childNodeDefDiffs);
        result += "\t]\n";
        result += "]\n";

        return result;
    }

    private String toString(List<? extends ChildItemDefDiff> childItemDefDiffs) {
        String result = "";
        for (Iterator iter = childItemDefDiffs.iterator(); iter.hasNext();) {
            ChildItemDefDiff propDefDiff = (ChildItemDefDiff) iter.next();
            result += "\t\t" + propDefDiff;
            if (iter.hasNext()) {
                result += ",";
            }
            result += "\n";
        }
        return result;
    }

    private String modificationTypeToString(int modificationType) {
        String typeString = "unknown";
        switch (modificationType) {
            case NONE:
                typeString = "NONE";
                break;
            case TRIVIAL:
                typeString = "TRIVIAL";
                break;
            case MAJOR:
                typeString = "MAJOR";
                break;
        }
        return typeString;
    }


    //--------------------------------------------------------< inner classes >

    abstract class ChildItemDefDiff {
        protected final QItemDefinition oldDef;
        protected final QItemDefinition newDef;
        protected int type;

        ChildItemDefDiff(QItemDefinition oldDef, QItemDefinition newDef) {
            this.oldDef = oldDef;
            this.newDef = newDef;
            init();
        }

        protected void init() {
            // determine type of modification
            if (isAdded()) {
                if (!newDef.isMandatory()) {
                    // adding a non-mandatory child item is a TRIVIAL change
                    type = TRIVIAL;
                } else {
                    // adding a mandatory child item is a MAJOR change
                    type = MAJOR;
                }
            } else if (isRemoved()) {
                // removing a child item is a MAJOR change
                type = MAJOR;
            } else {
                /**
                 * neither added nor removed => has to be either identical
                 * or modified
                 */
                if (oldDef.equals(newDef)) {
                    // identical
                    type = NONE;
                } else {
                    // modified
                    if (oldDef.isMandatory() != newDef.isMandatory()
                            && newDef.isMandatory()) {
                        // making a child item mandatory is a MAJOR change
                        type = MAJOR;
                    } else {
                        if (!oldDef.definesResidual()
                                && newDef.definesResidual()) {
                            // just making a child item residual is a TRIVIAL change
                            type = TRIVIAL;
                        } else {
                            if (!oldDef.getName().equals(newDef.getName())) {
                                // changing the name of a child item is a MAJOR change
                                type = MAJOR;
                            } else {
                                // all other changes are TRIVIAL
                                type = TRIVIAL;
                            }
                        }
                    }
                }
            }
        }

        public int getType() {
            return type;
        }

        public boolean isAdded() {
            return oldDef == null && newDef != null;
        }

        public boolean isRemoved() {
            return oldDef != null && newDef == null;
        }

        public boolean isModified() {
            return oldDef != null && newDef != null
                    && !oldDef.equals(newDef);
        }

        @Override
        public String toString() {
            String typeString = modificationTypeToString(getType());

            String operationString;
            if (isAdded()) {
                operationString = "ADDED";
            } else if (isModified()) {
                operationString = "MODIFIED";
            } else if (isRemoved()) {
                operationString = "REMOVED";
            } else {
                operationString = "NONE";
            }

            QItemDefinition itemDefinition = (oldDef != null) ? oldDef : newDef;

            return getClass().getName() + "[itemName="
                    + itemDefinition.getName() + ", type=" + typeString
                    + ", operation=" + operationString + "]";
        }

    }

    public class PropDefDiff extends ChildItemDefDiff {

        PropDefDiff(QPropertyDefinition oldDef, QPropertyDefinition newDef) {
            super(oldDef, newDef);
        }

        public QPropertyDefinition getOldDef() {
            return (QPropertyDefinition) oldDef;
        }

        public QPropertyDefinition getNewDef() {
            return (QPropertyDefinition) newDef;
        }

        @Override
        protected void init() {
            super.init();
            /**
             * only need to do comparison if base class implementation
             * detected a non-MAJOR (i.e. TRIVIAL) modification;
             * no need to check for additions or removals as this is already
             * handled in base class implementation.
             */
            if (isModified() && type == TRIVIAL) {
                // check if valueConstraints were made more restrictive
                QValueConstraint[] vca1 = getOldDef().getValueConstraints();
                Set<String> set1 = new HashSet<String>();
                for (QValueConstraint aVca1 : vca1) {
                    set1.add(aVca1.getString());
                }
                QValueConstraint[] vca2 = getNewDef().getValueConstraints();
                Set<String> set2 = new HashSet<String>();
                for (QValueConstraint aVca2 : vca2) {
                    set2.add(aVca2.getString());
                }

                if (!set1.equals(set2)) {
                    // valueConstraints have been modified
                    if (set2.containsAll(set1)) {
                        // new set is a superset of old set
                        // => constraints have been removed
                        // (TRIVIAL change, since constraints are OR'ed)
                        type = TRIVIAL;
                    } else {
                        // constraint have been removed/modified (MAJOR change);
                        // since we're unable to semantically compare
                        // value constraints (e.g. regular expressions), all
                        // modifications are considered a MAJOR change.
                        type = MAJOR;
                    }
                }

                // no need to check defaultValues (TRIVIAL change)
                // no need to check availableQueryOperators (TRIVIAL change)
                // no need to check queryOrderable (TRIVIAL change)

                if (type == TRIVIAL) {
                    int t1 = getOldDef().getRequiredType();
                    int t2 = getNewDef().getRequiredType();
                    if (t1 != t2) {
                        if (t2 == PropertyType.UNDEFINED) {
                            // changed getRequiredType to UNDEFINED (TRIVIAL change)
                            type = TRIVIAL;
                        } else {
                            // changed getRequiredType to specific type (MAJOR change)
                            type = MAJOR;
                        }
                    }
                    boolean b1 = getOldDef().isMultiple();
                    boolean b2 = getNewDef().isMultiple();
                    if (b1 != b2) {
                        if (b2) {
                            // changed multiple flag to true (TRIVIAL change)
                            type = TRIVIAL;
                        } else {
                            // changed multiple flag to false (MAJOR change)
                            type = MAJOR;
                        }
                    }
                }
            }
        }
    }

    public class ChildNodeDefDiff extends ChildItemDefDiff {

        ChildNodeDefDiff(QNodeDefinition oldDef, QNodeDefinition newDef) {
            super(oldDef, newDef);
        }

        public QNodeDefinition getOldDef() {
            return (QNodeDefinition) oldDef;
        }

        public QNodeDefinition getNewDef() {
            return (QNodeDefinition) newDef;
        }

        @Override
        protected void init() {
            super.init();
            /**
             * only need to do comparison if base class implementation
             * detected a non-MAJOR (i.e. TRIVIAL) modification;
             * no need to check for additions or removals as this is already
             * handled in base class implementation.
             */
            if (isModified() && type == TRIVIAL) {

                boolean b1 = getOldDef().allowsSameNameSiblings();
                boolean b2 = getNewDef().allowsSameNameSiblings();
                if (b1 != b2 && !b2) {
                    // changed sameNameSiblings flag to false (MAJOR change)
                    type = MAJOR;
                }

                // no need to check defaultPrimaryType (TRIVIAL change)

                if (type == TRIVIAL) {
                    List<Name> l1 = Arrays.asList(getOldDef().getRequiredPrimaryTypes());
                    List<Name> l2 = Arrays.asList(getNewDef().getRequiredPrimaryTypes());
                    if (!l1.equals(l2)) {
                        // requiredPrimaryTypes have been modified
                        if (l1.containsAll(l2)) {
                            // old list is a superset of new list
                            // => removed requiredPrimaryType (TRIVIAL change)
                            type = TRIVIAL;
                        } else {
                            // added/modified requiredPrimaryType (MAJOR change)
                            // todo check whether aggregate of old requiredTypes would include aggregate of new requiredTypes => trivial change
                            type = MAJOR;
                        }
                    }
                }
            }
        }
    }

    /**
     * Identifier used to identify corresponding property definitions
     */
    static class QPropertyDefinitionId {

        Name declaringNodeType;
        Name name;
        int requiredType;
        boolean definesResidual;
        boolean isMultiple;

        QPropertyDefinitionId(QPropertyDefinition def) {
            declaringNodeType = def.getDeclaringNodeType();
            name = def.getName();
            requiredType = def.getRequiredType();
            definesResidual = def.definesResidual();
            isMultiple = def.isMultiple();
        }

        //---------------------------------------< java.lang.Object overrides >
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof QPropertyDefinitionId) {
                QPropertyDefinitionId other = (QPropertyDefinitionId) obj;
                return declaringNodeType.equals(other.declaringNodeType)
                        && name.equals(other.name)
                        && requiredType == other.requiredType
                        && definesResidual == other.definesResidual
                        && isMultiple == other.isMultiple;
            }
            return false;
        }

        @Override
        public int hashCode() {
            int h = 17;
            h = 37 * h + declaringNodeType.hashCode();
            h = 37 * h + name.hashCode();
            h = 37 * h + (definesResidual ? 11 : 43);
            h = 37 * h + (isMultiple ? 11 : 43);
            h = 37 * h + requiredType;
            return h;
        }
    }

    /**
     * Identifier used to identify corresponding node definitions
     */
    static class QNodeDefinitionId {

        Name declaringNodeType;
        Name name;
        Name[] requiredPrimaryTypes;

        QNodeDefinitionId(QNodeDefinition def) {
            declaringNodeType = def.getDeclaringNodeType();
            name = def.getName();
            requiredPrimaryTypes = def.getRequiredPrimaryTypes();
            if (requiredPrimaryTypes == null || requiredPrimaryTypes.length == 0) {
                requiredPrimaryTypes = new Name[]{NameConstants.NT_BASE};
            }
            Arrays.sort(requiredPrimaryTypes);
        }

        //---------------------------------------< java.lang.Object overrides >
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof QNodeDefinitionId) {
                QNodeDefinitionId other = (QNodeDefinitionId) obj;
                return declaringNodeType.equals(other.declaringNodeType)
                        && name.equals(other.name)
                        && Arrays.equals(requiredPrimaryTypes, other.requiredPrimaryTypes);
            }
            return false;
        }

        @Override
        public int hashCode() {
            int h = 17;
            h = 37 * h + declaringNodeType.hashCode();
            h = 37 * h + name.hashCode();
            for (int i = 0; i < requiredPrimaryTypes.length; i++) {
                h = 37 * h + requiredPrimaryTypes[i].hashCode();
            }
            return h;
        }
    }
}
