<% 
request.getSession().invalidate();
response.sendRedirect("index.jsp");
%>