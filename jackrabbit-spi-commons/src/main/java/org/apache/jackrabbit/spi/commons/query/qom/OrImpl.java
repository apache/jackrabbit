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
package org.apache.jackrabbit.spi.commons.query.qom;

import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.Or;

import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;

/**
 * <code>OrImpl</code>...
 */
public class OrImpl extends ConstraintImpl implements Or {

    /**
     * The first constraint.
     */
    private final ConstraintImpl constraint1;

    /**
     * The second constraint.
     */
    private final ConstraintImpl constraint2;

    OrImpl(NamePathResolver resolver, ConstraintImpl c1, ConstraintImpl c2) {
        super(resolver);
        this.constraint1 = c1;
        this.constraint2 = c2;
    }

    /**
     * Gets the first constraint.
     *
     * @return the constraint; non-null
     */
    public Constraint getConstraint1() {
        return constraint1;
    }

    /**
     * Gets the second constraint.
     *
     * @return the constraint; non-null
     */
    public Constraint getConstraint2() {
        return constraint2;
    }

    //------------------------< AbstractQOMNode >-------------------------------

    /**
     * Accepts a <code>visitor</code> and calls the appropriate visit method
     * depending on the type of this QOM node.
     *
     * @param visitor the visitor.
     */
    public Object accept(QOMTreeVisitor visitor, Object data) throws Exception {
        return visitor.visit(this, data);
    }

    //------------------------< Object >----------------------------------------

    public String toString() {
        return protect(constraint1) + " OR " + protect(constraint2);
    }

}
