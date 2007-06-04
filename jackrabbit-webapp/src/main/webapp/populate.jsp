<%
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
%><%@ page import="javax.jcr.Repository,
                 javax.jcr.Session,
                 org.apache.jackrabbit.j2ee.RepositoryAccessServlet,
                 javax.jcr.SimpleCredentials,
                   java.util.Iterator,
                   java.net.URL,
                   java.net.URLEncoder,
                   java.net.MalformedURLException,
                   java.io.UnsupportedEncodingException,
                   java.io.InputStream,
                   javax.swing.text.html.HTML,
                   javax.swing.text.html.HTMLEditorKit,
                   javax.swing.text.html.HTMLDocument,
                   java.util.ArrayList,
                   java.util.List,
                   javax.swing.text.AttributeSet,
                   javax.jcr.Node,
                   java.util.Arrays,
                   java.util.Collections,
                   java.util.Map,
                   java.util.HashMap,
                   java.io.BufferedInputStream,
                   java.util.Calendar,
                   java.net.URLConnection,
                   java.io.IOException,
                   javax.jcr.RepositoryException,
                   java.io.InputStreamReader,
                   java.net.URLDecoder,
                   java.io.FilterInputStream"
 %><%@ page contentType="text/html;charset=UTF-8" %><%
    Repository rep;
    Session jcrSession;
    String wspName;
    try {
        rep = RepositoryAccessServlet.getRepository(pageContext.getServletContext());
        jcrSession = rep.login(new SimpleCredentials("user", "".toCharArray()));
        wspName = jcrSession.getWorkspace().getName();
    } catch (Throwable e) {
        %>Error while accessing the repository: <font color="red"><%= e.getMessage() %></font><br><%
        %>Check the configuration or use the <a href="admin/">easy setup</a> wizard.<%
        return;
    }
    try {
        String seedWord = request.getParameter("seed");
        int numDocs = 0;
        List filetypes = new ArrayList();
        if (request.getParameter("num") != null) {
            try {
                numDocs = Integer.parseInt(request.getParameter("num"));
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        String[] types = request.getParameterValues("filetype");
        if (types != null) {
            for (int i = 0; i < types.length; i++) {
                filetypes.add(types[i]);
            }
        }
%><html>
  <head><title>Populate workspace: <%= wspName %></title><script><!--

function draw() {
	// draw the bar
	document.write('<table cellspacing="0" cellpadding="0" style="border-color:' + this.borderColor + '; border-width:' + this.borderWidth + '; border-style:' + this.borderStyle + '">');
	document.write('<tr><td>');
	document.write('<table border="0" cellspacing="0" cellpadding="0" style="">');
	document.write('<tr><td style="background-color:' + this.barColor +'"><img src="<%= request.getContextPath() %>/images/0.gif" id="' + this.id + 'barFG" width="0" height="' + this.height + '"/></td>');
	document.write('<td><img src="<%= request.getContextPath() %>/images/0.gif" id="' + this.id + 'barBG" width="' + this.width + '" height="' + this.height + '"/></td></tr>');
	document.write('</table>');
	document.write('</tr></td>');
	document.write('</table>');
	document.write('<table>');
	document.write('<tr><td><img src="<%= request.getContextPath() %>/images/0.gif" width="' + this.width + '" height="0"/></td></tr>');
	document.write('<tr><td align="center"><div id="' + this.id + 'barValue">0%</div></td></tr>');
	document.write('<tr><td align="center"><div id="' + this.id + 'barInfo">&nbsp;</div></td></tr>');
	document.write('</table>');

	this.barFG = document.getElementById(this.id + 'barFG');
	this.barBG = document.getElementById(this.id + 'barBG');
	this.barValue = document.getElementById(this.id + 'barValue').firstChild;
	this.barInfo = document.getElementById(this.id + 'barInfo').firstChild;
}

// informs the progress bar about the current value
function inform(value, info) {
	var barWidth = Math.floor(this.width * value / this.maxValue);
	var spaceWidth = this.width - barWidth;
	var perCent = Math.floor(100 * value / this.maxValue);
	this.barFG.width = barWidth;
	this.barBG.width = spaceWidth;
	this.barValue.nodeValue = perCent + '%';
	this.barInfo.nodeValue = info;
}

// constructor
function ProgressBar(maxValue, width, height) {
	this.maxValue = maxValue;
	this.width = width;
	this.height = height;
	this.id = '' + Math.round(Math.random() * 10000);
	this.inform = inform;
	this.draw = draw;
}

// default values
ProgressBar.prototype.barColor = "green";
ProgressBar.prototype.borderColor = "grey";
ProgressBar.prototype.borderStyle = "groove";
ProgressBar.prototype.borderWidth = "2px";

// -->
</script></head>
  <body>
    <h2>Populate workspace: <%= wspName %></h2>
    <%
        if (seedWord != null && numDocs > 0 && filetypes.size() > 0) {
    %>
    Overall progress:
    <p/>
    <script>var pb = new ProgressBar(<%= numDocs %>, 300, 30);pb.draw();</script>
    Downloading document:
    <p/>
    <script>var dp = new ProgressBar(1000, 300, 30);dp.draw();</script>
    <%
            Node root = jcrSession.getRootNode();
            int n = 0;
            for (int typeIdx = 0; typeIdx < filetypes.size(); typeIdx++) {
                String type = (String) filetypes.get(typeIdx);
                int offset = 0;
                while (n < numDocs * (typeIdx + 1) / filetypes.size()) {
                    final URL[] urls = new Search(type, seedWord, offset).getURLs(out);
                    if (urls.length == 0) {
                        break;
                    }
                    for (int i = 0; i < urls.length; i++) {
                        final URL currentURL = urls[i];
                        String path = urls[i].getPath();
                        if (path.startsWith("/")) {
                            path = path.substring(1);
                        }
                        final String host = urls[i].getHost();
                        List folderNames = new ArrayList();
                        folderNames.addAll(Arrays.asList(host.split("\\.")));
                        Collections.reverse(folderNames);
                        folderNames.addAll(Arrays.asList(path.split("/", 0)));
                        final String fileName = URLDecoder.decode((String) folderNames.remove(folderNames.size() - 1), "UTF-8").replaceAll(":", "_");
                        Node node = root;
                        for (Iterator fn = folderNames.iterator(); fn.hasNext(); ) {
                            String name = URLDecoder.decode((String) fn.next(), "UTF-8");
                            name = name.replaceAll(":", "_");
                            if (!node.hasNode(name)) {
                                node.addNode(name, "nt:folder");
                            }
                            node = node.getNode(name);
                        }
                        if (!node.hasNode(fileName)) {
                            final JspWriter fOut = out;
                            Node file = node.addNode(fileName, "nt:file");
                            final Node resource = file.addNode("jcr:content", "nt:resource");
                            final Exception[] ex = new Exception[1];
                            Thread t = new Thread(new Runnable() {
                                public void run() {
                                    try {
                                        String info = fileName + " (" + host + ")";
                                        URLConnection con = currentURL.openConnection();
                                        con.setReadTimeout(10000); // 10 seconds
                                        InputStream in = con.getInputStream();
                                        try {
                                            synchronized (fOut) {
                                                fOut.println("<script>dp.inform(0, '" + info + "')</script>");
                                                fOut.flush();
                                            }
                                            int length = con.getContentLength();
                                            if (length != -1) {
                                                in = new ProgressInputStream(in, length, info, "dp", fOut);
                                            }
                                            resource.setProperty("jcr:data", in);
                                            String mimeType = URLConnection.guessContentTypeFromName(fileName);
                                            if (mimeType == null) {
                                                if (fileName.endsWith(".doc")) {
                                                    mimeType = "application/msword";
                                                } else if (fileName.endsWith(".xls")) {
                                                    mimeType = "application/vnd.ms-excel";
                                                } else if (fileName.endsWith(".ppt")) {
                                                    mimeType = "application/mspowerpoint";
                                                } else {
                                                    mimeType = "application/octet-stream";
                                                }
                                            }
                                            resource.setProperty("jcr:mimeType", mimeType);
                                            Calendar lastModified = Calendar.getInstance();
                                            lastModified.setTimeInMillis(con.getLastModified());
                                            resource.setProperty("jcr:lastModified", lastModified);
                                        } finally {
                                            in.close();
                                        }
                                    } catch (Exception e) {
                                        ex[0] = e;
                                    }
                                }
                            });
                            t.start();
                            for (int s = 0; t.isAlive(); s++) {
                                Thread.sleep(100);
                                if (s % 10 == 0) {
                                    synchronized (fOut) {
                                        fOut.println("<script>pb.inform(" + n + ", '')</script>");
                                        fOut.flush();
                                    }
                                }
                            }
                            if (ex[0] == null) {
                                jcrSession.save();
                                n++;
                                synchronized (fOut) {
                                    fOut.println("<script>pb.inform(" + n + ", '')</script>");
                                    fOut.flush();
                                }
                                if (n >= numDocs * (typeIdx + 1) / filetypes.size()) {
                                    break;
                                }
                            } else {
                                jcrSession.refresh(false);
                            }
                        }
                    }
                    offset += 10;
                }
            }
        } else {
    %>
    <p>This page allows you to populate the workspace with documents downloaded
from the Internet.</p>
    <%
        }
    %>
    <p/>
    <form>
      <table border=0 cellpadding=5 cellspacing=0 width=100%>
      <tr><td>Seed word:</td><td><input name="seed" type="text" size="30" value="<%= seedWord == null ? "download" : seedWord %>"/></td></tr>
      <tr><td>Number of documents:</td><td><input name="num" type="text" size="30" value="<%= numDocs == 0 ? 100 : numDocs %>"/></td></tr>
      <tr valign="top"><td>Document types:</td><td><input name="filetype" type="checkbox" value="pdf" <%= filetypes.contains("pdf") ? "checked" : "" %>/> Adobe Acrobat PDF<br/><input name="filetype" type="checkbox" value="rtf" <%= filetypes.contains("rtf") ? "checked" : "" %>/> Rich Text Format<br/><input name="filetype" type="checkbox" value="doc" <%= filetypes.contains("doc") ? "checked" : "" %>/> Microsoft Word<br/><input name="filetype" type="checkbox" value="ppt" <%= filetypes.contains("ppt") ? "checked" : "" %>/> Microsoft PowerPoint<br/><input name="filetype" type="checkbox" value="xls" <%= filetypes.contains("xls") ? "checked" : "" %>/> Microsoft Excel<br/></td></tr>
      <tr><td>&nbsp;</td><td><input type="submit" value="Populate!"/></td></tr>
    </form>
  </body>
</html><%
    } finally {
        if (jcrSession != null) {
            jcrSession.logout();
        }
    }
%><%!
    public Iterator getDocuments(String mimeType, String searchTerm) {
        return new Iterator() {
            public boolean hasNext() {
                return false;
            }

            public Object next() {
                return null;
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static class Search {

        private final String filetype;

        private final String term;

        private final int start;

        public Search(String filetype, String term, int start) {
            this.filetype = filetype;
            this.term = term;
            this.start = start;
        }

        public URL[] getURLs(JspWriter out) throws Exception {
            try {
                List urls = new ArrayList();
                String query = term + " filetype:" + filetype;
                URL google = new URL("http://www.google.com/search?q=" +
                        URLEncoder.encode(query, "UTF-8") + "&start=" + start);
                URLConnection con = google.openConnection();
                con.setRequestProperty("User-Agent", "");
                InputStream in = con.getInputStream();
                try {
                    HTMLEditorKit kit = new HTMLEditorKit();
                    HTMLDocument doc = new HTMLDocument();
                    doc.putProperty("IgnoreCharsetDirective", Boolean.TRUE);
                    kit.read(new InputStreamReader(in, "UTF-8"), doc, 0);
                    HTMLDocument.Iterator it = doc.getIterator(HTML.Tag.A);
                    while (it.isValid()) {
                        AttributeSet attr = it.getAttributes();
                        if (attr != null) {
                            String href = (String) attr.getAttribute(HTML.Attribute.HREF);
                            if (href != null && href.endsWith("." + filetype)) {
                                URL url = new URL(new URL("http", "www.google.com", "dummy"), href);
                                if (url.getHost().indexOf("google") == -1) {
                                    urls.add(url);
                                }
                            }
                        }
                        it.next();
                    }
                } finally {
                    in.close();
                }
                return (URL[]) urls.toArray(new URL[urls.size()]);
            } catch (Exception e) {
                throw e;
            }
        }
    }

    public static class ProgressInputStream extends FilterInputStream {

        private final int length;

        private final String fileName;

        private final String varName;

        private final JspWriter out;

        private long read;

        private long nextReport = (16 * 1024);

        public ProgressInputStream(InputStream in, int length, String fileName,
                                   String varName, JspWriter out) {
            super(in);
            this.length = length;
            this.fileName = fileName;
            this.varName = varName;
            this.out = out;
        }

        public int read() throws IOException {
            int r = super.read();
            reportProgress(r);
            return r;
        }

        public int read(byte b[]) throws IOException {
            int r = super.read(b);
            reportProgress(r);
            return r;
        }

        public int read(byte b[], int off, int len) throws IOException {
            int r = super.read(b, off, len);
            reportProgress(r);
            return r;
        }

        private void reportProgress(int r) throws IOException {
            if (r != -1) {
                read += r;
                if (read > nextReport || read == length * 2) {
                    // report every 16k
                    synchronized (out) {
                        double s = 500d * (double) read / (double) length;
                        out.println("<script>" + varName + ".inform(" +
                                Math.min((int) Math.ceil(s), 1000) +
                                ", '" + fileName + "')</script>");
                        out.flush();
                    }
                    nextReport += (16 * 1024);
                }
            }
        }
    }
%>