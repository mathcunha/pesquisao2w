<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Insert title here</title>
</head>
<body>
<a href="<c:url value="/filme/editar.jsp"/>">Novo</a>

<table>
	<tr>
		<td>Nome</td>
		<td>Quantidade</td>
		<td>Ação</td>
	</tr>
	<c:forEach varStatus="status" var="item" items="${requestScope.itens}">
	<tr>
		<td>
			<c:out value="${item.nome}"/>
		</td>
		<td>
			<c:out value="${item.quantidade}"/>
		</td>
		<td>
			
			<a href="<c:url value="/FilmeServlet"/>?acao=excluir&identificador=<c:out value="${ item.identificador}"/>">Excluir</a>
			<a href="<c:url value="/FilmeServlet"/>?acao=exibir&identificador=<c:out value="${ item.identificador}"/>">Editar</a>
			
		</td>
	</tr>	
	</c:forEach>
</table>

</body>
</html>