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

import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.Join;
import javax.jcr.query.qom.QueryObjectModelFactory;

class ConstraintSplitInfo {

    private final QueryObjectModelFactory factory;

    private final Join source;

    private final List<Constraint> leftConstraints;

    private final List<Constraint> rightConstraints;

    private boolean isMultiple;

    private ConstraintSplitInfo leftInnerConstraints = null;

    private ConstraintSplitInfo rightInnerConstraints = null;

    public ConstraintSplitInfo(QueryObjectModelFactory factory, Join source) {
        this(factory, source, new ArrayList<Constraint>(),
                new ArrayList<Constraint>());
    }

    private ConstraintSplitInfo(QueryObjectModelFactory factory, Join source,
            List<Constraint> leftConstraints, List<Constraint> rightConstraints) {
        this.factory = factory;
        this.source = source;
        this.isMultiple = false;
        this.leftConstraints = leftConstraints;
        this.rightConstraints = rightConstraints;
    }

    public void addLeftConstraint(Constraint c) {
        if (isMultiple) {
            leftInnerConstraints.addLeftConstraint(c);
            leftInnerConstraints.addRightConstraint(c);
            return;
        }
        leftConstraints.add(c);
    }

    public void addRightConstraint(Constraint c) {
        if (isMultiple) {
            rightInnerConstraints.addLeftConstraint(c);
            rightInnerConstraints.addRightConstraint(c);
            return;
        }
        rightConstraints.add(c);
    }

    public void splitOr() {

        if (isMultiple) {
            // this should never happen
            return;
        }

        this.isMultiple = true;
        ConstraintSplitInfo csi1 = new ConstraintSplitInfo(factory, source,
                new ArrayList<Constraint>(leftConstraints),
                new ArrayList<Constraint>(rightConstraints));
        this.leftInnerConstraints = csi1;

        ConstraintSplitInfo csi2 = new ConstraintSplitInfo(factory, source,
                new ArrayList<Constraint>(leftConstraints),
                new ArrayList<Constraint>(rightConstraints));
        this.rightInnerConstraints = csi2;

        this.leftConstraints.clear();
        this.rightConstraints.clear();
    }

    public boolean isMultiple() {
        return isMultiple;
    }

    public ConstraintSplitInfo getLeftInnerConstraints() {
        return leftInnerConstraints;
    }

    public ConstraintSplitInfo getRightInnerConstraints() {
        return rightInnerConstraints;
    }

    public Join getSource() {
        return source;
    }

    /**
     * @return the left constraint
     */
    public Constraint getLeftConstraint() throws RepositoryException {
        return Constraints.and(factory, leftConstraints);
    }

    /**
     * @return the right constraint
     */
    public Constraint getRightConstraint() throws RepositoryException {
        return Constraints.and(factory, rightConstraints);
    }

    @Override
    public String toString() {
        if (isMultiple) {
            return "ConstraintSplitInfo [multiple=" + ", leftInnerConstraints="
                    + leftInnerConstraints + ", rightInnerConstraints="
                    + rightInnerConstraints + "]";
        }
        return "ConstraintSplitInfo [single" + ", leftConstraints="
                + leftConstraints + ", rightConstraints=" + rightConstraints
                + "]";
    }

}
