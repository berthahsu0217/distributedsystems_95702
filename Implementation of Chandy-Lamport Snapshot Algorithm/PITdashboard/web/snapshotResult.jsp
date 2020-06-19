<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.util.LinkedList"%>
<%@page import="java.util.Iterator"%>
<%@page import="java.util.HashMap"%>
<% LinkedList commodity = (LinkedList) request.getAttribute("commodity"); %>
<% LinkedList state = (LinkedList) request.getAttribute("state"); %>

<table id="rtab" border="1" cellpadding="3">
    <tr><th align="center">Player</th>
    <% Iterator<String> ic = commodity.iterator(); while(ic.hasNext()){ %>
        <th align="center">Quantity: <%= ic.next() %></th>
    <%    } %>
    </tr>
     <% Iterator<HashMap> is = state.iterator(); while(is.hasNext()){ HashMap h = is.next();%>
     <tr align="center">
     <td><%= h.get("Player") %></td>
     <% Iterator<String> ic2 = commodity.iterator(); while(ic2.hasNext()){ String c = ic2.next();%>
     <td class=<%= c%>><% Integer count = (Integer) h.get(c); out.print((count==null)?0:count); %></td>
    <% } %>
     </tr>
     <% } %>
</table>










