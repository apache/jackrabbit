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
package org.apache.jackrabbit.core.search.jcrql;

import java.util.ArrayList;
import java.util.List;

public class ASTOrderClause extends SimpleNode {

    private List properties = new ArrayList();

    private boolean ascending = false;

    public ASTOrderClause(int id) {
        super(id);
    }

    public ASTOrderClause(JCRQLParser p, int id) {
        super(p, id);
    }

    public void addProperty(String name) {
        properties.add(name);
    }

    public String[] getProperties() {
        return (String[]) properties.toArray(new String[properties.size()]);
    }

    public boolean isAscending() {
        return ascending;
    }

    public void setAscending(boolean ascending) {
        this.ascending = ascending;
    }

    /**
     * Accept the visitor. *
     */
    public Object jjtAccept(JCRQLParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }
}
