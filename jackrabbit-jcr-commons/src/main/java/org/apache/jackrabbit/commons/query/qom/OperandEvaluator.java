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
package org.apache.jackrabbit.commons.query.qom;

import java.util.Locale;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyType;
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
import javax.jcr.query.qom.StaticOperand;
import javax.jcr.query.qom.UpperCase;

/**
 * Evaluator of QOM {@link Operand operands}. This class evaluates operands
 * in the context of a {@link ValueFactory value factory}, a set of bind
 * variables and possibly a query result row.
 */
public class OperandEvaluator {

    /** Value factory */
    private final ValueFactory factory;

    /** Bind variables */
    private final Map<String, Value> variables;

    /** The locale to use in upper- and lower-case conversion. */
    private final Locale locale;

    /**
     * Creates an operand evaluator for the given value factory and set of
     * bind variables. Upper- and lower-case conversions are performed using
     * the given locale.
     *
     * @param factory value factory
     * @param variables bind variables
     * @param locale locale to use in upper- and lower-case conversions
     */
    public OperandEvaluator(
            ValueFactory factory, Map<String, Value> variables, Locale locale) {
        this.factory = factory;
        this.variables = variables;
        this.locale = locale;
    }

    /**
     * Creates an operand evaluator for the given value factory and set of
     * bind variables. Upper- and lower-case conversions are performed using
     * the {@link Locale#ENGLISH}.
     *
     * @param factory value factory
     * @param variables bind variables
     */
    public OperandEvaluator(
            ValueFactory factory, Map<String, Value> variables) {
        this(factory, variables, Locale.ENGLISH);
    }

    /**
     * Returns the value of the given static operand
     * ({@link Literal literal} or {@link BindVariableValue bind variable})
     * casted to the given type.
     *
     * @param operand static operand to be evaluated
     * @param type expected value type
     * @return evaluated value, casted to the given type
     * @throws RepositoryException if a named bind variable is not found,
     *                             if the operand type is unknown, or
     *                             if the type conversion fails
     */
    public Value getValue(StaticOperand operand, int type)
            throws RepositoryException {
        Value value = getValue(operand);
        if (type == PropertyType.UNDEFINED || type == value.getType()) {
            return value;
        } if (type == PropertyType.LONG) {
            return factory.createValue(value.getLong());
        } if (type == PropertyType.DOUBLE) {
            return factory.createValue(value.getDouble());
        } if (type == PropertyType.DATE) {
            return factory.createValue(value.getDate());
        } else {
            return factory.createValue(value.getString(), type);
        }
    }

    /**
     * Returns the value of the given static operand
     * ({@link Literal literal} or {@link BindVariableValue bind variable}).
     *
     * @param operand static operand to be evaluated
     * @return evaluated value
     * @throws RepositoryException if a named bind variable is not found,
     *                             or if the operand type is unknown
     */
    public Value getValue(StaticOperand operand) throws RepositoryException {
        if (operand instanceof Literal) {
            Literal literal = (Literal) operand;
            return literal.getLiteralValue();
        } else if (operand instanceof BindVariableValue) {
            BindVariableValue bvv = (BindVariableValue) operand;
            Value value = variables.get(bvv.getBindVariableName());
            if (value != null) {
                return value;
            } else {
                throw new RepositoryException(
                        "Unknown bind variable: " + bvv.getBindVariableName());
            }
        } else {
            throw new UnsupportedRepositoryOperationException(
                    "Unknown static operand type: " + operand);
        }
    }

    /**
     * Returns the value of the given operand in the context of the given row.
     * This is a convenience method that uses a somewhat lossy best-effort
     * mapping to evaluate multi-valued operands to a single value. Use the
     * {@link #getValues(Operand, Row)} method for more accurate results.
     *
     * @param operand operand to be evaluated
     * @param row query result row
     * @return evaluated value
     * @throws RepositoryException if the operand can't be evaluated
     */
    public Value getValue(Operand operand, Row row) throws RepositoryException {
        Value[] values = getValues(operand, row);
        if (values.length == 1) {
            return values[0];
        } else {
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
     * Evaluates the given operand in the context of the given row.
     *
     * @param operand operand to be evaluated
     * @param row query result row
     * @return values of the operand at the given row
     * @throws RepositoryException if the operand can't be evaluated
     */
    public Value[] getValues(Operand operand, Row row)
            throws RepositoryException {
        if (operand instanceof StaticOperand) {
            StaticOperand so = (StaticOperand) operand;
            return new Value[] { getValue(so) };
        } else if (operand instanceof FullTextSearchScore) {
            FullTextSearchScore ftss = (FullTextSearchScore) operand;
            double score = row.getScore(ftss.getSelectorName());
            return new Value[] { factory.createValue(score) };
        } else if (operand instanceof NodeName) {
            NodeName nn = (NodeName) operand;
            String name = row.getNode(nn.getSelectorName()).getName();
            // root node
            if ("".equals(name)) {
                return new Value[] { factory.createValue(name,
                        PropertyType.STRING) };
            }
            return new Value[] { factory.createValue(name, PropertyType.NAME) };
        } else if (operand instanceof Length) {
            return getLengthValues((Length) operand, row);
        } else if (operand instanceof LowerCase) {
            return getLowerCaseValues((LowerCase) operand, row);
        } else if (operand instanceof UpperCase) {
            return getUpperCaseValues((UpperCase) operand, row);
        } else if (operand instanceof NodeLocalName) {
            return getNodeLocalNameValues((NodeLocalName) operand, row);
        } else if (operand instanceof PropertyValue) {
            return getPropertyValues((PropertyValue) operand, row);
        } else {
            throw new UnsupportedRepositoryOperationException(
                    "Unknown operand type: " + operand);
        }
    }

    /**
     * Evaluates the given operand in the context of the given node.
     *
     * @param operand operand to be evaluated
     * @param node node
     * @return values of the operand at the given node
     * @throws RepositoryException if the operand can't be evaluated
     */
    public Value[] getValues(Operand operand, Node node)
            throws RepositoryException {
        if (operand instanceof StaticOperand) {
            StaticOperand so = (StaticOperand) operand;
            return new Value[] { getValue(so) };
        }
        if (operand instanceof FullTextSearchScore) {
            final double defaultScore = 0.0;
            return new Value[] { factory.createValue(defaultScore) };
        }
        if (operand instanceof NodeName) {
            String name = node.getName();
            // root node
            if ("".equals(name)) {
                return new Value[] { factory.createValue(name,
                        PropertyType.STRING) };
            }
            return new Value[] { factory.createValue(name, PropertyType.NAME) };
        }
        if (operand instanceof Length) {
            return getLengthValues((Length) operand, node);
        }
        if (operand instanceof LowerCase) {
            return getLowerCaseValues((LowerCase) operand, node);
        }
        if (operand instanceof UpperCase) {
            return getUpperCaseValues((UpperCase) operand, node);
        }
        if (operand instanceof NodeLocalName) {
            return getNodeLocalNameValues((NodeLocalName) operand, node);
        }
        if (operand instanceof PropertyValue) {
            return getPropertyValues((PropertyValue) operand, node);
        }
        throw new UnsupportedRepositoryOperationException(
                "Unknown operand type: " + operand);
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
     * Returns the values of the given value length operand for the given node.
     *
     * @see #getProperty(PropertyValue, Node)
     * @param operand value length operand
     * @param node node
     * @return values of the operand for the given node
     * @throws RepositoryException if the operand can't be evaluated
     */
    private Value[] getLengthValues(Length operand, Node n)
            throws RepositoryException {
        Property property = getProperty(operand.getPropertyValue(), n);
        if (property == null) {
            return new Value[0];
        }
        if (property.isMultiple()) {
            long[] lengths = property.getLengths();
            Value[] values = new Value[lengths.length];
            for (int i = 0; i < lengths.length; i++) {
                values[i] = factory.createValue(lengths[i]);
            }
            return values;
        }
        long length = property.getLength();
        return new Value[] { factory.createValue(length) };
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
            String lower = value.toLowerCase(locale);
            if (!value.equals(lower)) {
                values[i] = factory.createValue(lower);
            }
        }
        return values;
    }

    /**
     * Returns the values of the given lower case operand for the given node.
     *
     * @param operand lower case operand
     * @param node node
     * @return values of the operand for the given node
     * @throws RepositoryException if the operand can't be evaluated
     */
    private Value[] getLowerCaseValues(LowerCase operand, Node node)
            throws RepositoryException {
        Value[] values = getValues(operand.getOperand(), node);
        for (int i = 0; i < values.length; i++) {
            String value = values[i].getString();
            String lower = value.toLowerCase(locale);
            if (!value.equals(lower)) {
                values[i] = factory.createValue(lower);
            }
        }
        return values;
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
            String upper = value.toUpperCase(locale);
            if (!value.equals(upper)) {
                values[i] = factory.createValue(upper);
            }
        }
        return values;
    }

    /**
     * Returns the values of the given upper case operand for the given node.
     *
     * @param operand upper case operand
     * @param node node
     * @return values of the operand for the given node
     * @throws RepositoryException if the operand can't be evaluated
     */
    private Value[] getUpperCaseValues(UpperCase operand, Node node)
            throws RepositoryException {
        Value[] values = getValues(operand.getOperand(), node);
        for (int i = 0; i < values.length; i++) {
            String value = values[i].getString();
            String upper = value.toUpperCase(locale);
            if (!value.equals(upper)) {
                values[i] = factory.createValue(upper);
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
        return getNodeLocalNameValues(operand,
                row.getNode(operand.getSelectorName()));
    }

    /**
     * Returns the value of the given local name operand for the given node.
     * 
     * @param operand
     *            local name operand
     * @param node
     *            node
     * @return value of the operand for the given node
     * @throws RepositoryException
     */
    private Value[] getNodeLocalNameValues(NodeLocalName operand, Node node)
            throws RepositoryException {
        String name = node.getName();

        // root node has no local name
        if ("".equals(name)) {
            return new Value[] { factory.createValue("", PropertyType.STRING) };
        }
        int colon = name.indexOf(':');
        if (colon != -1) {
            name = name.substring(colon + 1);
        }
        return new Value[] { factory.createValue(name, PropertyType.NAME) };
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

    private Value[] getPropertyValues(PropertyValue operand, Node node)
            throws RepositoryException {
        Property property = getProperty(operand, node);
        if (property == null) {
            return new Value[0];
        } else if (property.isMultiple()) {
            return property.getValues();
        } else {
            return new Value[] { property.getValue() };
        }
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
        return getProperty(operand, row.getNode(operand.getSelectorName()));
    }

    /**
     * Returns the identified property from the given node.
     * 
     * Can return <code>null</code> is the property doesn't exist or it is not
     * accessible.
     * 
     * @param operand
     * @param node
     * @return identified property
     * @throws RepositoryException
     */
    private Property getProperty(PropertyValue operand, Node node)
            throws RepositoryException {
        if (node == null) {
            return null;
        }
        try {
            return node.getProperty(operand.getPropertyName());
        } catch (PathNotFoundException e) {
            return null;
        }
    }
}
