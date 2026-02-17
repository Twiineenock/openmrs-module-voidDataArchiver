<%@ include file="/WEB-INF/template/include.jsp" %>

  <%@ include file="/WEB-INF/template/header.jsp" %>

    <h2>
      <spring:message code="voiddataarchiver-omod.title" />
    </h2>

    <br />
    <c:if test="${not empty openmrs_msg}">
      <div id="openmrs_msg">${openmrs_msg}</div>
    </c:if>
    <c:if test="${not empty openmrs_error}">
      <div id="openmrs_error" class="error">${openmrs_error}</div>
    </c:if>

    <style>
      .archiver-data-table {
        width: 100%;
        border-collapse: collapse;
      }

      .archiver-data-table th,
      .archiver-data-table td {
        border: 1px solid #ddd;
        padding: 8px;
      }

      .archiver-data-table th {
        background-color: #f2f2f2;
        text-align: left;
      }

      .archiver-data-table tr:nth-child(even) {
        background-color: #f9f9f9;
      }

      .archiver-data-table tr:hover {
        background-color: #f1f1f1;
      }

      .column-container {
        display: flex;
        flex-wrap: wrap;
      }

      .column-item {
        flex: 33%;
        padding: 5px;
        box-sizing: border-box;
      }
    </style>

    <div class="box">
      <h3>1. Non-Voidable Tables</h3>
      <p><i>Tables that do not support voiding.</i></p>
      <div class="column-container">
        <c:forEach var="info" items="${nonVoidableTables}">
          <div class="column-item" title="${info.tableName}">${info.prettyName}</div>
        </c:forEach>
      </div>
    </div>

    <br />

    <div class="box">
      <h3>2. Voidable Tables (No Voided Data)</h3>
      <p><i>Tables that support voiding but currently have 0 voided records.</i></p>
      <table cellpadding="2" cellspacing="0" class="archiver-data-table" style="width: 50%;">
        <thead>
          <tr>
            <th>Table Name</th>
            <th>Voided Records</th>
          </tr>
        </thead>
        <tbody>
          <c:forEach var="info" items="${voidableCleanTables}">
            <tr>
              <td title="${info.tableName}">${info.prettyName}</td>
              <td>0</td>
            </tr>
          </c:forEach>
        </tbody>
      </table>
    </div>

    <br />

    <div class="box">
      <h3>3. Voidable Tables WITH Voided Data</h3>
      <p><i>Tables containing voided data.</i></p>

      <c:if test="${empty voidableDataTables}">
        <p>No tables found with voided data.</p>
      </c:if>

      <c:forEach var="info" items="${voidableDataTables}">
        <fieldset style="margin-top: 10px; border: 1px solid #ccc; padding: 10px;">
          <legend style="padding: 0 5px;">
            <b>${info.prettyName}</b> <span style="font-size: small; color: gray;">(${info.tableName})</span> (Count:
            <span style="color: red;">${info.voidedRecords}</span>)
            <form method="post" style="display:inline; float:right; margin:0;">
              <input type="hidden" name="archive" value="${info.tableName}" />
              <input type="submit" value="Archive Now"
                onclick="return confirm('Are you sure you want to archive ${info.voidedRecords} records from ${info.tableName}? This action cannot be undone efficiently.');" />
            </form>
          </legend>

          <table cellpadding="2" cellspacing="0" class="archiver-data-table">
            <thead>
              <tr>
                <th>UUID</th>
                <th>Voided By</th>
                <th>Date Voided</th>
                <th>Void Reason</th>
              </tr>
            </thead>
            <tbody>
              <c:forEach var="entry" items="${info.voidedEntries}">
                <tr>
                  <td>${entry['uuid']}</td>
                  <td>${entry['voidedBy']}</td>
                  <td>${entry['dateVoided']}</td>
                  <td>${entry['voidReason']}</td>
                </tr>
              </c:forEach>
            </tbody>
          </table>
        </fieldset>
      </c:forEach>
    </div>

    <div class="box">
      <p>
        <spring:message code="voiddataarchiver-omod.description" />
      </p>
    </div>

    <%@ include file="/WEB-INF/template/footer.jsp" %>