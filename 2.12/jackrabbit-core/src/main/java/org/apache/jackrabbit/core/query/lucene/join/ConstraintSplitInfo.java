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

    private boolean hasLeftConstraints;

    private boolean hasRightConstraints;

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
        this.leftConstraints = leftConstraints;
        this.rightConstraints = rightConstraints;
        this.isMultiple = false;
        this.hasLeftConstraints = false;
        this.hasRightConstraints = false;
    }

    public void addLeftConstraint(Constraint c) {
        if (isMultiple) {
            leftInnerConstraints.addLeftConstraint(c);
            rightInnerConstraints.addLeftConstraint(c);
            return;
        }
        leftConstraints.add(c);
        this.hasLeftConstraints = true;
    }

    public void addRightConstraint(Constraint c) {
        if (isMultiple) {
            leftInnerConstraints.addRightConstraint(c);
            rightInnerConstraints.addRightConstraint(c);
            return;
        }
        rightConstraints.add(c);
        this.hasRightConstraints = true;
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
        csi1.hasLeftConstraints = this.hasLeftConstraints;
        csi1.hasRightConstraints = this.hasRightConstraints;
        this.leftInnerConstraints = csi1;

        ConstraintSplitInfo csi2 = new ConstraintSplitInfo(factory, source,
                new ArrayList<Constraint>(leftConstraints),
                new ArrayList<Constraint>(rightConstraints));
        csi2.hasLeftConstraints = this.hasLeftConstraints;
        csi2.hasRightConstraints = this.hasRightConstraints;
        this.rightInnerConstraints = csi2;

        this.leftConstraints.clear();
        this.rightConstraints.clear();
        this.hasLeftConstraints = false;
        this.hasRightConstraints = false;
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

    public boolean isHasLeftConstraints() {
        return hasLeftConstraints;
    }

    public boolean isHasRightConstraints() {
        return hasRightConstraints;
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
                + ", hasLeftConstraints=" + hasLeftConstraints
                + ", hasRightConstraints=" + hasRightConstraints + "]";
    }

}
