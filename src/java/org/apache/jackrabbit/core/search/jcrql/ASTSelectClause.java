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

import java.util.List;
import java.util.ArrayList;

public class ASTSelectClause extends SimpleNode {

    private List properties = new ArrayList();

    public ASTSelectClause(int id) {
	super(id);
    }

    public ASTSelectClause(JCRQLParser p, int id) {
	super(p, id);
    }

    public void addProperty(String prop) {
	properties.add(prop);
    }

    public String[] getProperties() {
	return (String[])properties.toArray(new String[properties.size()]);
    }

    /**
     * Accept the visitor. *
     */
    public Object jjtAccept(JCRQLParserVisitor visitor, Object data) {
	return visitor.visit(this, data);
    }
}
