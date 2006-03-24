<?php

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
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

require_once 'PHPCR/Value.php';
require_once 'PHPCR/nodetype/ItemDefinition.php';


/**
 * A property definition. Used in node type definitions.
 *
 * @author Markus Nix <mnix@mayflower.de>
 */
interface PropertyDefinition extends ItemDefinition
{
    /**
     * Gets the required type of the property. One of:
     * <ul>
     *   <li><code>PropertyType::STRING</code></li>
     *   <li><code>PropertyType::DATE</code></li>
     *   <li><code>PropertyType::BINARY</code></li>
     *   <li><code>PropertyType::DOUBLE</code></li>
     *   <li><code>PropertyType::LONG</code></li>
     *   <li><code>PropertyType::BOOLEAN</code></li>
     *   <li><code>PropertyType::NAME</code></li>
     *   <li><code>PropertyType::PATH</code></li>
     *   <li><code>PropertyType::REFERENCE</code></li>
     *   <li><code>PropertyType::UNDEFINED</code></li>
     * </ul>
     * <code>PropertyType.UNDEFINED</code> is returned if this property may be
     * of any type.
     *
     * @return an int
     */
    public function getRequiredType();

    /**
     * Gets the array of constraint strings. Each string in the array specifies
     * a constraint on the value of the property. The constraints are OR-ed together,
     * meaning that in order to be valid, the value must meet at least one of the
     * constraints. For example, a constraint array of <code>["constraint1", "constraint2",
     * "constraint3"]</code> has the interpretation: "the value of this property must
     * meet either constraint1, constraint2 or constraint3".
     * <p>
     * Reporting of value constraints is optional. An implementation may return
     * <code>null</code>, indicating that value constraint information is unavailable
     * (though a constraint may still exist).
     * <p/>
     * Returning an empty array, on the other hand, indicates that value constraint information
     * is available and that no constraints are placed on this value.
     * <p>
     * In the case of multi-value properties, the constraint string array
     * returned applies to all the values of the property.
     * <p>
     * The constraint strings themselves having differing formats and interpretations
     * depending on the type of the property in question. The following describes the
     * value constraint syntax for each property type:
     * <ul>
     * <li>
     * <code>STRING</code>: The constraint string is a regular expression pattern. For example the
     * regular expression "<code>.*</code>" means "any string, including the empty string". Whereas
     * a simple literal string (without any RE-specific meta-characters) like "<code>banana</code>"
     * matches only the string "<code>banana</code>".
     * </li>
     * <li>
     * <code>PATH</code>: The constraint string is a <i>JCR path</i> with an optional "<code>*</code>" character after
     * the last "<code>/</code>" character. For example,  possible constraint strings for a property
     * of type <code>PATH</code> include:
     * <ol>
     * <li>
     * "<code>/myapp:products/myapp:televisions</code>"
     * </li>
     * <li>
     * "<code>/myapp:products/myapp:televisions/</code>"
     * </li>
     * <li>
     * "<code>/myapp:products/*</code>"
     * </li>
     * <li>
     * "<code>myapp:products/myapp:televisions</code>"
     * </li>
     * <li>
     * "<code>../myapp:televisions</code>"
     * </li>
     * <li>
     * "<code>../myapp:televisions/*</code>"
     * </li>
     * </ol>
     * The following principles apply:
     * <ul>
     * <li>
     * The "*" means "matches descendants" not "matches any subsequent path". For example,
     * <code>/a/*</code> does not match <code>/a/../c</code>.
     * The constraint must match the normalized path.
     * </li>
     * <li>
     * Relative path constraint only match relative path values and absolute path
     * constraints only match absolute path values.
     * </li>
     * <li>
     * A trailing "<code>/</code>" has no effect (hence, <code>1</code> and <code>2</code>, above, are equivalent).
     * </li>
     * <li>
     * The trailing "<code>*</code>" character means that the value of the <code>PATH</code> property is
     * restricted to the indicated subtree (in other words any additional relative path
     * can replace the "<code>*</code>"). For example, 3, above would allow
     * <code>/myapp:products/myapp:radios</code>, <code>/myapp:products/myapp:microwaves/X900</code>, and so
     * forth.
     * </li>
     * <li>
     * A constraint without a "<code>*</code>" means that the <code>PATH</code> property is restricted to that
     * precise path. For example, <code>1</code>, above, would allow only the value
     * <code>/myapp:products/myapp:televisions</code>.
     * </li>
     * <li>
     * The constraint can indicate either a relative path or an absolute path
     * depending on whether it includes a leading "<code>/</code>" character. <code>1</code> and <code>4</code>, above for
     * example, are distinct.
     * </li>
     * <li>
     * The string returned must reflect the namespace mapping in the current <code>Session</code>
     * (i.e., the current state of the namespace registry overlaid with any
     * session-specific mappings). Constraint strings for <code>PATH</code> properties should be
     * stored in fully-qualified form (using the actual URI instead of the prefix) and
     * then be converted to prefix form according to the current mapping upon the
     * <code>PropertyDefinition.getValueConstraints</code> call.
     * </li>
     * </ul>
     * </li>
     * <li>
     * <code>NAME</code>: The constraint string is a <i>JCR name</i> in prefix form. For example
     * "<code>myapp:products</code>". No wildcards or other pattern matching are supported. As with
     * <code>PATH</code> properties, the string returned must reflect the namespace mapping in the
     * current <code>Session</code>. Constraint strings for <code>NAME</code> properties should be stored in
     * fully-qualified form (using the actual URI instead of the prefix) and then be
     * converted to prefix form according to the current mapping.
     * </li>
     * <li>
     * <code>REFERENCE</code>: The constraint string is a <i>JCR name</i> in prefix form. This name is
     * interpreted as a node type name and the <code>REFERENCE</code> property is restricted to
     * referring only to nodes that have at least the indicated node type. For
     * example, a constraint of "<code>mytype:document</code>" would indicate that the REFERENCE
     * property in question can only refer to nodes that have at least the node type
     * <code>mytype:document</code> (assuming this was the only constraint returned in the array,
     * recall that the array of constraints are to be "OR-ed" together). No wildcards or other
     * pattern matching are supported. As with <code>PATH</code> properties, the string returned
     * must reflect the namespace mapping in the current <code>Session</code>. Constraint strings
     * for <code>REFERENCE</code> properties should be stored in fully-qualified form (using the
     * actual URI instead of the prefix) and then be converted to prefix form according to the
     * current mapping.
     * </li>
     * <li>
     * <code>BOOLEAN</code>: Either "<code>true</code>" or "<code>false</code>".
     * </li>
     * </ul>
     * The remaining types all have value constraints in the form of inclusive or
     * exclusive ranges: i.e., "<code>[min, max]</code>", "<code>(min, max)</code>",
     * "<code>(min, max]</code>" or "<code>[min, max)</code>". Where "<code>[</code>"
     * and "<code>]</code>" indicate "inclusive", while "<code>(</code>" and "<code>)</code>"
     * indicate "exclusive". A missing <code>min</code> or <code>max</code> value
     * indicates no bound in that direction. For example [,5] means no minimum but a
     * maximum of 5 (inclusive) while [,] means simply that any value will suffice,
     * The meaning of the <code>min</code> and <code>max</code> values themselves
     * differ between types as follows:
     * <ul>
     * <li>
     * <code>BINARY</code>: <code>min</code> and <code>max</code> specify the allowed
     * size range of the binary value in bytes.
     * </li>
     * <li>
     * <code>DATE</code>: <code>min</code> and <code>max</code> are dates specifying the
     * allowed date range. The date strings must be in the ISO8601-compliant format:
     * <code>YYYY-MM-DDThh:mm:ss.sssTZD</code>.
     * </li>
     * <li>
     * <code>LONG</code>, <code>DOUBLE</code>: min and max are numbers.
     * </li>
     * </ul>
     * Because constraints are returned as an array of disjunctive constraints,
     * in many cases the elements of the array can serve directly as a "choice list".
     * This may, for example, be used by an application to display options to the
     * end user indicating the set of permitted values.
     *
     * @return a <code>String</code> array.
     */
    public function getValueConstraints();

    /**
     * Gets the default value(s) of the property. These are the values
     * that the property defined by this PropertyDefinition will be assigned if it
     * is automatically created (that is, if {@link #isAutoCreate()}
     * returns <code>true</code>).
     * <p>
     * This method returns an array of Value objects. If the property is
     * multi-valued, then this array represents the full set of values
     * that the property will be assigned upon being auto-created.
     * Note that this could be the empty array. If the property is single-valued,
     * then the array returned will be of size 1.
     * <p/>
     * If <code>null</code> is returned, then the property has no fixed default value.
     * This does not exclude the possibility that the property still assumes some
     * value automatically, but that value may be parameterized (for example,
     * "the current date") and hence not expressable as a single fixed value.
     * In particular, this <i>must</i> be the case if <code>isAutoCreate</code>
     * returns <code>true</code> and this method returns <code>null</code>.
     *
     * @return an array of <code>Value</code> objects.
     */
    public function getDefaultValues();

    /**
     * Reports whether this property can have multiple values. Note that the
     * <code>isMultiple</code> flag is special in that a given node type may
     * have two property definitions that are identical in every respect except
     * for the their <code>isMultiple</code> status. For example, a node type
     * can specify two string properties both called <code>X</code>, one of
     * which is multi-valued and the other not.
     *
     * @return a <code>boolean</code>
     */
    public function isMultiple();
}

?>
