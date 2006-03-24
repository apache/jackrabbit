<%--
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  The ASF licenses this file to You
   under the Apache License, Version 2.0 (the "License"); you may not
   use this file except in compliance with the License.
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
                     java.io.IOException,
                     java.util.List,
                     org.apache.commons.fileupload.FileUpload,
                     org.apache.commons.fileupload.DiskFileUpload,
                     org.apache.commons.fileupload.FileItem,
                     java.util.Iterator,
                     java.util.Properties"
%><%@page session="false" %><%
Session repSession = RepositoryServlet.getSession();
if (repSession == null) {
    return;
}
Node rootNode = repSession.getRootNode();

// save download key if needed
String key = request.getParameter("key");
if (key != null) {
    // save
    Node lk = rootNode.getNode("licNode");
    if (!key.equals(lk.getProperty("key").getString())) {
        lk.setProperty("key", key);
        rootNode.save();
    }
}

// save exclude list
if(request.getMethod().toLowerCase().equals("post")) {
    DiskFileUpload upload = new DiskFileUpload();

    // Parse the request
    List items = upload.parseRequest(request);

    // Process the uploaded items
    Iterator iter = items.iterator();
    while (iter.hasNext()) {
        FileItem item = (FileItem) iter.next();
        if (!item.isFormField() && item.getFieldName().equals("elfile")) {
            Node excludeListNode = (rootNode.hasNode("excludeList")) ?
            rootNode.getNode("excludeList") :
            rootNode.addNode("excludeList", "nt:unstructured");

            Properties props = new Properties();
            try {
                props.load(item.getInputStream());
                excludeListNode.setProperty("version", props.getProperty("version"));
                excludeListNode.setProperty("list", props.getProperty("list"));
                rootNode.save();
            } catch (IOException e) {
                %><script>window.alert("Unable to upload file");</script><%
            }
        }
    }
}


// load exclude list
String version = "";
String excludeList = "";
if (rootNode.hasNode("excludeList")) {
    Node excludeListNode = rootNode.getNode("excludeList");
    version = excludeListNode.getProperty("version").getString();
    excludeList = excludeListNode.getProperty("list").getString();

    // list is comma separated... make it better readable
    excludeList = excludeList.replace(',', '\n');
}

%>
<html>
    <head>
        <link rel="stylesheet" href="docroot/ui/default.css" type="text/css" title="style" />
    </head>
    <body style="margin-top:0px;border-width:0px">
        <form name="prefsform" action="preferences.jsp" method="get">
            <table width="100%">
                <tr><th class="content" colspan="2">Download ID</th></tr>
                <tr><td class="content">ID</td><td class="content"><input name="key" value="<%= rootNode.getNode("licNode").getProperty("key").getString() %>"></td></tr>
                <tr><td colspan="2"><input type="submit" value="Save" class="submit"></td></tr>
            </table>
        </form>

        <form name="excludelistform" action="preferences.jsp" method="post" enctype="multipart/form-data">
            <table width="100%">
                <tr><th class="content" colspan="2">ExcludeList</th></tr>
                <tr><td class="content">Exclude List File</td><td class="content"><input name="elfile" type="file"></td></tr>
                <tr></tr><td class="content">Version</td><td class="content"><%= version %></td></tr>
                <tr><td class="content">List</td><td class="content"><textarea name="excludeList" readonly><%= excludeList %></textarea></td></tr>
                <tr><td colspan="2"><input type="submit" value="Upload" class="submit"></td></tr>
            </table>
        </form>
    </body>
</html>
