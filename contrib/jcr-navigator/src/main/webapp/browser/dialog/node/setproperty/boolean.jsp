<%@taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@taglib uri="http://jakarta.apache.org/taglib/string" prefix="str" %>
<%@taglib uri="http://jackrabbit.apache.org/jcr-taglib" prefix="jcr" %>
<c:set var="type" scope="request">boolean</c:set>
<c:set var="editor" scope="request">
<input type="hidden" name="type" value="Boolean"/>
<input type="radio" name="value" value="true" />true
<input type="radio" name="value" value="false" checked="checked"/>false
</c:set>
<jsp:include flush="true" page="setproperty.jsp"></jsp:include>