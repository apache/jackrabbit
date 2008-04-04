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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.jcr.PropertyType;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QItemDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;

/**
 * A <code>NodeTypeDefDiff</code> represents the result of the comparison of
 * two node type definitions.
 * <p/>
 * The result of the comparison can be categorized as one of the following types:
 * <p/>
 * <b><code>NONE</code></b> inidcates that there is no modification at all.
 * <p/>
 * A <b><code>TRIVIAL</code></b> modification has no impact on the consistency
 * of existing content and does not affect existing/assigned definition id's.
 * The following modifications are considered <code>TRIVIAL</code>:
 * <ul>
 * <li>changing node type <code>orderableChildNodes</code> flag
 * <li>changing node type <code>primaryItemName</code> value
 * <li>adding non-<code>mandatory</code> property/child node
 * <li>changing property/child node <code>protected</code> flag
 * <li>changing property/child node <code>onParentVersion</code> value
 * <li>changing property/child node <code>mandatory</code> flag to <code>false</code>
 * <li>changing property/child node <code>autoCreated</code> flag
 * <li>changing child node <code>defaultPrimaryType</code>
 * <li>changing child node <code>sameNameSiblings</code> flag to <code>true</code>
 * <li>weaken property <code>valueConstraints</code> (e.g. by removing completely
 * or by adding to existing or by making a single constraint less restrictive)
 * <li>changing property <code>defaultValues</code>
 * </ul>
 * <p/>
 * A <b><code>MINOR</code></b> modification has no impact on the consistency
 * of existing content but <i>does</i> affect existing/assigned definition id's.
 * The following modifications are considered <code>MINOR</code>:
 * <ul>
 * <li>changing specific property/child node <code>name</code> to <code>*</code>
 * <li>weaken child node <code>requiredPrimaryTypes</code> (e.g. by removing)
 * <li>changing specific property <code>requiredType</code> to <code>undefined</code>
 * <li>changing property <code>multiple</code> flag to <code>true</code>
 * </ul>
 * <p/>
 * A <b><code>MAJOR</code></b> modification <i>affects</i> the consistency of
 * existing content and <i>does</i> change existing/assigned definition id's.
 * All modifications that are neither <b><code>TRIVIAL</code></b> nor
 * <b><code>MINOR</code></b> are considered <b><code>MAJOR</code></b>.
 *
 * @see #getType()
 */
public class NodeTypeDefDiff {

    /**
     * no modification
     */
    public static final int NONE = 0;
    /**
     * trivial modification: does neither affect consistency of existing content
     * nor does it change existing/assigned definition id's
     */
    public static final int TRIVIAL = 1;
    /**
     * minor modification: does not affect consistency of existing content but
     * <i>does</i> change existing/assigned definition id's
     */
    public static final int MINOR = 2;
    /**
     * major modification: <i>does</i> affect consistency of existing content
     * and <i>does</i> change existing/assigned definition id's
     */
    public static final int MAJOR = 3;

    private final QNodeTypeDefinition oldDef;
    private final QNodeTypeDefinition newDef;
    private int type;

    private final List propDefDiffs = new ArrayList();
    private final List childNodeDefDiffs = new ArrayList();

    /**
     * Constructor
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

            // no need to check orderableChildNodes flag (TRIVIAL modification)

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
     * @param oldDef
     * @param newDef
     * @return
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
     * @return
     */
    public boolean isModified() {
        return type != NONE;
    }

    /**
     * @return
     */
    public boolean isTrivial() {
        return type == TRIVIAL;
    }

    /**
     * @return
     */
    public boolean isMinor() {
        return type == MINOR;
    }

    /**
     * @return
     */
    public boolean isMajor() {
        return type == MAJOR;
    }

    /**
     * Returns the type of modification as expressed by the following constants:
     * <ul>
     * <li><b><code>NONE</code></b>: no modification at all
     * <li><b><code>TRIVIAL</code></b>: does neither affect consistency of
     * existing content nor does it change existing/assigned definition id's
     * <li><b><code>MINOR</code></b>: does not affect consistency of existing
     * content but <i>does</i> change existing/assigned definition id's
     * <li><b><code>MAJOR</code></b>: <i>does</i> affect consistency of existing
     * content and <i>does</i> change existing/assigned definition id's
     * </ul>
     *
     * @return the type of modification
     */
    public int getType() {
        return type;
    }

    /**
     * @return
     */
    public int mixinFlagDiff() {
        return oldDef.isMixin() != newDef.isMixin() ? MAJOR : NONE;
    }

    /**
     * @return
     */
    public int supertypesDiff() {
        return !Arrays.equals(oldDef.getSupertypes(), newDef.getSupertypes()) ? MAJOR : NONE;
    }

    /**
     * @return
     */
    private int buildPropDefDiffs() {
        /**
         * propDefId determinants: declaringNodeType, name, requiredType, multiple
         * todo: try also to match entries with modified id's
         */

        int maxType = NONE;
        QPropertyDefinition[] pda1 = oldDef.getPropertyDefs();
        HashMap defs1 = new HashMap();
        for (int i = 0; i < pda1.length; i++) {
            defs1.put(pda1[i].getName(), pda1[i]);
        }

        QPropertyDefinition[] pda2 = newDef.getPropertyDefs();
        HashMap defs2 = new HashMap();
        for (int i = 0; i < pda2.length; i++) {
            defs2.put(pda2[i].getName(), pda2[i]);
        }

        /**
         * walk through defs1 and process all entries found in
         * both defs1 & defs2 and those found only in defs1
         */
        Iterator iter = defs1.keySet().iterator();
        while (iter.hasNext()) {
            Name name = (Name) iter.next();
            QPropertyDefinition def1 = (QPropertyDefinition) defs1.get(name);
            QPropertyDefinition def2 = (QPropertyDefinition) defs2.get(name);
            PropDefDiff diff = new PropDefDiff(def1, def2);
            if (diff.getType() > maxType) {
                maxType = diff.getType();
            }
            propDefDiffs.add(diff);
            defs2.remove(name);
        }

        /**
         * defs2 by now only contains entries found in defs2 only;
         * walk through defs2 and process all remaining entries
         */
        iter = defs2.keySet().iterator();
        while (iter.hasNext()) {
            Name name = (Name) iter.next();
            QPropertyDefinition def = (QPropertyDefinition) defs2.get(name);
            PropDefDiff diff = new PropDefDiff(null, def);
            if (diff.getType() > maxType) {
                maxType = diff.getType();
            }
            propDefDiffs.add(diff);
        }

        return maxType;
    }

    /**
     * @return
     */
    private int buildChildNodeDefDiffs() {
        /**
         * nodeDefId determinants: declaringNodeType, name, requiredPrimaryTypes
         * todo: try also to match entries with modified id's
         */

        int maxType = NONE;
        QNodeDefinition[] cnda1 = oldDef.getChildNodeDefs();
        HashMap defs1 = new HashMap();
        for (int i = 0; i < cnda1.length; i++) {
            defs1.put(cnda1[i].getName(), cnda1[i]);
        }

        QNodeDefinition[] cnda2 = newDef.getChildNodeDefs();
        HashMap defs2 = new HashMap();
        for (int i = 0; i < cnda2.length; i++) {
            defs2.put(cnda2[i].getName(), cnda2[i]);
        }

        /**
         * walk through defs1 and process all entries found in
         * both defs1 & defs2 and those found only in defs1
         */
        Iterator iter = defs1.keySet().iterator();
        while (iter.hasNext()) {
            Name name = (Name) iter.next();
            QNodeDefinition def1 = (QNodeDefinition) defs1.get(name);
            QNodeDefinition def2 = (QNodeDefinition) defs2.get(name);
            ChildNodeDefDiff diff = new ChildNodeDefDiff(def1, def2);
            if (diff.getType() > maxType) {
                maxType = diff.getType();
            }
            childNodeDefDiffs.add(diff);
            defs2.remove(name);
        }

        /**
         * defs2 by now only contains entries found in defs2 only;
         * walk through defs2 and process all remaining entries
         */
        iter = defs2.keySet().iterator();
        while (iter.hasNext()) {
            Name name = (Name) iter.next();
            QNodeDefinition def = (QNodeDefinition) defs2.get(name);
            ChildNodeDefDiff diff = new ChildNodeDefDiff(null, def);
            if (diff.getType() > maxType) {
                maxType = diff.getType();
            }
            childNodeDefDiffs.add(diff);
        }

        return maxType;
    }

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

    private String toString(List childItemDefDiffs) {
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

    private String modificationTypeToString(int modifcationType) {
        String typeString = "unknown";
        switch (modifcationType) {
            case NONE:
                typeString = "NONE";
                break;
            case TRIVIAL:
                typeString = "TRIVIAL";
                break;
            case MINOR:
                typeString = "MINOR";
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
                            // just making a child item residual is a MINOR change
                            type = MINOR;
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

        protected void init() {
            super.init();
            /**
             * only need to do comparison if base class implementation
             * detected a non-MAJOR modification (i.e. TRIVIAL or MINOR);
             * no need to check for additions or removals as this is already
             * handled in base class implementation.
             */
            if (isModified() && type != NONE && type != MAJOR) {
                /**
                 * check if valueConstraints were made more restrictive
                 * (constraints are ORed)
                 */
                String[] vca1 = getOldDef().getValueConstraints();
                HashSet set1 = new HashSet();
                for (int i = 0; i < vca1.length; i++) {
                    set1.add(vca1[i]);
                }
                String[] vca2 = getNewDef().getValueConstraints();
                HashSet set2 = new HashSet();
                for (int i = 0; i < vca2.length; i++) {
                    set2.add(vca2[i]);
                }

                if (set1.isEmpty() && !set2.isEmpty()) {
                    // added constraint where there was no constraint (MAJOR change)
                    type = MAJOR;
                } else if (!set2.containsAll(set1) && !set2.isEmpty()) {
                    // removed existing constraint (MAJOR change)
                    type = MAJOR;
                }

                // no need to check defaultValues (TRIVIAL change)

                if (type == TRIVIAL) {
                    int t1 = getOldDef().getRequiredType();
                    int t2 = getNewDef().getRequiredType();
                    if (t1 != t2) {
                        if (t2 == PropertyType.UNDEFINED) {
                            // changed getRequiredType to UNDEFINED (MINOR change)
                            type = MINOR;
                        } else {
                            // changed getRequiredType to specific type (MAJOR change)
                            type = MAJOR;
                        }
                    }
                    boolean b1 = getOldDef().isMultiple();
                    boolean b2 = getNewDef().isMultiple();
                    if (b1 != b2) {
                        if (b2) {
                            // changed multiple flag to true (MINOR change)
                            type = MINOR;
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

        protected void init() {
            super.init();
            /**
             * only need to do comparison if base class implementation
             * detected a non-MAJOR modification (i.e. TRIVIAL or MINOR);
             * no need to check for additions or removals as this is already
             * handled in base class implementation.
             */
            if (isModified() && type != NONE && type != MAJOR) {

                boolean b1 = getOldDef().allowsSameNameSiblings();
                boolean b2 = getNewDef().allowsSameNameSiblings();
                if (b1 != b2 && !b2) {
                    // changed sameNameSiblings flag to false (MAJOR change)
                    type = MAJOR;
                }

                // no need to check defaultPrimaryType (TRIVIAL change)

                if (type == TRIVIAL) {
                    List l1 = Arrays.asList(getOldDef().getRequiredPrimaryTypes());
                    List l2 = Arrays.asList(getNewDef().getRequiredPrimaryTypes());
                    if (!l1.equals(l2)) {
                        if (l1.containsAll(l2)) {
                            // removed requiredPrimaryType (MINOR change)
                            type = MINOR;
                        } else {
                            // added requiredPrimaryType (MAJOR change)
                            type = MAJOR;
                        }
                    }
                }
            }
        }
    }
}
