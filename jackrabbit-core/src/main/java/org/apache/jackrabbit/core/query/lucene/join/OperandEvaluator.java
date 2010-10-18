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
package org.apache.jackrabbit.core.query.lucene.join;

import static java.util.Locale.ENGLISH;
import static javax.jcr.PropertyType.NAME;

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.query.Row;
import javax.jcr.query.qom.BindVariableValue;
import javax.jcr.query.qom.FullTextSearchScore;
import javax.jcr.query.qom.Length;
import javax.jcr.query.qom.Literal;
import javax.jcr.query.qom.LowerCase;
import javax.jcr.query.qom.NodeLocalName;
import javax.jcr.query.qom.NodeName;
import javax.jcr.query.qom.Operand;
import javax.jcr.query.qom.PropertyValue;
import javax.jcr.query.qom.UpperCase;

class OperandEvaluator {

    private final ValueFactory factory;

    private final Map<String, Value> variables;

    public OperandEvaluator(
            ValueFactory factory, Map<String, Value> variables) {
        this.factory = factory;
        this.variables = variables;
    }

    public Value getValue(Operand operand, Row row) throws RepositoryException {
        Value[] values = getValues(operand, row);
        switch (values.length) {
        case 0:
            return factory.createValue("");
        case 1:
            return values[0];
        default:
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                if (i > 0) {
                    builder.append(' ');
                }
                builder.append(values[i].getString());
            }
            return factory.createValue(builder.toString());
        }
    }

    /**
     * Evaluates the given operand against the given row.
     *
     * @param operand operand
     * @param row row
     * @return values of the operand at the given row
     * @throws RepositoryException if the operand can't be evaluated
     */
    public Value[] getValues(Operand operand, Row row)
            throws RepositoryException {
        if (operand instanceof BindVariableValue) {
            return getBindVariableValues((BindVariableValue) operand);
        } else if (operand instanceof FullTextSearchScore) {
            return getFullTextSearchScoreValues(
                    (FullTextSearchScore) operand, row);
        } else if (operand instanceof Length) {
            return getLengthValues((Length) operand, row);
        } else if (operand instanceof Literal) {
            return getLiteralValues((Literal) operand);
        } else if (operand instanceof LowerCase) {
            return getLowerCaseValues((LowerCase) operand, row);
        } else if (operand instanceof NodeLocalName) {
            return getNodeLocalNameValues((NodeLocalName) operand, row);
        } else if (operand instanceof NodeName) {
            return getNodeNameValues((NodeName) operand, row);
        } else if (operand instanceof PropertyValue) {
            return getPropertyValues((PropertyValue) operand, row);
        } else if (operand instanceof UpperCase) {
            return getUpperCaseValues((UpperCase) operand, row);
        } else {
            throw new UnsupportedRepositoryOperationException(
                    "Unknown operand type: " + operand);
        }
    }

    /**
     * Returns the value of the given variable value operand at the given row.
     *
     * @param operand variable value operand
     * @return value of the operand at the given row
     */
    private Value[] getBindVariableValues(BindVariableValue operand) {
        Value value = variables.get(operand.getBindVariableName());
        if (value != null) {
            return new Value[] { value };
        } else {
            return new Value[0];
        }
    }

    /**
     * Returns the value of the given search score operand at the given row.
     *
     * @param operand search score operand
     * @param row row
     * @return value of the operand at the given row
     * @throws RepositoryException if the operand can't be evaluated
     */
    private Value[] getFullTextSearchScoreValues(
            FullTextSearchScore operand, Row row) throws RepositoryException {
        double score = row.getScore(operand.getSelectorName());
        return new Value[] { factory.createValue(score) };
    }

    /**
     * Returns the values of the given value length operand at the given row.
     *
     * @see #getProperty(PropertyValue, Row)
     * @param operand value length operand
     * @param row row
     * @return values of the operand at the given row
     * @throws RepositoryException if the operand can't be evaluated
     */
    private Value[] getLengthValues(Length operand, Row row)
            throws RepositoryException {
        Property property = getProperty(operand.getPropertyValue(), row);
        if (property == null) {
            return new Value[0];
        } else if (property.isMultiple()) {
            long[] lengths = property.getLengths();
            Value[] values = new Value[lengths.length];
            for (int i = 0; i < lengths.length; i++) {
                values[i] = factory.createValue(lengths[i]);
            }
            return values;
        } else {
            long length = property.getLength();
            return new Value[] { factory.createValue(length) };
        }
    }

    /**
     * Returns the value of the given literal value operand.
     *
     * @param operand literal value operand
     * @return value of the operand
     */
    private Value[] getLiteralValues(Literal operand) {
        return new Value[] { operand.getLiteralValue() };
    }

    /**
     * Returns the values of the given lower case operand at the given row.
     *
     * @param operand lower case operand
     * @param row row
     * @return values of the operand at the given row
     * @throws RepositoryException if the operand can't be evaluated
     */
    private Value[] getLowerCaseValues(LowerCase operand, Row row)
            throws RepositoryException {
        Value[] values = getValues(operand.getOperand(), row);
        for (int i = 0; i < values.length; i++) {
            String value = values[i].getString();
            String lower = value.toLowerCase(ENGLISH);
            if (!value.equals(lower)) {
                values[i] = factory.createValue(lower);
            }
        }
        return values;
    }

    /**
     * Returns the value of the given local name operand at the given row.
     *
     * @param operand local name operand
     * @param row row
     * @return value of the operand at the given row
     * @throws RepositoryException if the operand can't be evaluated
     */
    private Value[] getNodeLocalNameValues(NodeLocalName operand, Row row)
            throws RepositoryException {
        String name = row.getNode(operand.getSelectorName()).getName();
        int colon = name.indexOf(':');
        if (colon != -1) {
            name = name.substring(colon + 1);
        }
        return new Value[] { factory.createValue(name, NAME) };
    }

    /**
     * Returns the value of the given node name operand at the given row.
     *
     * @param operand node name operand
     * @param row row
     * @return value of the operand at the given row
     * @throws RepositoryException if the operand can't be evaluated
     */
    private Value[] getNodeNameValues(NodeName operand, Row row)
            throws RepositoryException {
        Node node = row.getNode(operand.getSelectorName());
        return new Value[] { factory.createValue(node.getName(), NAME) };
    }

    /**
     * Returns the values of the given property value operand at the given row.
     *
     * @see #getProperty(PropertyValue, Row)
     * @param operand property value operand
     * @param row row
     * @return values of the operand at the given row
     * @throws RepositoryException if the operand can't be evaluated
     */
    private Value[] getPropertyValues(PropertyValue operand, Row row)
            throws RepositoryException {
        Property property = getProperty(operand, row);
        if (property == null) {
            return new Value[0];
        } else if (property.isMultiple()) {
            return property.getValues();
        } else {
            return new Value[] { property.getValue() };
        }
    }

    /**
     * Returns the values of the given upper case operand at the given row.
     *
     * @param operand upper case operand
     * @param row row
     * @return values of the operand at the given row
     * @throws RepositoryException if the operand can't be evaluated
     */
    private Value[] getUpperCaseValues(UpperCase operand, Row row)
            throws RepositoryException {
        Value[] values = getValues(operand.getOperand(), row);
        for (int i = 0; i < values.length; i++) {
            String value = values[i].getString();
            String upper = value.toLowerCase(ENGLISH);
            if (!value.equals(upper)) {
                values[i] = factory.createValue(upper);
            }
        }
        return values;
    }

    /**
     * Returns the identified property from the given row. This method
     * is used by both the {@link #getValue(Length, Row)} and the
     * {@link #getValue(PropertyValue, Row)} methods to access properties.
     *
     * @param operand property value operand
     * @param row row
     * @return the identified property,
     *         or <code>null</code> if the property does not exist
     * @throws RepositoryException if the property can't be accessed
     */
    private Property getProperty(PropertyValue operand, Row row)
            throws RepositoryException {
        try {
            String selector = operand.getSelectorName();
            String property = operand.getPropertyName();
            return row.getNode(selector).getProperty(property);
        } catch (PathNotFoundException e) {
            return null;
        }
    }

}
