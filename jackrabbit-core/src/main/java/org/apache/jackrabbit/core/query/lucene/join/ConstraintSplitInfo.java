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
import javax.jcr.query.qom.Or;
import javax.jcr.query.qom.QueryObjectModelFactory;

class ConstraintSplitInfo {

    private final QueryObjectModelFactory factory;

    private final List<Constraint> leftConstraints = new ArrayList<Constraint>();

    private final List<Constraint> rightConstraints = new ArrayList<Constraint>();

    private boolean isMultiple;

    private final List<ConstraintSplitInfo> innerConstraints = new ArrayList<ConstraintSplitInfo>();

    public ConstraintSplitInfo(QueryObjectModelFactory factory) {
        this.factory = factory;
        this.isMultiple = false;
    }

    private ConstraintSplitInfo(QueryObjectModelFactory factory,
            List<Constraint> leftConstraints, List<Constraint> rightConstraints) {
        this.factory = factory;
        this.isMultiple = false;
        this.leftConstraints.addAll(leftConstraints);
        this.rightConstraints.addAll(rightConstraints);
    }

    public void addLeftConstraint(Constraint c) {
        if (isMultiple) {
            for (ConstraintSplitInfo csi : innerConstraints) {
                csi.addLeftConstraint(c);
            }
            return;
        }
        leftConstraints.add(c);
    }

    public void addRightConstraint(Constraint c) {
        if (isMultiple) {
            for (ConstraintSplitInfo csi : innerConstraints) {
                csi.addRightConstraint(c);
            }
            return;
        }
        rightConstraints.add(c);
    }

    public void split(Or or) {
        if (isMultiple) {
            for (ConstraintSplitInfo csi : innerConstraints) {
                csi.split(or);
            }
            return;
        }

        this.isMultiple = true;

        ConstraintSplitInfo csi1 = new ConstraintSplitInfo(factory,
                leftConstraints, rightConstraints);
        csi1.addLeftConstraint(or.getConstraint1());
        this.innerConstraints.add(csi1);

        ConstraintSplitInfo csi2 = new ConstraintSplitInfo(factory,
                leftConstraints, rightConstraints);
        csi2.addLeftConstraint(or.getConstraint2());
        this.innerConstraints.add(csi2);

        // would null be better?
        this.leftConstraints.clear();
        this.rightConstraints.clear();
    }

    public boolean isMultiple() {
        return isMultiple;
    }

    public List<ConstraintSplitInfo> getInnerConstraints() {
        return innerConstraints;
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
}
