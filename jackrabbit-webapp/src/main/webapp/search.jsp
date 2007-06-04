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
                   org.apache.jackrabbit.j2ee.RepositoryAccessServlet,
                   javax.jcr.Session,
                   javax.jcr.SimpleCredentials,
                   java.util.Calendar,
                   javax.jcr.query.QueryManager,
                   javax.jcr.query.Query,
                   javax.jcr.query.QueryResult,
                   javax.jcr.query.RowIterator,
                   java.text.NumberFormat,
                   javax.jcr.query.Row,
                   javax.jcr.Node,
                   java.net.URLEncoder,
                   java.text.SimpleDateFormat,
                   java.text.DateFormat,
                   java.util.List,
                   java.util.ArrayList,
                   java.util.Iterator"%>
<%@ page contentType="text/html;charset=UTF-8" %><%
    Repository rep;
    Session jcrSession;
    try {
        rep = RepositoryAccessServlet.getRepository(pageContext.getServletContext());
        jcrSession = rep.login(new SimpleCredentials("anonymous", "".toCharArray()));
    } catch (Throwable e) {
        %>Error while accessing the repository: <font color="red"><%= e.getMessage() %></font><br><%
        %>Check the configuration or use the <a href="admin/">easy setup</a> wizard.<%
        return;
    }
    try {
        String wspName = jcrSession.getWorkspace().getName();
        String q = request.getParameter("q");
        if (q == null) {
            q = "";
        }
        if (request.getParameter("as_q") != null) {
            q += " " + request.getParameter("as_q");
        }
        String executedIn = "";
        String queryTerms = "";
        String totalResults = "";
        long from = 0;
        long to = 10;
        long total = 0;
        long maxPage = 0;
        long minPage = 0;
        long currentPageIndex = 0;
        List indexes = new ArrayList();
        RowIterator rows = null;
        if (q != null && q.length() > 0) {
            String stmt;
            if (q.startsWith("related:")) {
                String path = q.substring("related:".length());
                stmt = "//element(*, nt:file)[rep:similar(jcr:content, '" + path + "/jcr:content')]/rep:excerpt(.)";
                queryTerms = "similar to <b>" + path + "</b>";
            } else {
                queryTerms = "for <b>" + q + "</b>";
                q = q.replaceAll("'", "''");
                stmt = "//element(*, nt:file)[jcr:contains(jcr:content, '" + q + "')]/rep:excerpt(.)";
            }
            Query query = jcrSession.getWorkspace().getQueryManager().createQuery(stmt, Query.XPATH);
            long time = System.currentTimeMillis();
            rows = query.execute().getRows();
            time = System.currentTimeMillis() - time;
            NumberFormat nf = NumberFormat.getNumberInstance();
            nf.setMaximumFractionDigits(2);
            nf.setMinimumFractionDigits(2);
            executedIn = nf.format(((double) time) / 1000d);
            nf.setMaximumFractionDigits(0);
            totalResults = nf.format(rows.getSize());
            if (request.getParameter("start") != null) {
                from = Long.parseLong(request.getParameter("start"));
                try {
                    rows.skip(from);
                } catch (Exception e) {
                    // make sure rows are consumed
                    while (rows.hasNext()) {
                        rows.nextRow();
                    }
                }
            }
            to = Math.min(from + 10, rows.getSize());

            total = rows.getSize();
            maxPage = total / 10L;
            if (total % 10L > 0) {
                maxPage++;
            }
            currentPageIndex = from / 10L;
            maxPage = Math.min(maxPage, currentPageIndex + 10);
            minPage = Math.max(0, currentPageIndex - 10);
            for (long i = minPage; i < maxPage; i++) {
                indexes.add(new Long(i));
            }
        }
%><html>
  <head>
    <title>Jackrabbit Search</title>
    <link rel="shortcut icon" href="<%= request.getContextPath() %>/images/favicon.ico" type="image/vnd.microsoft.icon">
    <style><!--
div,td{color:#000}
.f{color:#666}
.flc,.fl:link{color:#77c}
a:link,.w,a.w:link,.w a:link,.q:visited,.q:link,.q:active,.q{color:#00c}
a:visited,.fl:visited{color:#551a8b}
a:active,.fl:active{color:red}
.t{background:#e5ecf9;color:#000}
.bb{border-bottom:1px solid #36c}
.bt{border-top:1px solid #36c}
.j{width:34em}
.h{color:#36c}
.i,.i:link{color:#a90a08}
.a,.a:link{color:green}
.z{display:none}
div.n{margin-top:1ex}
.n a{font-size:10pt;color:#000}
.n .i{font-size:10pt;font-weight:bold}
.b a{font-size:12pt;color:#00c;font-weight:bold}
#np,#nn,.nr,#logo span,.ch{cursor:pointer;cursor:hand}
.tas{padding:3px 3px 3px 5px}
.taf{padding:3px 3px 6px 5px}
.tam{padding:6px 3px 6px 5px}
.tal{padding:6px 3px 3px 5px}
.sl,.r{font-weight:normal;margin:0;display:inline}
.sl{font-size:84%}
.r{font-size:1em}
.e{margin:.75em 0}
.mblink:visited{color:#00c}
.sm{display:block;margin:0;margin-left:40px}
.bl{display:none}
.fl2,.fl2:link,.fl2:visited{color:#77c}
.fl2:active{color:red}
#navbar div,#logo span{background:url(<%= request.getContextPath() %>/images/jackrabbitlogo.gif) no-repeat;overflow:hidden;height:83px}
#navbar .nr{background-position:-235px 0;width:22px}
#navbar #np{background-position:-95px;width:140px}
#navbar #nf{background-position:-95px 0;width:140px}
#navbar #nc{background-position:-235px 0;width:22px}
#navbar #nn{background-position:-279px 0;width:66px;margin-right:34px}
#navbar #nl{background-position:-279px 0;width:46px}
#logo{display:block;width:320px;height:83px;position:relative;overflow:hidden}
#logo span{background-position:0 0px;position:absolute;top:0;left:0;width:100%;height:100%}
body,td,div,.p,a{font-family:arial,sans-serif}
.g{margin:1em 0}
#sd{font-size:113%;font-weight:bold}
#ap{font-size:64%}
--></style></head>
  <bodybgcolor="#ffffff" topmargin="3" marginheight="3">
  <table border=0 cellpadding=0 cellspacing=0 width=100% style=clear:left>
    <tr><form name=gs method=GET>
      <td valign=top><a id=logo href="http://jackrabbit.apache.org/" title="Go to Jackrabbit Home">Jackrabbit<span></span></a></td>
      <td>&nbsp;&nbsp;</td>
      <td valign=top width=100% style="padding-top:8px">
        <table cellpadding=0 cellspacing=0 border=0>
          <tr>
            <td height=14 valign=bottom>
              <img align=right alt="" height=1 width=1><style>#lgpd{display:none}</style>
              <table border=0 cellspacing=0 cellpadding=4>
                <tr>
                  <td nowrap><font size=-1><b>Workspace: <%= wspName %></b></font></td>
                </tr>
              </table>
            </td>
          </tr>
          <tr>
            <td>
              <table border=0 cellpadding=0 cellspacing=0>
                <tr>
                  <td nowrap>
                    <input type=text name=q size=41 maxlength=2048 value="<%= q %>" title="Search"><font size=-1> <input type=submit value="Search"><span id=hf></span></font>
                  </td>
                </tr>
              </table>
            </td>
          </tr>
        </table>
      </td></form>
    </tr>
  </table>

  <%
    if (rows != null && rows.getSize() == 0) {
  %>
  <table border=0 cellpadding=0 cellspacing=0 width=100% class="t bt">
    <tr><td nowrap><span id=sd>&nbsp;Workspace: <%= wspName %>&nbsp;</span></td></tr>
  </table>
  <p/>Your search - <b><%= q %></b> - did not match any documents.
  <br/><br/>Suggestions:
  <ul><li>Make sure all words are spelled correctly.</li><li>Try different keywords.</li><li>Try more general keywords.</li><li>Try fewer keywords.</li></ul>
  <%
    } else if (rows != null) {
  %>
  <table border=0 cellpadding=0 cellspacing=0 width=100% class="t bt">
    <tr><td nowrap><span id=sd>&nbsp;Workspace: <%= wspName %>&nbsp;</span></td><td align=right nowrap><font size=-1>Results <b><%= from + 1 %></b> - <b><%= to %></b> of about <b><%= totalResults %></b> <%= queryTerms %>. (<b><%= executedIn %></b> seconds)&nbsp;</font></td></tr>
  </table>

  <div id=res>
    <%
      while (rows.hasNext() && rows.getPosition() < to) {
          Row r = rows.nextRow();
          Node file = (Node) jcrSession.getItem(r.getValue("jcr:path").getString());
          Node resource = file.getNode("jcr:content");
          String size = "";
          if (resource.hasProperty("jcr:data")) {
              double length = resource.getProperty("jcr:data").getLength();
              size = String.valueOf(Math.round(Math.ceil(length / 1000d))) + "k";
          }
          DateFormat df = SimpleDateFormat.getDateInstance(SimpleDateFormat.LONG);
          String lastModified = df.format(resource.getProperty("jcr:lastModified").getDate().getTime());
    %>
    <div class=g><h2 class=r><a href="<%= request.getContextPath() %>/repository/<%= wspName %><%= file.getPath() %>" class=l><%= file.getName() %></a></h2>
      <table border=0 cellpadding=0 cellspacing=0>
        <tr><td class="j"><font size=-1><%= r.getValue("rep:excerpt(jcr:content)").getString() %>
          <span class=a><%= file.getPath() %> - <%= size %> - <%= lastModified %> - </span><nobr><a class=fl href="<%= request.getContextPath() %>/search.jsp?q=related:<%= URLEncoder.encode(file.getPath(), "UTF-8") %>">Similar pages</a></nobr></font><!--n--></td>
        </tr>
      </table>
    </div>
    <%
      } // while
    %>
  </div>

  <br clear=all>
  <%
    if (indexes.size() > 1) {
  %>
  <div id=navbar class=n>
    <table border=0 cellpadding=0 width=1% cellspacing=0 align=center>
      <tr align=center style=text-align:center valign=top>
        <td valign=bottom nowrap><font size=-1>Result&nbsp;Page:&nbsp;</font>
        <%
        if (currentPageIndex != ((Long) indexes.get(0)).longValue()) {
            %><td nowrap align=right class=b><a href=search.jsp?q=<%= q %>&start=<%= (currentPageIndex - 1) * 10 %>><div id=np></div>Previous</a><%
        } else {
            %><td nowrap ><div id=nf></div><%
        }
        for (Iterator it = indexes.iterator(); it.hasNext(); ) {
            long pageIdx = ((Long) it.next()).longValue();
            if (pageIdx == currentPageIndex) {
                %><td nowrap><div id=nc></div><span class=i><%= pageIdx + 1 %></span><%
            } else {
                %><td nowrap><a href=search.jsp?q=<%= q %>&start=<%= pageIdx * 10 %>><div class=nr></div><%= pageIdx + 1 %></a><%
            }
        }
        if (currentPageIndex < (maxPage - 1)) {
            %><td nowrap class=b><a href=search.jsp?q=<%= q %>&start=<%= (currentPageIndex + 1) * 10 %>><div id=nn></div>Next</a><%
        } else {
            %><td nowrap ><div id=nl></div><%
        }
        %>
    </table>
  </div>
  <%
    }
  %>

  <center>
    <br clear=all><br>
    <table border=0 cellpadding=0 cellspacing=0 width=100% class="ft t bb bt">
      <tr><td align=center>&nbsp;<br>
        <table border=0 cellpadding=0 cellspacing=0 align=center><form method=GET action=<%= request.getContextPath() %>/search.jsp><tr><td nowrap>
          <font size=-1><input type=text name=q size=31 maxlength=2048 value="<%= q %>" title="Search"> <input type=submit value="Search">
          </font></td></tr></form>
        </table>
        <br><font size=-1>
        <a href="<%= request.getContextPath() %>/swr.jsp?q=<%= q %>&swrnum=<%= rows.getSize() %>">Search&nbsp;within&nbsp;results</a> | <a href="http://issues.apache.org/jira/browse/JCR" target=_blank>Dissatisfied? Help us improve</a></font><br>
        <br>
      </td></tr>
    </table>
  </center>

  <%
    } // if (rows != null)

    String tableClass = "";
    if (rows != null && rows.getSize() == 0) {
        tableClass = " class=\"t n bt\"";
    }
  %>

  <center>
    <p><hr class=z>
    <table border=0 cellpadding=2 cellspacing=0 width=100%<%= tableClass %>>
      <tr><td align=center><font size=-1><a href="http://jackrabbit.apache.org/">Jackrabbit&nbsp;Home</a></font></td></tr>
    </table>
    <br><font size=-1 class=p>&copy;<%= Calendar.getInstance().get(Calendar.YEAR) %> Apache Jackrabbit</font>
  </center>
  </body>
</html><%
    } finally {
        if (jcrSession != null) {
            jcrSession.logout();
        }
    }
%>