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

	<tr><td>id</td><td>${disk.id} </td></tr>
	<tr><td>name</td><td>${disk.name} </td></tr>
	<tr><td>url</td><td>${disk.url} </td></tr>
	<tr><td>size</td><td>${disk.size} </td></tr>
	<tr><td colspan="1"><a href="<c:url value="OCCICompute?acao=create&id=${disk.id}"/>">Nova!</a></td></tr>
</table>
</body>
</html>