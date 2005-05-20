<%@taglib uri="/taglib/jcr" prefix="jcr" %>
<%@taglib uri="/taglib/c" prefix="c" %>
<h1>Set</h1>

<jcr:session>
	<jcr:set item="/TestA" var="myA" scope="request"/>
	node: <jcr:out item="${myA}" /><br/>
	<jcr:set item="/TestA" property="jcr:primaryType" var="type" />
	Node type: <c:out value="${type.string}" />	
	
</jcr:session>