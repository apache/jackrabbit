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
package org.apache.jackrabbit.core.search.sql;

public class ASTPredicate extends SimpleNode {

    private int operationType;

    private boolean negate = false;

    private String identifier;

    private String identifierOperand;

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

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifierOperand(String identifier) {
        this.identifierOperand = identifier;
    }

    public String getIdentifierOperand() {
        return identifierOperand;
    }

  /** Accept the visitor. **/
  public Object jjtAccept(JCRSQLParserVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }

    public String toString() {
        return super.toString() + " type: " + operationType + " negate: " + negate;
    }
}
