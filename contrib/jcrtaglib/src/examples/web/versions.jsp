<%@taglib uri="/taglib/jcr" prefix="jcr" %>
<%@taglib uri="/taglib/c" prefix="c" %>
<h1>Versions</h1>

<jcr:session>

<jcr:versions node="/TestA" var="node" traverserDepth="1">
	<c:out value="${node.path}"/><br>
</jcr:versions>

</jcr:session>

