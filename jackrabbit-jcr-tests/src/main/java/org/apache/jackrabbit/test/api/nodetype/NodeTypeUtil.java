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
package org.apache.jackrabbit.test.api.nodetype;

import org.apache.jackrabbit.test.ISO8601;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to locate item definitions in the NodeTyeManager.
 */
public class NodeTypeUtil {

    public static final int ANY_PROPERTY_TYPE = -1;

    /**
     * Locate a non-protected child node def parsing all node types
     *
     * @param session                  the session to access the node types
     * @param regardDefaultPrimaryType if true, the default primary type of the
     *                                 returned <code>NodeDef</code> is
     *                                 according to param <code>defaultPrimaryType</code>.
     *                                 If false, the returned <code>NodeDef</code>
     *                                 might have a default primary type or
     *                                 not.
     * @param defaultPrimaryType       if <code>regardDefaultPrimaryType</code>
     *                                 is true: if true, the returned
     *                                 <code>NodeDef</code> has a default
     *                                 primary type, else not
     * @param residual                 if true, the returned <code>NodeDef</code>
     *                                 is of the residual name "*", else not
     * @return
     * @throws RepositoryException
     */
    public static NodeDefinition locateChildNodeDef(Session session,
                                                    boolean regardDefaultPrimaryType,
                                                    boolean defaultPrimaryType,
                                                    boolean residual)
            throws RepositoryException {

        NodeTypeManager manager = session.getWorkspace().getNodeTypeManager();
        NodeTypeIterator types = manager.getAllNodeTypes();

        boolean overjump = false;

        while (types.hasNext()) {
            NodeType type = types.nextNodeType();

            // node types with more than one residual child node definition
            // will cause trouble in test cases. the implementation
            // might pick another definition than the definition returned by
            // this method, when a child node is set.
            NodeDefinition[] childDefs = type.getChildNodeDefinitions();
            int residuals = 0;
            for (int i = 0; i < childDefs.length; i++) {
                if (childDefs[i].getName().equals("*")) {
                    residuals++;
                }
            }
            if (residuals > 1) {
                // more than one residual, not suitable for tests
                continue;
            }

            NodeDefinition nodeDefs[] = type.getDeclaredChildNodeDefinitions();

            for (int i = 0; i < nodeDefs.length; i++) {
                NodeDefinition nodeDef = nodeDefs[i];

                if (nodeDef.isProtected()) {
                    continue;
                }

                if (nodeDef.getRequiredPrimaryTypes().length > 1) {
                    // behaviour of implementations that support multiple multiple inheritance
                    // of primary node types is not specified
                    continue;
                }

                if (regardDefaultPrimaryType) {

                    if (defaultPrimaryType && nodeDef.getDefaultPrimaryType() == null) {
                        continue;
                    }

                    if (!defaultPrimaryType && nodeDef.getDefaultPrimaryType() != null) {
                        continue;
                    }
                }

                if (residual && !nodeDef.getName().equals("*")) {
                    continue;
                }

                if (!residual) {
                    // if another child node def is a residual definition
                    // overjump the current node type
                    NodeDefinition nodeDefsAll[] = type.getChildNodeDefinitions();
                    for (int j = 0; j < nodeDefsAll.length; j++) {
                        if (nodeDefsAll[j].getName().equals("*")) {
                            overjump = true;
                            break;
                        }
                    }
                    if (overjump) {
                        // break the loop of the current child not defs
                        overjump = false;
                        break;
                    }
                }

                return nodeDef;
            }
        }
        return null;
    }

    /**
     * Locate a child node def parsing all node types
     *
     * @param session     the session to access the node types
     * @param isProtected if true, the returned <code>NodeDef</code> is
     *                    protected, else not
     * @param mandatory   if true, the returned <code>NodeDef</code> is
     *                    mandatory, else not
     * @return the first <code>NodeDef</code> found fitting the requirements
     */
    public static NodeDefinition locateChildNodeDef(Session session,
                                                    boolean isProtected,
                                                    boolean mandatory)
            throws RepositoryException {

        NodeTypeManager manager = session.getWorkspace().getNodeTypeManager();
        NodeTypeIterator types = manager.getAllNodeTypes();

        while (types.hasNext()) {
            NodeType type = types.nextNodeType();
            NodeDefinition nodeDefs[] = type.getDeclaredChildNodeDefinitions();

            for (int i = 0; i < nodeDefs.length; i++) {
                NodeDefinition nodeDef = nodeDefs[i];

                if (nodeDef.getName().equals("*")) {
                    continue;
                }

                if (isProtected && !nodeDef.isProtected()) {
                    continue;
                }
                if (!isProtected && nodeDef.isProtected()) {
                    continue;
                }

                if (mandatory && !nodeDef.isMandatory()) {
                    continue;
                }
                if (!mandatory && nodeDef.isMandatory()) {
                    continue;
                }

                return nodeDef;
            }
        }
        return null;
    }

    /**
     * Locate a property def parsing all node types
     *
     * @param session      the session to access the node types
     * @param propertyType the type of the returned property. -1 indicates to
     *                     return a property of any type but not UNDEFIEND
     * @param multiple     if true, the returned <code>PropertyDef</code> is
     *                     multiple, else not
     * @param isProtected  if true, the returned <code>PropertyDef</code> is
     *                     protected, else not
     * @param residual     if true, the returned <code>PropertyDef</code> is of
     *                     the residual name "*", else not
     * @return the first <code>PropertyDef</code> found fitting the
     *         requirements
     */
    public static PropertyDefinition locatePropertyDef(Session session,
                                                       int propertyType,
                                                       boolean multiple,
                                                       boolean isProtected,
                                                       boolean constraints,
                                                       boolean residual)
            throws RepositoryException {

        NodeTypeManager manager = session.getWorkspace().getNodeTypeManager();
        NodeTypeIterator types = manager.getAllNodeTypes();

        while (types.hasNext()) {
            NodeType type = types.nextNodeType();
            PropertyDefinition propDefs[] = type.getDeclaredPropertyDefinitions();
            for (int i = 0; i < propDefs.length; i++) {
                PropertyDefinition propDef = propDefs[i];

                if (propertyType != ANY_PROPERTY_TYPE &&
                        propDef.getRequiredType() != propertyType) {
                    continue;
                }

                if (propertyType == ANY_PROPERTY_TYPE &&
                        propDef.getRequiredType() == PropertyType.UNDEFINED) {
                    continue;
                }

                if (multiple && !propDef.isMultiple()) {
                    continue;
                }
                if (!multiple && propDef.isMultiple()) {
                    continue;
                }

                if (isProtected && !propDef.isProtected()) {
                    continue;
                }
                if (!isProtected && propDef.isProtected()) {
                    continue;
                }

                String vc[] = propDef.getValueConstraints();
                if (!constraints && vc != null && vc.length > 0) {
                    continue;
                }
                if (constraints) {
                    // property def with constraints requested
                    if (vc == null || vc.length == 0) {
                        // property def has no constraints
                        continue;
                    }
                }

                if (!residual && propDef.getName().equals("*")) {
                    continue;
                }

                if (residual && !propDef.getName().equals("*")) {
                    continue;
                }

                // also skip property residual property definition if there
                // is another residual definition
                if (residual) {
                    // check if there is another residual property def
                    if (getNumResidualPropDefs(type) > 1) {
                        continue;
                    }
                }

                if (!residual) {
                    // if not looking for a residual property def then there
                    // must not be any residual definition at all on the node
                    // type
                    if (getNumResidualPropDefs(type) > 0) {
                        continue;
                    }
                }

                return propDef;
            }
        }
        return null;
    }

    /**
     * Returns the number of residual property definitions of <code>type</code>
     * including its base types.
     * @param type the node type
     * @return the number of residual property definitions.
     */
    private static int getNumResidualPropDefs(NodeType type) {
        PropertyDefinition[] pDefs = type.getPropertyDefinitions();
        int residuals = 0;
        for (int j = 0; j < pDefs.length; j++) {
            PropertyDefinition pDef = pDefs[j];
            if (pDef.getName().equals("*")) {
                residuals++;
            }
        }
        return residuals;
    }

    /**
     * Locate a property def parsing all node types
     *
     * @param session     the session to access the node types
     * @param isProtected if true, the returned <code>PropertyDef</code> is
     *                    protected, else not
     * @param mandatory   if true, the returned <code>PropertyDef</code> is
     *                    mandatory, else not
     * @return the first <code>PropertyDef</code> found fitting the
     *         requirements
     */
    public static PropertyDefinition locatePropertyDef(Session session,
                                                       boolean isProtected,
                                                       boolean mandatory)
            throws RepositoryException {

        NodeTypeManager manager = session.getWorkspace().getNodeTypeManager();
        NodeTypeIterator types = manager.getAllNodeTypes();

        while (types.hasNext()) {
            NodeType type = types.nextNodeType();
            PropertyDefinition propDefs[] = type.getDeclaredPropertyDefinitions();
            for (int i = 0; i < propDefs.length; i++) {
                PropertyDefinition propDef = propDefs[i];

                if (propDef.getName().equals("*")) {
                    continue;
                }

                if (isProtected && !propDef.isProtected()) {
                    continue;
                }
                if (!isProtected && propDef.isProtected()) {
                    continue;
                }

                if (mandatory && !propDef.isMandatory()) {
                    continue;
                }
                if (!mandatory && propDef.isMandatory()) {
                    continue;
                }

                return propDef;
            }
        }
        return null;
    }

    /**
     * Returns a name that is not defined by the nodeType's child node def
     */
    public static String getUndefinedChildNodeName(NodeType nodeType) {

        NodeDefinition nodeDefs[] = nodeType.getChildNodeDefinitions();
        StringBuffer s = new StringBuffer("X");

        for (int i = 0; i < nodeDefs.length; i++) {
            s.append(nodeDefs[i].getName());
        }
        String undefinedName = s.toString();
        undefinedName = undefinedName.replaceAll("\\*", "");
        undefinedName = undefinedName.replaceAll(":", "");
        return undefinedName;
    }

    /**
     * Returns a node type that is nor legalType nor a sub type of of
     */
    public static String getIllegalChildNodeType(NodeTypeManager manager,
                                                 String legalType)
            throws RepositoryException {

        NodeTypeIterator types = manager.getAllNodeTypes();
        while (types.hasNext()) {
            NodeType type = types.nextNodeType();
            if (!type.getName().equals(legalType)) {
                NodeType superTypes[] = type.getSupertypes();
                boolean isSubType = false;
                for (int i = 0; i < superTypes.length; i++) {
                    String name = superTypes[i].getName();
                    if (name.equals(legalType)) {
                        isSubType = true;
                        break;
                    }
                }
                if (!isSubType) {
                    return type.getName();
                }
            }
        }
        return null;
    }

    /**
     * Returns any value of the requested type
     */
    public static Value getValueOfType(Session session, int type)
            throws ValueFormatException, UnsupportedOperationException, RepositoryException {
        switch (type) {
            case (PropertyType.BINARY):
                // note: If binary is not UTF-8 behavior is implementation-specific
                return session.getValueFactory().createValue("abc", PropertyType.BINARY);
            case (PropertyType.BOOLEAN):
                return session.getValueFactory().createValue(true);
            case (PropertyType.DATE):
                return session.getValueFactory().createValue(Calendar.getInstance());
            case (PropertyType.DOUBLE):
                return session.getValueFactory().createValue(1.0);
            case (PropertyType.LONG):
                return session.getValueFactory().createValue(1);
            case (PropertyType.NAME):
                return session.getValueFactory().createValue("abc", PropertyType.NAME);
            case (PropertyType.PATH):
                return session.getValueFactory().createValue("/abc", PropertyType.PATH);
            default:
                // STRING and UNDEFINED
                // note: REFERENCE is not testable since its format is implementation-specific
                return session.getValueFactory().createValue("abc");
        }
    }

    /**
     * Returns a value according to the value contraints of a
     * <code>PropertyDefinition</code>
     *
     * @param propDef   The <code>PropertyDefinition</code> whose constraints
     *                  will be regarded
     * @param satisfied If true, the returned <code>Value</code> will satisfying
     *                  the constraints - If false, the returned
     *                  <code>Value</code> will not satisfying the constraints.
     * @return Depending on param <code>satisfied</code> a <code>Value</code>
     *         satisfying or not satistying the constraints of
     *         <code>propDef</code> will be returned. Null will be returned if
     *         no accordant <code>Value</code> could be build.
     */
    public static Value getValueAccordingToValueConstraints(Session session,
                                                            PropertyDefinition propDef,
                                                            boolean satisfied)
            throws ValueFormatException, RepositoryException {

        int type = propDef.getRequiredType();
        String constraints[] = propDef.getValueConstraints();

        if (constraints == null || constraints.length == 0) {
            return null;
        }

        switch (type) {
            case (PropertyType.BINARY):
                {
                    long absMin = 0;
                    long absMax = 0;

                    // indicate if absMin and absMax are already set
                    boolean absMinSet = false;
                    boolean absMaxSet = false;

                    // boundless vars indicate min/max without bounds,
                    // if constraint is e.g.(min,) or [,max]
                    boolean maxBoundless = false;
                    boolean minBoundless = false;

                    // find smallest min and largest max value
                    for (int i = 0; i < constraints.length; i++) {
                        if (!minBoundless) {
                            String minStr = getConstraintMin(constraints[i]);
                            if (minStr == null) {
                                minBoundless = true;
                            } else {
                                long min = Long.valueOf(minStr).longValue();
                                if (!absMinSet) {
                                    absMin = min;
                                    absMinSet = true;
                                } else if (min < absMin) {
                                    absMin = min;
                                }
                            }
                        }
                        if (!maxBoundless) {
                            String maxStr = getConstraintMax(constraints[i]);
                            if (maxStr == null) {
                                maxBoundless = true;
                            } else {
                                long max = Long.valueOf(maxStr).longValue();
                                if (!absMaxSet) {
                                    absMax = max;
                                    absMaxSet = true;
                                } else if (max > absMax) {
                                    absMin = max;
                                }
                            }
                        }
                    }
                    if (satisfied) {
                        // build a binary value absMin < size > absMax
                        StringBuffer content = new StringBuffer();
                        for (int i = 0; i <= absMin + 1; i++) {
                            content.append("X");
                        }
                        if (!maxBoundless && content.length() >= absMax) {
                            return null;
                        } else {
                            return session.getValueFactory().createValue(content.toString(), PropertyType.BINARY);
                        }
                    } else {
                        if (!minBoundless && absMin > 1) {
                            // return a value of size < absMin
                            return session.getValueFactory().createValue("0", PropertyType.BINARY);
                        } else if (!maxBoundless) {
                            // build a binary value of size > absMax
                            StringBuffer content = new StringBuffer();
                            for (int i = 0; i <= absMax; i = i + 10) {
                                content.append("0123456789");
                            }
                            return session.getValueFactory().createValue(content.toString(), PropertyType.BINARY);
                        } else {
                            return null;
                        }
                    }
                }

            case (PropertyType.BOOLEAN):
                {
                    if (constraints.length > 1) {
                        return null; // silly constraint
                    }
                    boolean value = Boolean.valueOf(constraints[0]).booleanValue();
                    if (satisfied) {
                        return session.getValueFactory().createValue(value);
                    } else {
                        return session.getValueFactory().createValue(!value);
                    }
                }

            case (PropertyType.DATE):
                {
                    Calendar absMin = null;
                    Calendar absMax = null;

                    // boundless vars indicate min/max without bounds,
                    // if constraint is e.g.(min,) or [,max]
                    boolean maxBoundless = false;
                    boolean minBoundless = false;

                    // find smallest min and largest max value
                    for (int i = 0; i < constraints.length; i++) {
                        if (!minBoundless) {
                            String minStr = getConstraintMin(constraints[i]);
                            if (minStr == null) {
                                minBoundless = true;
                            } else {
                                Calendar min = ISO8601.parse(minStr);
                                if (absMin == null || min.before(absMin)) {
                                    absMin = min;
                                }
                            }
                        }
                        if (!maxBoundless) {
                            String maxStr = getConstraintMax(constraints[i]);
                            if (maxStr == null) {
                                maxBoundless = true;
                            } else {
                                Calendar max = ISO8601.parse(maxStr);
                                if (absMax == null || max.after(absMax)) {
                                    absMax = max;
                                }
                            }
                        }
                    }
                    if (satisfied) {
                        if (absMin != null) {
                            absMin.setTimeInMillis(absMin.getTimeInMillis() + 1);
                            if (absMin.after(absMax)) {
                                return null;
                            }
                            return session.getValueFactory().createValue(absMin);
                        } else if (absMax != null) {
                            absMax.setTimeInMillis(absMax.getTimeInMillis() - 1);
                            if (absMax.before(absMin)) {
                                return null;
                            }
                            return session.getValueFactory().createValue(absMax);
                        } else {
                            // neither min nor max set: return "now"
                            return session.getValueFactory().createValue(Calendar.getInstance());
                        }
                    } else {
                        if (!minBoundless) {
                            absMin.setTimeInMillis(absMin.getTimeInMillis() - 1);
                            return session.getValueFactory().createValue(absMin);
                        } else if (!maxBoundless) {
                            absMax.setTimeInMillis(absMax.getTimeInMillis() + 1);
                            return session.getValueFactory().createValue(absMax);
                        } else {
                            return null;
                        }
                    }
                }

            case (PropertyType.DOUBLE):
                {
                    double absMin = 0;
                    double absMax = 0;

                    // indicate if absMin and absMax are already set
                    boolean absMinSet = false;
                    boolean absMaxSet = false;

                    // boundless vars indicate min/max without bounds,
                    // if constraint is e.g.(min,) or [,max]
                    boolean maxBoundless = false;
                    boolean minBoundless = false;

                    // find smallest min and largest max value
                    for (int i = 0; i < constraints.length; i++) {
                        if (!minBoundless) {
                            String minStr = getConstraintMin(constraints[i]);
                            if (minStr == null) {
                                minBoundless = true;
                            } else {
                                double min = Double.valueOf(minStr).doubleValue();
                                if (!absMinSet) {
                                    absMin = min;
                                    absMinSet = true;
                                } else if (min < absMin) {
                                    absMin = min;
                                }
                            }
                        }
                        if (!maxBoundless) {
                            String maxStr = getConstraintMax(constraints[i]);
                            if (maxStr == null) {
                                maxBoundless = true;
                            } else {
                                double max = Double.valueOf(maxStr).doubleValue();
                                if (!absMaxSet) {
                                    absMax = max;
                                    absMaxSet = true;
                                } else if (max > absMax) {
                                    absMax = max;
                                }
                            }
                        }
                    }
                    if (satisfied) {
                        if (minBoundless) {
                            return session.getValueFactory().createValue(absMax - 1.0);
                        } else if (maxBoundless) {
                            return session.getValueFactory().createValue(absMin + 1.0);
                        } else if (absMin < absMax) {
                            double d = (absMin + absMax) / 2;
                            return session.getValueFactory().createValue(d);
                        } else {
                            return null;
                        }
                    } else {
                        if (!minBoundless) {
                            return session.getValueFactory().createValue(absMin - 1.0);
                        } else if (!maxBoundless) {
                            return session.getValueFactory().createValue(absMax + 1.0);
                        } else {
                            return null;
                        }
                    }
                }

            case (PropertyType.LONG):
                {
                    long absMin = 0;
                    long absMax = 0;

                    // indicate if absMin and absMax are already set
                    boolean absMinSet = false;
                    boolean absMaxSet = false;

                    // boundless vars indicate min/max without bounds,
                    // if constraint is e.g.(min,) or [,max]
                    boolean maxBoundless = false;
                    boolean minBoundless = false;

                    // find smallest min and largest max value
                    for (int i = 0; i < constraints.length; i++) {
                        if (!minBoundless) {
                            String minStr = getConstraintMin(constraints[i]);
                            if (minStr == null) {
                                minBoundless = true;
                            } else {
                                long min = Long.valueOf(minStr).longValue();
                                if (!absMinSet) {
                                    absMin = min;
                                    absMinSet = true;
                                } else if (min < absMin) {
                                    absMin = min;
                                }
                            }
                        }
                        if (!maxBoundless) {
                            String maxStr = getConstraintMax(constraints[i]);
                            if (maxStr == null) {
                                maxBoundless = true;
                            } else {
                                long max = Long.valueOf(maxStr).longValue();
                                if (!absMaxSet) {
                                    absMax = max;
                                    absMaxSet = true;
                                } else if (max > absMax) {
                                    absMax = max;
                                }
                            }
                        }
                    }
                    if (satisfied) {
                        if (minBoundless) {
                            return session.getValueFactory().createValue(absMax - 1);
                        } else if (maxBoundless) {
                            return session.getValueFactory().createValue(absMin + 1);
                        } else if (absMin < absMax - 1) {
                            long x = (absMin + absMax) / 2;
                            return session.getValueFactory().createValue(x);
                        } else {
                            return null;
                        }
                    } else {
                        if (!minBoundless) {
                            return session.getValueFactory().createValue(absMin - 1);
                        } else if (!maxBoundless) {
                            return session.getValueFactory().createValue(absMax + 1);
                        } else {
                            return null;
                        }
                    }
                }

            case (PropertyType.NAME):
                {
                    if (satisfied) {
                        // not in use so far
                        return null;
                    } else {
                        // build a name that is for sure not part of the constraints
                        StringBuffer name = new StringBuffer("X");
                        for (int i = 0; i < constraints.length; i++) {
                            name.append(constraints[i].replaceAll(":", ""));
                        }
                        return session.getValueFactory().createValue(name.toString(), PropertyType.NAME);
                    }
                }

            case (PropertyType.PATH):
                {
                    if (satisfied) {
                        // not in use so far
                        return null;
                    } else {
                        // build a path that is for sure not part of the constraints
                        StringBuffer path = new StringBuffer("X");
                        for (int i = 0; i < constraints.length; i++) {
                            path.append(constraints[i]);
                        }
                        String pathStr = path.toString();

                        // replace colon to avoid /a/x:b + y:c => /a/x:b:y:c
                        // where x:b:y:c is not a legal path element
                        pathStr = pathStr.replaceAll(":", "");
                        pathStr = pathStr.replaceAll("\\*", "");
                        pathStr = pathStr.replaceAll("//", "/");

                        return session.getValueFactory().createValue(pathStr, PropertyType.PATH);
                    }
                }

            case (PropertyType.UNDEFINED):
                {
                    return null;
                }

            default:
                {
                    if (satisfied) {
                        // not in use so far
                        return null;
                    } else {
                        // build a string that will probably not satisfy the constraints
                        StringBuffer value = new StringBuffer("X");
                        for (int i = 0; i < constraints.length; i++) {
                            value.append(constraints[i]);
                        }

                        // test if value does not match any of the constraints
                        for (int i = 0; i < constraints.length; i++) {
                            Pattern pattern = Pattern.compile(constraints[i]);
                            Matcher matcher = pattern.matcher(value);
                            if (matcher.matches()) {
                                return null;
                            }
                        }
                        return session.getValueFactory().createValue(value.toString());
                    }
                }
        }
    }

    // ------------------------< internal >-------------------------------------

    /**
     * Get the min value (as string) of a numeric/date constraint string
     */
    private static String getConstraintMin(String constraint) {
        String min = constraint.substring(0, constraint.indexOf(","));
        min = min.replaceAll("\\(", "");
        min = min.replaceAll("\\[", "");
        min = min.replaceAll(" ", "");
        if (min.equals("")) {
            min = null;
        }
        return min;
    }

    /**
     * Get the max value (as string) of a numeric/date constraint string
     */
    private static String getConstraintMax(String constraint) {
        String max = constraint.substring(constraint.indexOf(",") + 1);
        max = max.replaceAll("\\)", "");
        max = max.replaceAll("\\]", "");
        max = max.replaceAll(" ", "");
        if (max.equals("")) {
            max = null;
        }
        return max;
    }

}
