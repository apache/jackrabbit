/*
 * Copyright 2004 The Apache Software Foundation.
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
package org.apache.jackrabbit.core.search.jcrql;

public class ASTRelExpr extends SimpleNode {

    private String property;

    private int opType;

    public ASTRelExpr(int id) {
        super(id);
    }

    public ASTRelExpr(JCRQLParser p, int id) {
        super(p, id);
    }


    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public int getOperationType() {
        return opType;
    }

    public void setOperationType(int opType) {
        this.opType = opType;
    }

    /**
     * Accept the visitor. *
     */
    public Object jjtAccept(JCRQLParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }
}
