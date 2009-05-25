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

<form action="<c:url value="/FilmeServlet"/>">

Nome: <input type="text" name="nome" value="<c:out value="${requestScope.bean.nome}"/>">
Quantidade: <input type="text" name="quantidade" value="<c:out value="${requestScope.bean.quantidade}"/>">
<input type="hidden" name="identificador" value="<c:out value="${requestScope.bean.identificador}"/>"/>

<input type="submit""/> 

</form>

</body>
</html>