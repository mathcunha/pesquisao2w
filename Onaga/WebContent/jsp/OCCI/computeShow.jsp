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

	<tr><td>id</td><td>${item.id} </td></tr>
	<tr><td>name</td><td>${item.name} </td></tr>
	<tr><td>state</td><td>${item.state} </td></tr>
	<tr><td>type</td><td>${item.instance_type} </td></tr>
	
	<c:forEach items="${item.storage.disks}" var="disk" varStatus="status">
		<tr>
			<td>${disk.href}</td> <td><a href="<c:url value="OCCIStorage?acao=show&id=${disk.idFromHref}"/>">Detalhar</a></td>
			<td><a href="<c:url value="OCCICompute?acao=create&id=${disk.idFromHref}"/>">Nova!</a></td>
		
		</tr>
	</c:forEach>
	
	<tr>
		<td colspan="1"><a href="<c:url value="OCCICompute?acao=delete&id=${item.id}"/>">delete</a></td>
	</tr>
</table>
</body>
</html>