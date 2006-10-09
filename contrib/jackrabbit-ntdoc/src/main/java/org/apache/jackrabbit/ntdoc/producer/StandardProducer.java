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
package org.apache.jackrabbit.ntdoc.producer;

import java.io.*;

import org.apache.jackrabbit.ntdoc.util.*;
import org.apache.jackrabbit.ntdoc.model.*;

/**
 * This class implements a standard producer.
 */
public final class StandardProducer
        extends HtmlProducer {
    /**
     * Produce the documentation.
     */
    public void produce()
            throws IOException {
        produceCss();
        produceFrameSet();
        produceToc();
        produceNodeTypes();
    }

    /**
     * Produce the css.
     */
    private void produceCss()
            throws IOException {
        CssWriter out = createCssWriter("default.css");
        produceCss(out);
        out.close();
    }

    /**
     * Produce the css.
     */
    private void produceCss(CssWriter out)
            throws IOException {
        out.enter("body, th, td");
        out.attrib("font-family", "Arial, Helvetica, sans-serif");
        out.attrib("font-size", "12px");
        out.attrib("color", "#000000");
        out.leave();

        out.enter("th, td");
        out.attrib("padding-left", "4px");
        out.leave();

        out.enter("td");
        out.attrib("vertical-align", "top");
        out.attrib("white-space", "nowrap");
        out.attrib("border-right", "1px solid #999999");
        out.leave();

        out.enter("th");
        out.attrib("text-align", "left");
        out.attrib("font-weight", "normal");
        out.attrib("border-right", "1px solid #999999");
        out.attrib("border-bottom", "1px solid #999999");
        out.attrib("border-top", "1px solid #999999");
        out.attrib("background-color", "#DDDDDD");
        out.leave();

        out.enter("th.first-col");
        out.attrib("border-left", "1px solid #999999");
        out.leave();

        out.enter("td.first-col");
        out.attrib("border-left", "1px solid #999999");
        out.leave();

        out.enter("td.block-header");
        out.attrib("border-style", "none");
        out.attrib("font-weight", "bold");
        out.leave();

        out.enter("td.block-footer");
        out.attrib("border-right", "1px none #999999");
        out.attrib("border-top", "1px solid #999999");
        out.leave();

        out.enter("a, a:visited, a:active");
        out.attrib("color", "#000080");
        out.attrib("text-decoration", "none");
        out.leave();

        out.enter("a:hover");
        out.attrib("color", "#FFFFFF");
        out.attrib("background", "#000080");
        out.attrib("text-decoration", "none");
        out.leave();

        out.enter("tr.attr-first-row td");
        out.attrib("border-right", "1px solid #999999");
        out.attrib("border-top", "1px solid #999999");
        out.leave();

        out.enter("td.attr-first-col");
        out.attrib("text-align", "left");
        out.attrib("font-weight", "normal");
        out.attrib("border-left", "1px solid #999999");
        out.attrib("background-color", "#DDDDDD");
        out.leave();
    }

    /**
     * Produce the frame set.
     */
    private void produceFrameSet()
            throws IOException {
        HtmlWriter out = createHtmlWriter("index.html");
        produceFrameSet(out);
        out.close();
    }

    /**
     * Produce the frame set.
     */
    private void produceFrameSet(HtmlWriter out)
            throws IOException {
        NodeType firstNode = getNodeTypes().getNodeType(0);
        String firstName = firstNode != null ? getFileName("types", firstNode.getName()) : "";

        htmlBegin(out, getTitle(), "default.css", false);
        out.enter("frameset").attrib("cols", "20%,80%");
        out.enter("frame").attrib("src", "toc.html").attrib("name", "toc").leave();
        out.enter("frame").attrib("src", firstName).attrib("name", "body").leave();
        out.leave();
        htmlEnd(out, false);
    }

    /**
     * Produce the toc.
     */
    private void produceToc()
            throws IOException {
        HtmlWriter out = createHtmlWriter("toc.html");
        produceToc(out);
        out.close();
    }

    /**
     * Produce the toc.
     */
    private void produceToc(HtmlWriter out)
            throws IOException {
        htmlBegin(out, getTitle(), "default.css", true);
        produceNodeTypeToc(out, "Mixin Types", getNodeTypes().getMixinNodeTypes());
        produceNodeTypeToc(out, "Node Types", getNodeTypes().getConcreteNodeTypes());
        htmlEnd(out, true);
    }

    /**
     * Produce the toc.
     */
    private void produceNodeTypeToc(HtmlWriter out, String title, NodeType[] nodeTypes)
            throws IOException {
        if (nodeTypes.length > 0) {
            htmlHeading(out, 3, title);
            for (int i = 0; i < nodeTypes.length; i++) {
                produceNodeTypeTocLink(out, nodeTypes[i]);
            }
        }
    }

    /**
     * Produce toc link.
     */
    private void produceNodeTypeTocLink(HtmlWriter out, NodeType nt)
            throws IOException {
        String name = getFileName("types", nt.getName());
        out.enter("a").attrib("href", name).attrib("target", "body");
        out.text(nt.getName()).leave();
        out.enter("br").leave();
    }

    /**
     * Produce node type doc.
     */
    private void produceNodeTypes()
            throws IOException {
        NodeType[] list = getNodeTypes().getNodeTypes();
        for (int i = 0; i < list.length; i++) {
            produceNodeType(list[i]);
        }
    }

    /**
     * Produce node type doc.
     */
    private void produceNodeType(NodeType nt)
            throws IOException {
        HtmlWriter out = createHtmlWriter(getFileName("types", nt.getName()));
        produceNodeType(out, nt);
        out.close();
    }

    /**
     * Produce node type doc.
     */
    private void produceNodeType(HtmlWriter out, NodeType nt)
            throws IOException {
        htmlBegin(out, nt.getName(), "../default.css", true);
        htmlHeading(out, 2, nt.getName());
        out.enter("table").attrib("width", "100%").attrib("cellpadding", "1");
        out.attrib("cellspacing", "0").attrib("border", "0");

        produceAttribBlock(out, nt);
        produceNodeDefsBlock(out, nt);
        producePropertyDefsBlock(out, nt);

        out.leave();
        htmlEnd(out, true);
    }

    /**
     * Produce child node defs block.
     */
    private void produceNodeDefsBlock(HtmlWriter out, NodeType type)
            throws IOException {
        // Add normal definitions
        NodeDef[] defs = type.getNodeDefs();
        if (defs.length > 0) {
            produceBlockHeader(out, "Child Node Definitions");
            produceNodeDefHeader(out, false);

            for (int i = 0; i < defs.length; i++) {
                produceNodeDefData(out, defs[i], false);
            }

            produceBlockFooter(out);
        }

        // Add inherited definitions
        defs = type.getInheritedNodeDefs();
        if (defs.length > 0) {
            produceBlockHeader(out, "Inherited Child Node Definitions");
            produceNodeDefHeader(out, true);

            for (int i = 0; i < defs.length; i++) {
                produceNodeDefData(out, defs[i], true);
            }

            produceBlockFooter(out);
        }
    }

    /**
     * Produce property defs block.
     */
    private void producePropertyDefsBlock(HtmlWriter out, NodeType type)
            throws IOException {
        // Add normal definitions
        PropertyDef[] defs = type.getPropertyDefs();
        if (defs.length > 0) {
            produceBlockHeader(out, "Property Definitions");
            producePropertyDefHeader(out, false);

            for (int i = 0; i < defs.length; i++) {
                producePropertyDefData(out, defs[i], false);
            }

            produceBlockFooter(out);
        }

        // Add inherited definitions
        defs = type.getInheritedPropertyDefs();
        if (defs.length > 0) {
            produceBlockHeader(out, "Inherited Property Definitions");
            producePropertyDefHeader(out, true);

            for (int i = 0; i < defs.length; i++) {
                producePropertyDefData(out, defs[i], true);
            }

            produceBlockFooter(out);
        }
    }

    /**
     * Produce child def row.
     */
    private void produceNodeDefHeader(HtmlWriter out, boolean inherited)
            throws IOException {
        out.enter("tr");
        out.enter("th").attrib("class", "first-col").text("Name").leave();
        out.enter("th").attrib("title", "Declaring Node Type").text("Decl. Type").leave();
        out.enter("th").attrib("title", "Required Node Types").text("Req. Types").leave();
        out.enter("th").attrib("title", "Defining Node Type").attrib("colspan", "2").text("Def. Type").leave();
        out.enter("th").attrib("title", "On Parent Version").text("OPV").leave();
        out.enter("th").attrib("title", "Auto Created").text("AC").leave();
        out.enter("th").attrib("title", "Mandatory").text("Man").leave();
        out.enter("th").attrib("title", "Protected").text("Prot").leave();
        out.enter("th").attrib("title", "Same Name Siblings").text("SNS").leave();
        out.leave();
    }

    /**
     * Produce child def row.
     */
    private void produceNodeDefData(HtmlWriter out, NodeDef def, boolean inherited)
            throws IOException {
        out.enter("tr");
        out.enter("td").attrib("class", "first-col").text(def.getName()).leave();

        out.enter("td");
        produceValue(out, def.getDeclaringNodeType().getName(), true);
        out.leave();

        out.enter("td");
        produceValueList(out, def.getRequiredPrimaryTypes(), true);
        out.leave();

        out.enter("td").attrib("colspan", "2");
        produceValue(out, def.getDefaultPrimaryType(), true);
        out.leave();

        out.enter("td").text(def.getOnParentVersionString()).leave();
        produceBooleanFlag(out, def.isAutoCreated());
        produceBooleanFlag(out, def.isMandatory());
        produceBooleanFlag(out, def.isProtected());
        produceBooleanFlag(out, def.isMultiple());
    }

    /**
     * Produce property def row.
     */
    private void producePropertyDefHeader(HtmlWriter out, boolean inherited)
            throws IOException {
        out.enter("tr");
        out.enter("th").attrib("class", "first-col").text("Name").leave();
        out.enter("th").attrib("title", "Declaring Node Type").text("Decl. Type").leave();
        out.enter("th").attrib("title", "Required Type").text("Req. Type").leave();
        out.enter("th").attrib("title", "Default Value").text("Default").leave();
        out.enter("th").attrib("title", "Constraint").text("Constraint").leave();
        out.enter("th").attrib("title", "On Parent Version").text("OPV").leave();
        out.enter("th").attrib("title", "Auto Created").text("AC").leave();
        out.enter("th").attrib("title", "Mandatory").text("Man").leave();
        out.enter("th").attrib("title", "Protected").text("Prot").leave();
        out.enter("th").attrib("title", "Multiple Values").text("Mul").leave();
        out.leave();
    }

    /**
     * Produce property def row.
     */
    private void producePropertyDefData(HtmlWriter out, PropertyDef def, boolean inherited)
            throws IOException {
        out.enter("tr");
        out.enter("td").attrib("class", "first-col").text(def.getName()).leave();

        out.enter("td");
        produceValue(out, def.getDeclaringNodeType().getName(), true);
        out.leave();

        out.enter("td");
        produceValue(out, def.getRequiredTypeString(), false);
        out.leave();

        out.enter("td");
        produceValueList(out, def.getDefaultValues(), false);
        out.leave();

        out.enter("td");
        produceValueList(out, def.getConstraints(), false);
        out.leave();

        out.enter("td").text(def.getOnParentVersionString()).leave();
        produceBooleanFlag(out, def.isAutoCreated());
        produceBooleanFlag(out, def.isMandatory());
        produceBooleanFlag(out, def.isProtected());
        produceBooleanFlag(out, def.isMultiple());
    }

    /**
     * Produce attribute block.
     */
    private void produceAttribBlock(HtmlWriter out, NodeType nt)
            throws IOException {
        produceBlockHeader(out, "Node Type Attributes");
        // produceAttribHeader(out);
        produceAttribData(out, "Node Type Name", nt.getName(), true, false);
        produceAttribData(out, "Node Type Namespace", nt.getNamespace(), false, false);
        produceAttribData(out, "Mixin Node Type", String.valueOf(nt.isMixin()), false, false);
        produceAttribData(out, "Orderable child nodes", String.valueOf(nt.isOrderable()), false, false);
        produceAttribData(out, "Primary Item Name", nt.getPrimaryItemName(), false, false);
        produceAttribData(out, "Supertypes", nt.getSuperTypes(), false, true);
        produceBlockFooter(out);
    }

    /**
     * Block header.
     */
    private void produceBlockHeader(HtmlWriter out, String name)
            throws IOException {
        out.enter("tr");
        out.enter("td").attrib("class", "block-header").text(name).leave();
        out.leave();
    }

    /**
     * Block footer.
     */
    private void produceBlockFooter(HtmlWriter out)
            throws IOException {
        out.enter("tr");
        out.enter("td").attrib("class", "block-footer").attrib("colspan", "10");
        out.spacer().leave();
        out.leave();
    }

    /**
     * Produce attrib row.
     */
    private void produceAttribData(HtmlWriter out, String name, Object value, boolean first, boolean link)
            throws IOException {
        out.enter("tr");

        if (first) {
            out.attrib("class", "attr-first-row");
        }

        out.enter("td").attrib("class", "attr-first-col").text(name).leave();
        out.enter("td").attrib("colspan", "9");

        if (value instanceof String[]) {
            produceValueList(out, (String[]) value, link);
        } else if (value != null) {
            produceValue(out, value.toString(), link);
        }

        out.spacer();
        out.leave();
        out.leave();
    }

    /**
     * Produce name link.
     */
    private void produceValue(HtmlWriter out, String name, boolean link)
            throws IOException {
        if (link) {
            link = getNodeTypes().getNodeType(name) != null;
        }

        if (link) {
            out.enter("a").attrib("href", getFileName(null, name)).text(name.trim()).leave();
        } else {
            out.text(name);
        }

        out.spacer();
    }

    /**
     * Produce name link.
     */
    private void produceValueList(HtmlWriter out, String[] names, boolean link)
            throws IOException {
        if (names != null) {
            for (int i = 0; i < names.length; i++) {
                if (i > 0) {
                    out.enter("br").leave();
                }

                produceValue(out, names[i], link);
            }
        }

        out.spacer();
    }

    /**
     * Produce boolean flag.
     */
    private void produceBooleanFlag(HtmlWriter out, boolean value)
            throws IOException {
        out.enter("td");
        if (value) {
            out.enter("li").attrib("type", "disc").leave();
        }

        out.spacer();
        out.leave();
    }

    /**
     * Title tag.
     */
    private void htmlHeading(HtmlWriter out, int level, String title)
            throws IOException {
        out.enter("h" + level).text(title).leave();
    }

    /**
     * Html begin.
     */
    private void htmlBegin(HtmlWriter out, String title, String css, boolean body)
            throws IOException {
        out.enter("html");
        out.enter("head");
        out.enter("title").text(title).leave();
        out.enter("link").attrib("href", css).attrib("rel", "stylesheet").
                attrib("type", "text/css").leave();
        out.leave();

        if (body) {
            out.enter("body");
        }
    }

    /**
     * Html end.
     */
    private void htmlEnd(HtmlWriter out, boolean body)
            throws IOException {
        if (body) {
            out.leave();
        }

        out.leave();
    }

    /**
     * Return name of file.
     */
    private String getFileName(String prefix, String name) {
        String tmp = name.replace(':', '-') + ".html";
        if ((prefix != null) && (prefix.length() > 0)) {
            return prefix + "/" + tmp;
        } else {
            return tmp;
        }
    }
}
