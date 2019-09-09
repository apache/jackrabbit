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
package org.apache.jackrabbit.spi.commons.query.sql;

import org.apache.jackrabbit.spi.Name;

public class ASTPredicate extends SimpleNode {

    private int operationType;

    private boolean negate = false;

    private Name identifier;

    private String identifierOperand;

    private String escapeString;

  public ASTPredicate(int id) {
    super(id);
  }

  public ASTPredicate(JCRSQLParser p, int id) {
    super(p, id);
  }

    public void setOperationType(int type) {
        this.operationType = type;
    }

    public int getOperationType() {
        return operationType;
    }

    public void setNegate(boolean b) {
        this.negate = b;
    }

    public boolean isNegate() {
        return this.negate;
    }

    public void setIdentifier(Name identifier) {
        this.identifier = identifier;
    }

    public Name getIdentifier() {
        return identifier;
    }

    public void setIdentifierOperand(String identifier) {
        this.identifierOperand = identifier;
    }

    public String getIdentifierOperand() {
        return identifierOperand;
    }

    public void setEscapeString(String esc) {
        this.escapeString = esc;
    }

    public String getEscapeString() {
        return escapeString;
    }

  /** Accept the visitor. **/
  public Object jjtAccept(JCRSQLParserVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }

    public String toString() {
        return super.toString() + " type: " + operationType + " negate: " + negate;
    }
}
