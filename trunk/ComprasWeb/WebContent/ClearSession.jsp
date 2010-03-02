<%--
 Copyright 2006 Sun Microsystems, Inc. All rights reserved.
 Use is subject to license terms.
--%>

<HTML>
<HEAD><TITLE>Cluster - Ha JSP Sample </TITLE></HEAD>
<BODY>

<% String action = request.getParameter("action");
   System.out.println("ClearSession.jsp: invalidating session");
   if (action != null && action.equals("CLEAR SESSION")) {
        session.invalidate();
   }
%>
<BR><BR><BR>
Served From Server: <b><%= request.getServerName() %></b>

<BR><BR>
<B>Instruction</B>
<UL>
<LI>Click on START NEW SESSION to start a new session</LI>
</UL>
<BR>
<A HREF="HaJsp.jsp" NAME="Link3">START NEW SESSION</A>
</BODY>
</HTML>
