/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import javax.jcr.Session;
import javax.jcr.RepositoryException;
import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.BinaryValue;
import javax.jcr.BooleanValue;
import javax.jcr.DateValue;
import javax.jcr.DoubleValue;
import javax.jcr.LongValue;
import javax.jcr.NameValue;
import javax.jcr.PathValue;
import javax.jcr.StringValue;
import javax.jcr.nodetype.NodeDef;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDef;
import javax.jcr.util.ISO8601;
import java.util.Calendar;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.text.ParseException;

/**
 * Utility class to locate item definitions in the NodeTyeManager.
 */
class NodeTypeUtil {

    public static NodeDef locateChildNodeDef(Session session,
                                             boolean regardDefaultPrimaryType,
                                             boolean defaultPrimaryType,
                                             boolean residual)
        throws RepositoryException {

        NodeTypeManager manager = session.getWorkspace().getNodeTypeManager();
        NodeTypeIterator types = manager.getAllNodeTypes();

        boolean overjump = false;

        while (types.hasNext()) {
            NodeType type = types.nextNodeType();
            NodeDef nodeDefs[] = type.getDeclaredChildNodeDefs();

            for (int i = 0; i < nodeDefs.length; i++) {
                NodeDef nodeDef = nodeDefs[i];

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

                if (!residual && i == 0) {
                    // if another child node def is a residual definition
                    // overjump the current node type
                    NodeDef nodeDefsAll[] = type.getChildNodeDefs();
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
     * Locate a property def parsing all node types
     *
     * @param session:      the session to access the node types
     * @param propertyType: the type of the returned property. <cod>PropertyType.UNDEFINED</code>
     *                      returns a property of any type
     * @param multiple:     if true, the returned <code>PropertyDef</code> is
     *                      multiple, else not
     * @param isProtected:  if true, the returned <code>PropertyDef</code> is
     *                      protected, else not
     * @param residual:     if true, the returned <code>PropertyDef</code> is of
     *                      the residual name "*", else not
     * @return the first <code>PropertyDef</code> found fitting the
     *         requirements
     */
    public static PropertyDef locatePropertyDef(Session session,
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
            PropertyDef propDefs[] = type.getDeclaredPropertyDefs();
            for (int i = 0; i < propDefs.length; i++) {
                PropertyDef propDef = propDefs[i];

                // PropertyType.UNDEFINED is in use to get a property of any type
                if (propertyType != PropertyType.UNDEFINED &&
                        propDef.getRequiredType() != propertyType) {
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
                    try {
                        // check if a value out of constraint is buildable
                        Value v = getValueOutOfContstraint(propDef);
                        if (v == null) {
                            // no value out of the constraint range available
                            continue;
                        }
                    } catch (ParseException e) {
                        // an error occured
                        continue;
                    }
                }

                if (!residual && propDef.getName().equals("*")) {
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

        NodeDef nodeDefs[] = nodeType.getChildNodeDefs();
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
        return null;
    }

    /**
     * Returns any value of the requested type
     */
    public static Value getValueOfType(int type) throws ValueFormatException {
        switch (type) {
            case (PropertyType.BINARY):
                // note: If binary is not UTF-8 behavior is implementation-specific
               return new BinaryValue("abc");
            case (PropertyType.BOOLEAN):
                return new BooleanValue(true);
            case (PropertyType.DATE):
                return new DateValue(Calendar.getInstance());
            case (PropertyType.DOUBLE):
                return new DoubleValue(1.0);
            case (PropertyType.LONG):
                return new LongValue(1);
            case (PropertyType.NAME):
                return NameValue.valueOf("abc");
            case (PropertyType.PATH):
                return PathValue.valueOf("/abc");
            default:
                // STRING and UNDEFINED
                // note: REFERENCE is not testable since its format is implementation-specific
                return new StringValue("abc");
        }
    }

    /**
     * Returns a value out of the value constraints
     */
    public static Value getValueOutOfContstraint(PropertyDef propDef)
            throws ValueFormatException, RepositoryException, ParseException {

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
                                if (min < absMin) {
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
                                if (max > absMax) {
                                    absMax = max;
                                }
                            }
                        }
                    }
                    if (!minBoundless && absMin > 1) {
                        return new BinaryValue("0");
                    } else if (!maxBoundless) {
                        // build a binary value of size > absMax
                        StringBuffer content = new StringBuffer();
                        for (int i = 0; i <= absMax; i = i + 10) {
                            content.append("0123456789");
                        }
                        return new BinaryValue(content.toString());
                    } else {
                        return null;
                    }
                }

            case (PropertyType.BOOLEAN):
                {
                    if (constraints.length > 1) {
                        return null; // silly constraint
                    }
                    boolean value = !Boolean.valueOf(constraints[0]).booleanValue();
                    return new BooleanValue(value);
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
                    if (!minBoundless) {
                        absMin.setTimeInMillis(absMin.getTimeInMillis() - 1);
                        return new DateValue(absMin);
                    } else if (!maxBoundless) {
                        absMax.setTimeInMillis(absMax.getTimeInMillis() + 1);
                        return new DateValue(absMax);
                    } else {
                        return null;
                    }
                }

            case (PropertyType.DOUBLE):
                {
                    double absMin = 0;
                    double absMax = 0;

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
                                if (min < absMin) {
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
                                if (max > absMax) {
                                    absMax = max;
                                }
                            }
                        }
                    }
                    if (!minBoundless) {
                        return new DoubleValue(absMin - 1);
                    } else if (!maxBoundless) {
                        return new DoubleValue(absMax + 1);
                    } else {
                        return null;
                    }
                }

            case (PropertyType.LONG):
                {
                    long absMin = 0;
                    long absMax = 0;

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
                                if (min < absMin) {
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
                                if (max > absMax) {
                                    absMax = max;
                                }
                            }
                        }
                    }
                    if (!minBoundless) {
                        return new LongValue(absMin - 1);
                    } else if (!maxBoundless) {
                        return new LongValue(absMax + 1);
                    } else {
                        return null;
                    }
                }

            case (PropertyType.NAME):
                {
                    // build a name that is for sure not part of the constraints
                    StringBuffer name = new StringBuffer("X");
                    for (int i = 0; i < constraints.length; i++) {
                        name.append(constraints[i].replaceAll(":", ""));
                    }
                    return NameValue.valueOf(name.toString());
                }

            case (PropertyType.PATH):
                {
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

                    return PathValue.valueOf(pathStr);
                }

            case (PropertyType.UNDEFINED):
                {
                    return null;
                }

            default:
                {
                    // build a string that will probably not be part of the constraints
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
                    return new StringValue(value.toString());
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
