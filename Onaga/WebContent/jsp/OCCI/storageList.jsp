<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Insert title here</title>
</head>
<body>
<table>
<c:forEach items="${requestScope.storage.disks}" var="disk" varStatus="status">
	<tr><td>${disk.href}</td> <td><a href="<c:url value="OCCIStorage?acao=show&id=${disk.idFromHref}"/>">Detalhar</a></td></tr>
</c:forEach>
</table>
</body>
</html>