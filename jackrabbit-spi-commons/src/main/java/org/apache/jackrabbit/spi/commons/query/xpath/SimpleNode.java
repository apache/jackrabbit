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
package org.apache.jackrabbit.spi.commons.query.xpath;


/**
 * Implements a JavaCC Node interface.
 * This Class was initially created by JavaCC and then adapted for our needs.
 */
public class SimpleNode implements Node {
    protected Node parent;
    protected Node[] children;
    protected int id;
    protected XPath parser;

    public SimpleNode(int i) {
        id = i;
    }

    public SimpleNode(XPath p, int i) {
        this(i);
        parser = p;
    }

    // Factory method
    public static Node jjtCreate(XPath p, int id) {
        return new SimpleNode(p, id);
    }

    public void jjtOpen() {
    }

    public void jjtClose() {
    }

    public void jjtSetParent(Node n) {
        parent = n;
    }

    public Node jjtGetParent() {
        return parent;
    }

    public void jjtAddChild(Node n, int i) {
        if (children == null) {
            children = new Node[i + 1];
        } else if (i >= children.length) {
            Node[] c = new Node[i + 1];
            System.arraycopy(children, 0, c, 0, children.length);
            children = c;
        }
        children[i] = n;
    }

    public Node jjtGetChild(int i) {
        return children[i];
    }

    public int jjtGetNumChildren() {
        return (children == null) ? 0 : children.length;
    }

    /**
     * Accept the visitor. *
     */
    public Object jjtAccept(XPathVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }

    /**
     * Accept the visitor. *
     */
    public Object childrenAccept(XPathVisitor visitor, Object data) {
        if (children != null) {
            for (int i = 0; i < children.length; ++i) {
                data = children[i].jjtAccept(visitor, data);
            }
        }
        return data;
    }

    /* You can override these two methods in subclasses of SimpleNode to
       customize the way the node appears when the tree is dumped.  If
       your output uses more than one line you should override
       toString(String), otherwise overriding toString() is probably all
       you need to do. */

    public String toString() {
        return XPathTreeConstants.jjtNodeName[id];
    }

    public String toString(String prefix) {
        return prefix + toString();
    }

    public void dump(String prefix) {
        dump(prefix, System.out);
    }

    public void dump(String prefix, java.io.PrintStream ps) {
        ps.print(toString(prefix));
        printValue(ps);
        ps.println();
        if (children != null) {
            for (int i = 0; i < children.length; ++i) {
                SimpleNode n = (SimpleNode) children[i];
                if (n != null) {
                    n.dump(prefix + "   ", ps);
                }
            }
        }
    }


    // Manually inserted code begins here

    protected String m_value;

    public void processToken(Token t) {
        m_value = t.image;
    }

    public void printValue(java.io.PrintStream ps) {
        if (null != m_value) {
            ps.print(" " + m_value);
        }
    }

    public int getId() {
        return id;
    }

    public String getValue() {
        return m_value;
    }

}


