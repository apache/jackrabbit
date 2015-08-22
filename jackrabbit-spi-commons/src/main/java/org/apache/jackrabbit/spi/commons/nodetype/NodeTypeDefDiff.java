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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.PropertyType;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QItemDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QValueConstraint;
import org.apache.jackrabbit.spi.commons.name.NameConstants;

/**
 * A <code>NodeTypeDefDiff</code> represents the result of the comparison of
 * two node type definitions.
 * <p>
 * The result of the comparison can be categorized as one of the following types:
 * <p>
 * <b><code>NONE</code></b> indicates that there is no modification at all.
 * <p>
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
 * <p>
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
            PropDefDiffBuilder propDefDiffBuilder = new PropDefDiffBuilder(oldDef.getPropertyDefs(), newDef.getPropertyDefs());
            propDefDiffs.addAll(propDefDiffBuilder.getChildItemDefDiffs());
            tmpType = propDefDiffBuilder.getMaxType();
            if (tmpType > type) {
                type = tmpType;
            }

            // check child node definitions
            ChildNodeDefDiffBuilder childNodeDefDiffBuilder = new ChildNodeDefDiffBuilder(oldDef.getChildNodeDefs(), newDef.getChildNodeDefs());
            childNodeDefDiffs.addAll(childNodeDefDiffBuilder.getChildItemDefDiffs());
            tmpType = childNodeDefDiffBuilder.getMaxType();
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

    private abstract class ChildItemDefDiffBuilder<T extends QItemDefinition, V extends ChildItemDefDiff<T>> {

        private final List<V> childItemDefDiffs = new ArrayList<V>();

        private ChildItemDefDiffBuilder(T[] oldDefs, T[] newDefs) {
            buildChildItemDefDiffs(collectChildNodeDefs(oldDefs), collectChildNodeDefs(newDefs));
        }

        private void buildChildItemDefDiffs(Map<Object, List<T>> oldDefs, Map<Object, List<T>> newDefs) {
            for (Object defId : oldDefs.keySet()) {
                this.childItemDefDiffs.addAll(getChildItemDefDiffs(oldDefs.get(defId), newDefs.get(defId)));
                newDefs.remove(defId);
            }
            for (Object defId : newDefs.keySet()) {
                this.childItemDefDiffs.addAll(getChildItemDefDiffs(null, newDefs.get(defId)));
            }
        }

        private Map<Object, List<T>> collectChildNodeDefs(final T[] defs) {
            Map<Object, List<T>> result = new HashMap<Object, List<T>>();
            for (T def : defs) {
                final Object defId = createQItemDefinitionId(def);
                List<T> list = result.get(defId);
                if (list == null) {
                    list = new ArrayList<T>();
                    result.put(defId, list);
                }
                list.add(def);
            }
            return result;
        }

        abstract Object createQItemDefinitionId(T def);

        abstract V createChildItemDefDiff(T def1, T def2);

        Collection<V> getChildItemDefDiffs(List<T> defs1, List<T> defs2) {
            defs1 = defs1 != null ? defs1 : Collections.<T>emptyList();
            defs2 = defs2 != null ? defs2 : Collections.<T>emptyList();
            // collect all possible combinations of diffs
            final List<V> diffs = new ArrayList<V>();
            for (T def1 : defs1) {
                for (T def2 : defs2) {
                    diffs.add(createChildItemDefDiff(def1, def2));
                }
            }
            if (defs2.size() < defs1.size()) {
                for (T def1 : defs1) {
                    diffs.add(createChildItemDefDiff(def1, null));
                }
            }
            if (defs1.size() < defs2.size()) {
                for (T def2 : defs2) {
                    diffs.add(createChildItemDefDiff(null, def2));
                }
            }
            // sort them according to decreasing compatibility
            Collections.sort(diffs, new Comparator<V>() {
                @Override
                public int compare(final V o1, final V o2) {
                    return o1.getType() - o2.getType();
                }
            });
            // select the most compatible ones
            final int size = defs1.size() > defs2.size() ? defs1.size() : defs2.size();
            int allowedNewNull = defs1.size() - defs2.size();
            int allowedOldNull = defs2.size() - defs1.size();
            final List<V> results = new ArrayList<V>();
            for (V diff : diffs) {
                if (!alreadyMatched(results, diff.getNewDef(), diff.getOldDef(), allowedNewNull, allowedOldNull)) {
                    results.add(diff);
                    if (diff.getNewDef() == null) {
                        allowedNewNull--;
                    }
                    if (diff.getOldDef() == null) {
                        allowedOldNull--;
                    }
                }
                if (results.size() == size) {
                    break;
                }
            }
            return results;
        }

        private boolean alreadyMatched(final List<V> result, final T newDef, final T oldDef, final int allowedNewNull, final int allowedOldNull) {
            boolean containsNewDef = false, containsOldDef = false;
            for (V d : result) {
                if (d.getNewDef() != null && d.getNewDef().equals(newDef)) {
                    containsNewDef = true;
                    break;
                }
                if (d.getOldDef() != null && d.getOldDef().equals(oldDef)) {
                    containsOldDef = true;
                    break;
                }
            }
            if (oldDef == null) {
                if (allowedOldNull < 1) {
                    containsOldDef = true;
                }
            }
            if (newDef == null) {
                if (allowedNewNull < 1) {
                    containsNewDef = true;
                }
            }

            return containsNewDef || containsOldDef;
        }

        List<V> getChildItemDefDiffs() {
            return childItemDefDiffs;
        }

        int getMaxType() {
            int maxType = NONE;
            for (V childItemDefDiff : childItemDefDiffs) {
                if (childItemDefDiff.getType() > maxType) {
                    maxType = childItemDefDiff.getType();
                }
            }
            return maxType;
        }
    }

    private abstract class ChildItemDefDiff<T extends QItemDefinition> {
        protected final T oldDef;
        protected final T newDef;
        protected int type;

        private ChildItemDefDiff(T oldDef, T newDef) {
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

        T getOldDef() {
            return oldDef;
        }

        T getNewDef() {
            return newDef;
        }

        int getType() {
            return type;
        }

        boolean isAdded() {
            return oldDef == null && newDef != null;
        }

        boolean isRemoved() {
            return oldDef != null && newDef == null;
        }

        boolean isModified() {
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

    private class PropDefDiff extends ChildItemDefDiff<QPropertyDefinition> {

        private PropDefDiff(QPropertyDefinition oldDef, QPropertyDefinition newDef) {
            super(oldDef, newDef);
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
                    if (set2.isEmpty()) {
                        // all existing constraints have been cleared
                        // => TRIVIAL change
                        type = TRIVIAL;
                    } else if (set1.isEmpty()) {
                        // constraints have been set on a previously unconstrained property
                        // => MAJOR change
                        type = MAJOR;
                    } else if (set2.containsAll(set1)) {
                        // new set is a superset of old set,
                        // i.e. constraints have been weakened
                        // (since constraints are OR'ed)
                        // => TRIVIAL change
                        type = TRIVIAL;
                    } else {
                        // constraint have been removed/modified (MAJOR change);
                        // since we're unable to semantically compare
                        // value constraints (e.g. regular expressions), all
                        // such modifications are considered a MAJOR change.
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

    private class ChildNodeDefDiff extends ChildItemDefDiff<QNodeDefinition> {

        private ChildNodeDefDiff(QNodeDefinition oldDef, QNodeDefinition newDef) {
            super(oldDef, newDef);
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
                    Set<Name> s1 = new HashSet<Name>(Arrays.asList(getOldDef().getRequiredPrimaryTypes()));
                    Set<Name> s2 = new HashSet<Name>(Arrays.asList(getNewDef().getRequiredPrimaryTypes()));
                    // normalize sets by removing nt:base (adding/removing nt:base is irrelevant for the diff)
                    s1.remove(NameConstants.NT_BASE);
                    s2.remove(NameConstants.NT_BASE);
                    if (!s1.equals(s2)) {
                        // requiredPrimaryTypes have been modified
                        if (s1.containsAll(s2)) {
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
    private static class QPropertyDefinitionId {

        private Name declaringNodeType;
        private Name name;

        private QPropertyDefinitionId(QPropertyDefinition def) {
            declaringNodeType = def.getDeclaringNodeType();
            name = def.getName();
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
                        && name.equals(other.name);
            }
            return false;
        }

        @Override
        public int hashCode() {
            int h = 17;
            h = 37 * h + declaringNodeType.hashCode();
            h = 37 * h + name.hashCode();
            return h;
        }
    }

    /**
     * Identifier used to identify corresponding node definitions
     */
    private static class QNodeDefinitionId {

        private Name declaringNodeType;
        private Name name;

        private QNodeDefinitionId(QNodeDefinition def) {
            declaringNodeType = def.getDeclaringNodeType();
            name = def.getName();
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
                        && name.equals(other.name);
            }
            return false;
        }

        @Override
        public int hashCode() {
            int h = 17;
            h = 37 * h + declaringNodeType.hashCode();
            h = 37 * h + name.hashCode();
            return h;
        }
    }

    private class ChildNodeDefDiffBuilder extends ChildItemDefDiffBuilder<QNodeDefinition, ChildNodeDefDiff> {

        private ChildNodeDefDiffBuilder(final QNodeDefinition[] defs1, final QNodeDefinition[] defs2) {
            super(defs1, defs2);
        }

        @Override
        Object createQItemDefinitionId(final QNodeDefinition def) {
            return new QNodeDefinitionId(def);
        }

        @Override
        ChildNodeDefDiff createChildItemDefDiff(final QNodeDefinition def1, final QNodeDefinition def2) {
            return new ChildNodeDefDiff(def1, def2);
        }
    }

    private class PropDefDiffBuilder extends ChildItemDefDiffBuilder<QPropertyDefinition, PropDefDiff> {

        private PropDefDiffBuilder(final QPropertyDefinition[] defs1, final QPropertyDefinition[] defs2) {
            super(defs1, defs2);
        }

        @Override
        Object createQItemDefinitionId(final QPropertyDefinition def) {
            return new QPropertyDefinitionId(def);
        }

        @Override
        PropDefDiff createChildItemDefDiff(final QPropertyDefinition def1, final QPropertyDefinition def2) {
            return new PropDefDiff(def1, def2);
        }
    }

}
