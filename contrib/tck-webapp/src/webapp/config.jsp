<%--
Copyright 2004-2005 The Apache Software Foundation or its licensors,
                    as applicable.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
--%><%@ page import="javax.jcr.Session,
                 javax.jcr.Node,
                 javax.jcr.NodeIterator,
                 java.text.SimpleDateFormat,
                 java.util.*,
                 org.apache.jackrabbit.tck.WebAppTestConfig,
                 org.apache.jackrabbit.test.JNDIRepositoryStub,
                 org.apache.jackrabbit.tck.WebAppTestConfig,
                 org.apache.jackrabbit.tck.j2ee.RepositoryServlet"
%><%@page session="false" %><%

Session repSession = RepositoryServlet.getSession(request);
if (repSession == null) {
    return;
}

String mode = request.getParameter("mode");

%><html>
    <head>
        <link rel="stylesheet" href="docroot/ui/default.css" type="text/css" title="style" />
    </head>
    <body style="margin-top:0px;border-width:0px">
        <table width="100%">
            <tr>
                <td colspan="3" id="technavcell">
                    <div id="technav">
                    <%
                    if (mode == null || !mode.equals("view")) {
                        %><span class="technavat">New Test</span><a href="config.jsp?mode=view">View Results</a><%
                    } else {
                        %><a href="config.jsp?mode=">New Test</a><span class="technavat">View Results</span></a><%
                    }
                        %>
                    </div>
                </td>
            </tr>
            <tr>
                <td colspan="3">
                    <%
                    if (mode == null || !mode.equals("view")) {
                        Map props = WebAppTestConfig.getOriConfig();
                        props.putAll(WebAppTestConfig.getConfig());
                        %>
                        <form name="test" action="graph.jsp" target="graph">
                            <table>
                                <tr class="content"><td class="content">Repository Lookup Name</td><td class="content"><input name="<%= JNDIRepositoryStub.REPOSITORY_LOOKUP_PROP %>" value="<%= props.get(JNDIRepositoryStub.REPOSITORY_LOOKUP_PROP) %>"></td><td class="content">Workspace Name</td><td class="content"><input name="<%= JNDIRepositoryStub.PROP_PREFIX + "." + JNDIRepositoryStub.PROP_WORKSPACE_NAME %>" value="<%= props.get(JNDIRepositoryStub.PROP_PREFIX + "." + JNDIRepositoryStub.PROP_WORKSPACE_NAME) %>"></td><td class="content">Description</td><td class="content"><input name="desc"></td><td>&nbsp;</td><td><input class="submit" type="submit" value="start"></td><td><input type="hidden" name="mode" value="testnow"</td></tr>
                                <tr><td class="content">Superuser Name</td><td class="content"><input name="<%= JNDIRepositoryStub.PROP_PREFIX + "." + JNDIRepositoryStub.PROP_SUPERUSER_NAME %>" value="<%= props.get(JNDIRepositoryStub.PROP_PREFIX + "." + JNDIRepositoryStub.PROP_SUPERUSER_NAME) %>"></td><td class="content">NodeType</td><td class="content"><input name="<%= JNDIRepositoryStub.PROP_PREFIX + "." + JNDIRepositoryStub.PROP_NODETYPE %>" value="<%= props.get(JNDIRepositoryStub.PROP_PREFIX + "." + JNDIRepositoryStub.PROP_NODETYPE) %>"></td></tr>
                                <tr><td class="content">Superuser Password</td><td class="content"><input name="<%= JNDIRepositoryStub.PROP_PREFIX + "." + JNDIRepositoryStub.PROP_SUPERUSER_PWD %>" value="<%= props.get(JNDIRepositoryStub.PROP_PREFIX + "." + JNDIRepositoryStub.PROP_SUPERUSER_PWD) %>"></td><td class="content">Node Name 1</td><td class="content"><input name="<%= JNDIRepositoryStub.PROP_PREFIX + "." + JNDIRepositoryStub.PROP_NODE_NAME1 %>" value="<%= props.get(JNDIRepositoryStub.PROP_PREFIX + "." + JNDIRepositoryStub.PROP_NODE_NAME1) %>"></td></tr>
                                <tr><td class="content">ReadWrite User Id</td><td class="content"><input name="<%= JNDIRepositoryStub.PROP_PREFIX + "." + JNDIRepositoryStub.PROP_READWRITE_NAME %>" value="<%= props.get(JNDIRepositoryStub.PROP_PREFIX + "." + JNDIRepositoryStub.PROP_READWRITE_NAME) %>"></td><td class="content">Node Name 2</td><td class="content"><input name="<%= JNDIRepositoryStub.PROP_PREFIX + "." + JNDIRepositoryStub.PROP_NODE_NAME2 %>" value="<%= props.get(JNDIRepositoryStub.PROP_PREFIX + "." + JNDIRepositoryStub.PROP_NODE_NAME2) %>"></td></tr>
                                <tr><td class="content">ReadWrite User Password</td><td class="content"><input name="<%= JNDIRepositoryStub.PROP_PREFIX + "." + JNDIRepositoryStub.PROP_READWRITE_PWD %>" value="<%= props.get(JNDIRepositoryStub.PROP_PREFIX + "." + JNDIRepositoryStub.PROP_READWRITE_PWD) %>"></td><td class="content">Node Name 3</td><td class="content"><input name="<%= JNDIRepositoryStub.PROP_PREFIX + "." + JNDIRepositoryStub.PROP_NODE_NAME3 %>" value="<%= props.get(JNDIRepositoryStub.PROP_PREFIX + "." + JNDIRepositoryStub.PROP_NODE_NAME3) %>"></td></tr>
                                <tr><td class="content">Readonly User Id</td><td class="content"><input name="<%= JNDIRepositoryStub.PROP_PREFIX + "." + JNDIRepositoryStub.PROP_READONLY_NAME %>" value="<%= props.get(JNDIRepositoryStub.PROP_PREFIX + "." + JNDIRepositoryStub.PROP_READONLY_NAME) %>"></td><td class="content">Naming Factory Name</td><td class="content"><input name="java.naming.factory.initial" value="<%= props.get("java.naming.factory.initial") %>"></td></tr>
                                <tr><td class="content">Readonly User Password</td><td class="content"><input name="<%= JNDIRepositoryStub.PROP_PREFIX + "." + JNDIRepositoryStub.PROP_READONLY_PWD %>" value="<%= props.get(JNDIRepositoryStub.PROP_PREFIX + "." + JNDIRepositoryStub.PROP_READONLY_PWD) %>"></td><td class="content">Naming Provider Url</td><td class="content"><input name="java.naming.provider.url" value="<%= props.get("java.naming.provider.url") %>"></td></tr>
                                <tr><td class="content">Test Root Path</td><td class="content"><input name="<%= JNDIRepositoryStub.PROP_PREFIX + "." + JNDIRepositoryStub.PROP_TESTROOT %>" value="<%= props.get(JNDIRepositoryStub.PROP_PREFIX + "." + JNDIRepositoryStub.PROP_TESTROOT) %>"></td></tr>
                            </table>
                        </form>
                    <%
                    } else {
                        %>
                        <form name="view" action="graph.jsp" target="graph">
                        <input type="hidden" name="mode" value="view">
                        <table>
                        <%
                        Node rootNode = repSession.getRootNode();

                        if (rootNode.hasNode("testing")) {
                            %>
                            <tr><td class="graph" valign="top">Select test to be viewed</td><td>
                            <select name="test" size="10" onchange="document.view.submit();">
                            <%
                            NodeIterator tests = rootNode.getNode("testing").getNodes();

                            ArrayList al = new ArrayList();
                            //hack : todo??
                            while (tests.hasNext()) {
                                al.add(tests.nextNode());
                            }

                            Collections.reverse(al);
                            Iterator itr = al.iterator();
                            // eoh

                            while (itr.hasNext()) {
                                Node n = (Node) itr.next();
                                String sdate = n.getName();
                                Calendar cal = Calendar.getInstance();
                                cal.setTimeInMillis(Long.parseLong(sdate));
                                SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy hh:mm:ss");
                                sdate = formatter.format(cal.getTime());
                                %><option value="<%= n.getName() %>"><%= sdate %><%
                            }
                            %>
                            </select>

                            </td></tr>
                        <%
                        }
                        %>
                        </table>
                        </form>
                        <%
                    }
                    %>
                </td>
            </tr>
        </table>
    </body>
</html>