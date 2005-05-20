<%@taglib uri="/taglib/jcr" prefix="jcr" %>
<%@taglib uri="/taglib/c" prefix="c" %>
<h1>Nodes Tag with default traverser</h1>
<font color="red">Sorted by "item.name"</font>

<jcr:session>

<h2>Descending</h2>
<table border="1" cellpadding="5">
<tr>
	<TH>Path</TH>
	<TH>Name</TH>	
</tr>
<jcr:nodes node="/" var="node" traverserDepth="2" sortExp="item.name" ascending="false">
<tr>
	<TD><c:out value="${node.path}"/></TD>
	<TD><c:out value="${node.name}"/></TD>	
</tr>
</jcr:nodes>
</table>

<h2>Ascending</h2>
<table border="1" cellpadding="5">
<tr>
	<TH>Path</TH>
	<TH>Name</TH>	
</tr>
<jcr:nodes node="/" var="node" traverserDepth="2" sortExp="item.name" ascending="true">
<tr>
	<TD><c:out value="${node.path}"/></TD>
	<TD><c:out value="${node.name}"/></TD>	
</tr>
</jcr:nodes>
</table>


</jcr:session>