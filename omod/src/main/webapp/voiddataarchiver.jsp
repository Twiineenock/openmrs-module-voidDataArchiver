<%@ include file="/WEB-INF/template/include.jsp" %>

  <%@ include file="/WEB-INF/template/header.jsp" %>

    <h2>
      <spring:message code="voiddataarchiver-omod.title" />
    </h2>

    <br />
    <div class="box">
        <h3>Voided Data Tables</h3>
        <c:if test="${empty voidedTables}">
            <p>No tables with voided data found.</p>
        </c:if>
        <c:if test="${not empty voidedTables}">
            <table>
                <tr>
                    <th>Table Name</th>
                </tr>
                <c:forEach var="tableName" items="${voidedTables}">
                    <tr>
                        <td>${tableName}</td>
                    </tr>
                </c:forEach>
            </table>
        </c:if>
    </div>

    <div class="box">
      <p>
        <spring:message code="voiddataarchiver-omod.description" />
      </p>
    </div>

    <%@ include file="/WEB-INF/template/footer.jsp" %>