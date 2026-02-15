<%@ include file="/WEB-INF/template/include.jsp" %>

  <%@ include file="/WEB-INF/template/header.jsp" %>

    <h2>
      <spring:message code="voiddataarchiver-omod.title" />
    </h2>

    <br />
    <!-- <table>
  <tr>
   <th>User Id</th>
   <th>Username</th>
  </tr>
  <c:forEach var="user" items="${users}">
      <tr>
        <td>${user.userId}</td>
        <td>${user.systemId}</td>
      </tr>		
  </c:forEach>
</table> -->

    <div class="box">
      <p>
        <spring:message code="voiddataarchiver-omod.description" />
      </p>
    </div>

    <%@ include file="/WEB-INF/template/footer.jsp" %>