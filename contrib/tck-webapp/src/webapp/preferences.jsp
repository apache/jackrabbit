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
                     org.apache.jackrabbit.tck.j2ee.RepositoryServlet,
                     javax.jcr.Node,
                     javax.jcr.RepositoryException,
                     java.util.HashMap,
                     java.io.IOException"
%><%@page session="false" %><%
Session repSession = RepositoryServlet.getSession();
if (repSession == null) {
    return;
}

String alertBox = "";

// save download key if needed
String key = request.getParameter("key");
if (key != null) {
    // save
    Node lk = repSession.getRootNode().getNode("licNode");
    if (!key.equals(lk.getProperty("key").getString())) {
        lk.setProperty("key", key);
        repSession.getRootNode().save();
    }
}

Node rootNode = repSession.getRootNode();

%>
<html>
    <head>
        <link rel="stylesheet" href="docroot/ui/default.css" type="text/css" title="style" />
    </head>
    <body style="margin-top:0px;border-width:0px">
        <%= alertBox %>
        <form name="prefsform" action="preferences.jsp" method="post">
            <table width="100%">
                <tr><td class="content">Licence Key</td><td class="content"><input name="key" value="<%= rootNode.getNode("licNode").getProperty("key").getString() %>"></td></tr>
                <tr><td colspan="2"><input type="submit" value="Save" class="submit"></td></tr>
            </table>
        </form>
    </body>
</html>