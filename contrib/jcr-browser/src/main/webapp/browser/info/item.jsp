<%@taglib uri="http://jackrabbit.apache.org/jcr-taglib" prefix="jcr" %>
<%@taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<% 
pageContext.setAttribute("path", request.getParameter("path")); 
pageContext.setAttribute("jcrsession",session.getAttribute("jcr.session"));
%>
<%@page import="javax.jcr.Node"%>
<jcr:set var="node" item="${path}"/>
<table width="100%" >
<tr>
<td width="50%" valign="top">
<table width="100%" class="itemInfo">
<tr>
	 <td width="33%" >New: <c:out value="${node.new}"/></td>
	 <td width="33%" >Modified: <c:out value="${node.modified}"/></td>
	 <td width="33%" >Depth: <c:out value="${node.depth}"/></td>
</tr>
<c:if test="${node.node}">
<% Node node = (Node) pageContext.getAttribute("node") ; %>
<tr>
	 <td width="33%" >Index: <c:out value="${node.index}"/></td>
	 <td width="33%" >Child nodes: <c:out value="${node.nodes.size}"/></td>
	 <td width="33%" >Child properties: <c:out value="${node.properties.size}"/></td>
</tr>
<% if (node.isNodeType("mix:referenceable")) {%> 
 <tr>
	 <td ><b>Referenceable</b>: <img src="<c:url value="/images/ok.gif"/>" height="12" width="12"></td>
 	 <td >References: <c:out value="${node.references.size}"/></td>
 	 <td ></td>
 </tr>
<% } else { %> 
  <tr>
	 <td colspan="3"><b>Referenceable</b>: 
		<img src="<c:url value="/images/x.gif"/>" height="12" width="12"> 
	 </td>
 </tr>
<% } %>
 <% if (node.isNodeType("mix:versionable")) {%> 
 <tr>
	 <td width="33%" ><b>Versionable</b>: <img src="<c:url value="/images/ok.gif"/>" height="12" width="12"></td>
	 <td width="33%" >Checked out: <c:out value="${node.checkedOut}"/> </td>
	 <td width="33%" >Base: 
<jcr:set item="${node.path}" property="jcr:isCheckedOut" var="prop"/>
<c:if test="${!prop.new}">
	  <c:out value="${node.baseVersion.name}"/>
</c:if>	 	  
	 </td>
 </tr>
 <tr>
	 <td colspan="3" ><b>Version labels</b>:<br/>
<c:if test="${!prop.new}">	 
<% pageContext.setAttribute("labels", node.getVersionHistory().getVersionLabels(node.getBaseVersion())) ;%>
<c:forEach var="label" items="${labels}">
	<c:out value="${label}"></c:out>
</c:forEach>
</c:if>
	 </td>
 </tr> 
<% } else { %> 
  <tr>
	 <td colspan="3"><b>Versionable</b>: 
		<img src="<c:url value="/images/x.gif"/>" height="12" width="12"> 
	 </td>
 </tr>
<% } %>
<% if (node.isNodeType("mix:lockable")) {%> 
  <tr>
	 <td width="33%" ><b>Lockable</b>: <img src="<c:url value="/images/ok.gif"/>" height="12" width="12"> </td>
	 <td width="33%" >Locked: <c:out value="${node.locked}"/></td>
	 <td width="33%" >Holds Lock: <%= node.holdsLock() %></td>	 
 </tr>
<c:if test="${node.locked}">
  <tr>
	 <td colspan="3">Locked Node: <c:out value="${node.lock.node.path}"/></td>
 </tr>
  <tr>
	 <td width="67%" colspan="2">Token: <c:out value="${node.lock.lockToken}"/></td>
	 <td width="33%" >Is Deep: <c:out value="${node.lock.deep}"/></td>
 </tr>
  <tr>
	 <td width="33%" >Owner: <c:out value="${node.lock.lockOwner}"/></td>
	 <td width="33%" >Session Scoped: <c:out value="${node.lock.sessionScoped}"/></td>
	 <td width="33%" >Live: <c:out value="${node.lock.live}"/></td>
 </tr>
</c:if> 
<% } else { %> 
  <tr>
	 <td colspan="3"><b>Lockable</b>: 
		<img src="<c:url value="/images/x.gif"/>" height="12" width="12"> 
	 </td>
 </tr>
<% } %>
</c:if>
 </table>
</td>
<td width="50%" valign="top">
	<div id="roundMePreview">
		TODO: Preview 
	</div>			
</td>
</tr>
</table>
<script language="JavaScript" type="text/javascript">
    dojo.addOnLoad(function(){
    
		dojo.lfx.rounded({
				tl:{ radius:15 },
				tr:{ radius:10 },
				br:{ radius:15 }
			}, ["roundMePreview"]
		);	        
		
    });	
</script>