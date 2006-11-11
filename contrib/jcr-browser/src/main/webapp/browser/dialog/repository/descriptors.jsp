<%@taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@taglib uri="http://jakarta.apache.org/taglib/string" prefix="str" %>
<%@taglib uri="http://jackrabbit.apache.org/jcr-taglib" prefix="jcr" %>
<jcr:session>
<div class="dialog">
<h3>Repository descriptors</h3>
<hr height="1"/>
<table class="dialog">
<c:forEach var="desc" items="${jcrsession.repository.descriptorKeys}">
<tr><td><c:out value="${desc}"/></td><td><%= 
((javax.jcr.Session) pageContext.getAttribute("jcrsession")).getRepository().getDescriptor(pageContext.getAttribute("desc").toString())
%></td></tr>
</c:forEach>
</table>
<input type="button" value="Close" onClick="hideDialog();"/>
</div>
</jcr:session>
