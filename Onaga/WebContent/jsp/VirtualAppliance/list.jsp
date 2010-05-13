<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Virtual Appliances</title>
</head>
<body>
<pre>
${config}
</pre>
<table>
	<c:forEach items="${requestScope.list}" var="item" varStatus="status">
		<tr>
			<td>${item.name}</td>
			<td>${item.id}</td>
		</tr>
		<tr>
			<td colspan="1">
			<table>
				<c:forEach items="${item.virtualMachines}" var="item_inner"
					varStatus="status_inner">
					<tr>
						<td><a href="<c:url value="/VirtualApplianceServlet"/>?acao=show&id=${item_inner.id}">${item_inner.name}</a></td>
						<td>${item_inner.id}</td>
					</tr>
				</c:forEach>
			</table>
			</td>
		</tr>
	</c:forEach>
</table>
</body>
</html>