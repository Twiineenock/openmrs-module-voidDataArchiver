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
        padding: 6px 8px;
        font-size: 13px;
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
        padding: 3px 5px;
        box-sizing: border-box;
        font-size: 12px;
      }

      /* Collapsible sections */
      details.section-toggle {
        margin-bottom: 10px;
      }

      details.section-toggle>summary {
        cursor: pointer;
        padding: 8px 12px;
        background-color: #eef;
        border: 1px solid #ccd;
        border-radius: 4px;
        font-size: 15px;
        font-weight: bold;
        user-select: none;
        list-style: none;
      }

      details.section-toggle>summary::-webkit-details-marker {
        display: none;
      }

      details.section-toggle>summary::before {
        content: "\25B6 ";
        font-size: 12px;
        margin-right: 6px;
        display: inline-block;
        transition: transform 0.2s;
      }

      details.section-toggle[open]>summary::before {
        content: "\25BC ";
      }

      details.section-toggle>.section-content {
        padding: 10px 0;
      }

      .badge-live {
        background-color: #e8f5e9;
        color: #2e7d32;
        padding: 2px 8px;
        border-radius: 4px;
        font-weight: bold;
        font-size: 12px;
      }

      .badge-voided {
        background-color: #ffebee;
        color: #c62828;
        padding: 2px 8px;
        border-radius: 4px;
        font-weight: bold;
        font-size: 12px;
      }

      .btn-archive {
        background-color: #c62828;
        color: white;
        border: none;
        padding: 3px 10px;
        border-radius: 4px;
        cursor: pointer;
        font-size: 12px;
      }

      .btn-archive:hover {
        background-color: #b71c1c;
      }

      .btn-restore {
        background-color: #2e7d32;
        color: white;
        border: none;
        padding: 3px 10px;
        border-radius: 4px;
        cursor: pointer;
        font-size: 12px;
      }

      .btn-restore:hover {
        background-color: #1b5e20;
      }

      .btn-drop {
        background-color: #fff;
        color: #c62828;
        border: 1px solid #c62828;
        padding: 3px 10px;
        border-radius: 4px;
        cursor: pointer;
        font-size: 12px;
      }

      .btn-drop:hover {
        background-color: #ffebee;
      }
    </style>

    <%--========Section 1: Non-Voidable Tables========--%>
      <details class="section-toggle">
        <summary>1. Non-Voidable Tables (${fn:length(nonVoidableTables)} tables)</summary>
        <div class="section-content">
          <p><i>Tables that do not support voiding.</i></p>
          <div class="column-container">
            <c:forEach var="info" items="${nonVoidableTables}">
              <div class="column-item" title="${info.tableName}">${info.prettyName}</div>
            </c:forEach>
          </div>
        </div>
      </details>

      <%--========Section: Dependency Graph========--%>
        <details class="section-toggle">
          <summary>Dependency Graph (Foreign Keys)</summary>
          <div class="section-content">
            <p><i>Shows <b>foreign key relationships</b> between tables that currently contain voided data.
                Each arrow points from a <b>child table</b> (has FK column) to its <b>parent table</b> (referenced PK).
                Only tables with voided records are shown. Self-references are excluded.</i></p>

            <div id="dependencyGraphNetwork"
              style="width: 100%; height: 400px; border: 1px solid lightgray; background-color: #f9f9f9;"></div>

            <script type="text/javascript"
              src="https://unpkg.com/vis-network/standalone/umd/vis-network.min.js"></script>
            <script type="text/javascript">
              try {
                var graphData = ${ visGraphData };
                var container = document.getElementById('dependencyGraphNetwork');

                if (graphData.nodes.length > 0) {
                  var options = {
                    nodes: {
                      shape: 'box',
                      font: { multi: true },
                      color: {
                        background: '#dbeafe',
                        border: '#2563eb'
                      }
                    },
                    edges: {
                      arrows: 'to',
                      color: { color: '#848484' },
                      smooth: {
                        type: 'cubicBezier',
                        forceDirection: 'horizontal',
                        roundness: 0.4
                      }
                    },
                    layout: {
                      hierarchical: {
                        direction: 'LR',
                        sortMethod: 'directed',
                        levelSeparation: 200,
                        nodeSpacing: 150
                      }
                    },
                    physics: false,
                    interaction: {
                      dragNodes: false,
                      dragView: false,
                      zoomView: false,
                      selectable: false,
                      hover: false
                    }
                  };
                  var network = new vis.Network(container, graphData, options);
                  network.fit();
                } else {
                  container.innerHTML = '<p style="padding:20px; text-align:center; color:gray;">No voided data dependencies found to visualize.</p>';
                }
              } catch (e) {
                console.error("Failed to initialize dependency graph", e);
                document.getElementById('dependencyGraphNetwork').innerHTML = '<p style="color:red; padding:10px;">Error loading dependency graph: ' + e.message + '</p>';
              }
            </script>
          </div>
        </details>

        <%--========Section 2: Voidable Tables Overview========--%>
          <details class="section-toggle" open>
            <summary>2. Voidable Tables &mdash; Live &amp; Voided Data (${fn:length(allVoidableTables)} tables)
            </summary>
            <div class="section-content">
              <p><i><span style="color: #2e7d32;">&#9632; Green = Live</span> &nbsp; <span
                    style="color: #c62828;">&#9632; Red = Voided</span></i></p>

              <table cellpadding="2" cellspacing="0" class="archiver-data-table">
                <thead>
                  <tr>
                    <th>Table Name</th>
                    <th>Total</th>
                    <th style="color: #2e7d32;">Live</th>
                    <th style="color: #c62828;">Voided</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  <c:forEach var="info" items="${allVoidableTables}">
                    <tr>
                      <td title="${info.tableName}"><b>${info.prettyName}</b> <span
                          style="font-size: 11px; color: #888;">(${info.tableName})</span></td>
                      <td>${info.totalRecords}</td>
                      <td><span class="badge-live">${info.liveRecords}</span></td>
                      <td>
                        <c:choose>
                          <c:when test="${info.voidedRecords > 0}">
                            <span class="badge-voided">${info.voidedRecords}</span>
                          </c:when>
                          <c:otherwise>
                            <span style="color: #999;">0</span>
                          </c:otherwise>
                        </c:choose>
                      </td>
                      <td>
                        <c:if test="${info.voidedRecords > 0}">
                          <form method="post" style="display:inline; margin:0;">
                            <input type="hidden" name="archive" value="${info.tableName}" />
                            <input type="submit" value="Archive Voided" class="btn-archive"
                              onclick="return confirm('Archive ${info.voidedRecords} voided records from ${info.tableName}?');" />
                          </form>
                        </c:if>
                        <c:if test="${info.voidedRecords == 0 || info.voidedRecords == null}">
                          <span style="color: #4caf50; font-size: 12px;">&#10004; Clean</span>
                        </c:if>
                      </td>
                    </tr>
                  </c:forEach>
                </tbody>
              </table>
            </div>
          </details>

          <%--========Section 3: Voided Records Detail========--%>
            <c:if test="${not empty voidableDataTables}">
              <details class="section-toggle">
                <summary>3. Voided Records Detail (${fn:length(voidableDataTables)} tables)</summary>
                <div class="section-content">
                  <p><i>Individual voided records for each table. Each sub-section is also expandable.</i></p>

                  <c:forEach var="info" items="${voidableDataTables}">
                    <details
                      style="margin: 6px 0; border: 1px solid #e57373; border-radius: 4px; background-color: #fff8f8;">
                      <summary
                        style="padding: 6px 10px; cursor: pointer; color: #c62828; font-weight: bold; font-size: 13px;">
                        ${info.prettyName}
                        <span style="font-size: small; color: gray;">(${info.tableName})</span>
                        &mdash; <span class="badge-voided">${info.voidedRecords}</span> voided
                      </summary>
                      <div style="padding: 6px 10px;">
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
                              <tr style="background-color: #ffebee;">
                                <td>${entry['uuid']}</td>
                                <td>${entry['voidedBy']}</td>
                                <td>${entry['dateVoided']}</td>
                                <td>${entry['voidReason']}</td>
                              </tr>
                            </c:forEach>
                          </tbody>
                        </table>
                      </div>
                    </details>
                  </c:forEach>
                </div>
              </details>
            </c:if>

            <%--========Section 4: Archived Tables========--%>
              <details class="section-toggle" open>
                <summary>4. Archived Tables (${fn:length(archivedTables)} archives)</summary>
                <div class="section-content">
                  <p><i>Data that has been moved to archive tables.</i></p>
                  <table cellpadding="2" cellspacing="0" class="archiver-data-table">
                    <thead>
                      <tr>
                        <th>Archive Table</th>
                        <th>Source Table</th>
                        <th>Records</th>
                        <th>Action</th>
                      </tr>
                    </thead>
                    <tbody>
                      <c:forEach var="info" items="${archivedTables}">
                        <tr>
                          <td>archive_${info.tableName}</td>
                          <td>${info.tableName}</td>
                          <td>${info.totalRecords}</td>
                          <td>
                            <form method="post" style="display:inline-block; margin:0;">
                              <input type="hidden" name="restore" value="${info.tableName}" />
                              <input type="submit" value="Restore" class="btn-restore"
                                onclick="return confirm('Restore ${info.tableName}? This will move data back and drop the archive table.');" />
                            </form>
                            <form method="post" style="display:inline-block; margin:0; margin-left: 4px;">
                              <input type="hidden" name="dropArchive" value="archive_${info.tableName}" />
                              <input type="submit" value="Drop Archive" class="btn-drop"
                                onclick="return confirm('WARNING: Permanently DELETE archive_${info.tableName}? This CANNOT be undone.');" />
                            </form>
                          </td>
                        </tr>
                      </c:forEach>
                      <c:if test="${empty archivedTables}">
                        <tr>
                          <td colspan="4">No archived tables found.</td>
                        </tr>
                      </c:if>
                    </tbody>
                  </table>
                </div>
              </details>

              <div class="box" style="margin-top: 10px;">
                <p>
                  <spring:message code="voiddataarchiver-omod.description" />
                </p>
              </div>

              <%@ include file="/WEB-INF/template/footer.jsp" %>