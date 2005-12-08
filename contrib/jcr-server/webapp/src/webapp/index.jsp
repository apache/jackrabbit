<%@ page import="org.apache.jackrabbit.j2ee.SimpleWebdavServlet,
		 javax.jcr.Repository,
		 org.apache.jackrabbit.j2ee.RepositoryAccessServlet,
                 org.apache.jackrabbit.j2ee.JCRWebdavServerServlet"%><%
%><html>
<head>
<title>Jackrabbit JCR-Server Contribution</title>
</head>
<body style="font-family:monospace">
<h1>JCR-Server Contribution</h1>
<h3>JCR-Server contribution provides 2 views</h3><p/>
<ol>
    <li>
        <b>Filebased View</b> (SimpleWebdavServlet)<p/>
        Filebased ("Simple") WebDAV View to the JSR170 repository.<p/>
        Enter the following URL to your WebDAV client:<br>
        http://&lt;<i>host</i>&gt;:&lt;<i>port</i>&gt;<%= request.getContextPath() %><%= SimpleWebdavServlet.getPathPrefix(pageContext.getServletContext()) %>/&lt;<i>workspace name</i>&gt;/
        <p/>
        <ul>
            <li><a href="<%= request.getContextPath() %><%= SimpleWebdavServlet.getPathPrefix(pageContext.getServletContext()) %>/default/">Browser View</a></li>
            <li>Context Path: <%= request.getContextPath() %></li>
            <li>Resource Path Prefix: <%= SimpleWebdavServlet.getPathPrefix(pageContext.getServletContext()) %></li>
            <li>Workspace Name: see /WEB-INF/repository/repository.xml (Default = 'default')</li>
            <li>Source: /contrib/jcr-server/server/webdav/simple</li>
        </ul>
        <p/>
    </li>
    <li>
        <b>Item View</b> (JCRServerServlet)<p/>
        Itembased WebDAV View to the JSR170 repository, mapping the functionality
        provided by JSR170 to WebDAV, in order to allow remoting of JSR170 via
        WebDAV. Some more details regarding remoting are available as initial
        draft "<a href="http://www.day.com/jsr170/server/JCR_Webdav_Protocol.zip">JCR_Webdav_Protocol.zip</a>".
        In addition the implementation attempts to cover functionality of RFC 2518 and
        its extensions wherever possible, namely<br>
        <ul>
            <li><a href="http://www.ietf.org/rfc/rfc2518.txt">RFC 2518 (WebDAV 1,2)</a></li>
            <li><a href="http://www.ietf.org/rfc/rfc3253.txt">RFC 3253 (DeltaV)</a></li>
            <li><a href="http://www.ietf.org/rfc/rfc3648.txt">RFC 3648 (Ordering)</a></li>
            <li><a href="http://greenbytes.de/tech/webdav/draft-reschke-webdav-search-latest.html">Internet Draft WebDAV Search</a>.</li>
        </ul>
        <p/>
        Enter the following URL to your WebDAV client:<br>
        http://&lt;<i>host</i>&gt;:&lt;<i>port</i>&gt;<%= request.getContextPath() %><%= JCRWebdavServerServlet.getPathPrefix(pageContext.getServletContext()) %>/
        <p/>
        <ul>
            <li>Browser View: - Not Available - ("<%= request.getContextPath() %><%= JCRWebdavServerServlet.getPathPrefix(pageContext.getServletContext()) %>/")</li>
            <li>Context Path: <%= request.getContextPath() %></li>
            <li>Resource Path Prefix: <%= JCRWebdavServerServlet.getPathPrefix(pageContext.getServletContext()) %></li>
            <li>Workspace Name: - Not required - (available workspaces are mapped as resources)</li>
            <li>Source: /contrib/jcr-server/server/webdav/jcr</li>
        </ul>
    </li>
</ol>
<p/><p/>
<h3>Basic overview</h3><p/>
<ol>
    <li><b>webdav module</b> (library only)<p/>
      <ul>
          <li>aim: JSR170 independent WebDAV library</li>
          <li>packages: org.apache.jackrabbit.webdav</li>
      </ul>
      <p/>
    </li>
    <li><b>server module</b><p/>
      <ul>
          <li>aim: server and server-side WebDAV implementation</li>
          <li>packages:
              <br>- org.apache.jackrabbit.server
              <br>- org.apache.jackrabbit.server.jcr = jcr-server specific server part
              <br>- org.apache.jackrabbit.server.io  = import/export
              <br>- org.apache.jackrabbit.webdav.simple = simple (filebased) webdav implementation
              <br>- org.apache.jackrabbit.webdav.jcr = jcr-server (itembased) webdav implementation
          </li>
      </ul>
      <p/>
    </li>
    <li><b>webapp module</b> (jackrabbit-server)<p/>
      <ul>
          <li>aim: contains the webapp</li>
          <li>packages: org.apache.jackrabbit.j2ee (servlets)</li>
      </ul>
      <p/>
    </li>
    <li><b>client module</b><p/>
      <ul>
          <li>aim: provide jcr-client and WebDAV transport layer for the client</li>
          <li>packages: - Not Available -</li>
      </ul>
    </li>
</ol>
<%
    Repository rep = RepositoryAccessServlet.getRepository(pageContext.getServletContext());

%><hr size="1"><em>Powered by <a href="<%= rep.getDescriptor(Repository.REP_VENDOR_URL_DESC) %>"><%= rep.getDescriptor(Repository.REP_NAME_DESC)%></a> version <%= rep.getDescriptor(Repository.REP_VERSION_DESC) %>.</em>
</body>
</html>