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
package org.apache.jackrabbit.ntdoc.util;

import java.io.*;
import java.util.*;

/**
 * This class implements the html writer.
 */
public final class HtmlWriter {
    /**
     * Print writer.
     */
    private final PrintWriter out;

    /**
     * Element open.
     */
    private boolean elementOpen;

    /**
     * Element stack.
     */
    private final Stack elementStack;

    /**
     * Construct the document.
     */
    public HtmlWriter(Writer out) {
        this.out = new PrintWriter(out);
        this.elementStack = new Stack();
        this.elementOpen = false;
    }

    /**
     * Push element.
     */
    private void pushElement(String name) {
        closeElement(false);
        this.elementOpen = true;
        this.elementStack.push(name);
    }

    /**
     * Pop element.
     */
    private String popElement() {
        if (!this.elementStack.isEmpty()) {
            return (String) this.elementStack.pop();
        } else {
            return null;
        }
    }

    /**
     * Close element. Return true if closed.
     */
    private boolean closeElement(boolean leave) {
        if (this.elementOpen) {
            if (leave) {
                this.out.print("/>");
                popElement();
            } else {
                this.out.print(">");
            }

            this.elementOpen = false;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Start element.
     */
    public HtmlWriter enter(String name) {
        pushElement(name);
        this.out.print("<" + name);
        return this;
    }

    /**
     * Add attribute.
     */
    public HtmlWriter attrib(String name, String value) {
        if (this.elementOpen) {
            this.out.print(" " + name + "=\"" + value + "\"");
        }

        return this;
    }

    /**
     * Add text.
     */
    public HtmlWriter text(String value) {
        closeElement(false);
        this.out.print(value);
        return this;
    }

    /**
     * Add text.
     */
    public HtmlWriter spacer() {
        return text("&nbsp;");
    }

    /**
     * End element.
     */
    public HtmlWriter leave() {
        if (!closeElement(true)) {
            String name = popElement();
            if (name != null) {
                this.out.print("</" + name + ">");
            }
        }

        return this;
    }

    /**
     * Flush the document.
     */
    public void flush() {
        this.out.flush();
    }

    /**
     * Close the document.
     */
    public void close() {
        this.out.close();
    }
}

