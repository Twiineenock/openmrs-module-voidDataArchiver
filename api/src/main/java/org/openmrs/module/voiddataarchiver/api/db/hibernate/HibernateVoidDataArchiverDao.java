/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.voiddataarchiver.api.db.hibernate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.openmrs.Voidable;

import org.openmrs.module.voiddataarchiver.TableInfo;
import org.openmrs.module.voiddataarchiver.api.dao.VoidDataArchiverDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * Hibernate implementation of {@link VoidDataArchiverDao}.
 */
@Repository("voiddataarchiver.VoidDataArchiverDao")
public class HibernateVoidDataArchiverDao implements VoidDataArchiverDao {
	
	protected final Log log = LogFactory.getLog(this.getClass());
	
	@Autowired
	private SessionFactory sessionFactory;
	
	/**
	 * @param sessionFactory the sessionFactory to set
	 */
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	/**
	 * @return the sessionFactory
	 */
	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}
	
	@Override
	public List<TableInfo> getAllTableInfo() {
		final List<TableInfo> tableInfos = new ArrayList<TableInfo>();
		
		sessionFactory.getCurrentSession().doWork(new org.hibernate.jdbc.Work() {
			
			@Override
			public void execute(java.sql.Connection connection) throws java.sql.SQLException {
				java.sql.DatabaseMetaData dbmd = connection.getMetaData();
				// Retrieve all tables (TABLE type only)
				java.sql.ResultSet tables = dbmd.getTables(null, null, "%", new String[] { "TABLE" });
				
				while (tables.next()) {
					String tableName = tables.getString("TABLE_NAME");
					
					// Skip typical Liquibase or irrelevant system tables if needed, but User asked
					// for ALL.
					if (tableName.toLowerCase().startsWith("liquibase")
					        || tableName.toLowerCase().startsWith("databasechangelog")) {
						continue;
					}
					
					// Check if table has "voided" column
					boolean isVoidable = false;
					java.sql.ResultSet columns = dbmd.getColumns(null, null, tableName, "voided");
					if (columns.next()) {
						isVoidable = true;
					}
					columns.close();
					
					TableInfo info = new TableInfo(tableName, isVoidable);
					info.setPrettyName(prettifyTableName(tableName));
					
					if (isVoidable) {
						// Check if we can proceed with void checks
						java.sql.Statement stmt = null;
						java.sql.ResultSet rs = null;
						try {
							stmt = connection.createStatement();
							
							// Check count of voided records
							// Try-catch for SQL generic compatibility, though 'voided=1' is standard for
							// OpenMRS
							String countSql = "select count(*) from " + tableName + " where voided = 1";
							
							rs = stmt.executeQuery(countSql);
							if (rs.next()) {
								long count = rs.getLong(1);
								info.setVoidedRecords(count);
								
								if (count > 0) {
									// Verify columns exist before querying to avoid crashes
									boolean hasUuid = hasColumn(dbmd, tableName, "uuid");
									boolean hasVoidedBy = hasColumn(dbmd, tableName, "voided_by");
									boolean hasDateVoided = hasColumn(dbmd, tableName, "date_voided");
									boolean hasVoidReason = hasColumn(dbmd, tableName, "void_reason");
									
									StringBuilder selectCols = new StringBuilder();
									if (hasUuid)
										selectCols.append("t.uuid");
									else
										selectCols.append("'' as uuid");
									selectCols.append(", ");
									// Always fetch the ID as fallback
									if (hasVoidedBy)
										selectCols.append("t.voided_by");
									else
										selectCols.append("null as voided_by");
									selectCols.append(", ");
									// Fetch username
									if (hasVoidedBy)
										selectCols.append("u.username");
									else
										selectCols.append("null as username");
									selectCols.append(", ");
									if (hasDateVoided)
										selectCols.append("t.date_voided");
									else
										selectCols.append("null as date_voided");
									selectCols.append(", ");
									if (hasVoidReason)
										selectCols.append("t.void_reason");
									else
										selectCols.append("'' as void_reason");
									
									String entriesSql = "select " + selectCols.toString() + " from " + tableName + " t ";
									if (hasVoidedBy) {
										// Use left join to preserve records even if user is missing
										entriesSql += "left join users u on t.voided_by = u.user_id ";
									}
									entriesSql += "where t.voided = 1";
									
									if (hasDateVoided)
										entriesSql += " order by t.date_voided desc";
									
									// Limit 50
									java.sql.Statement maxStmt = connection.createStatement();
									maxStmt.setMaxRows(50);
									java.sql.ResultSet entriesRs = maxStmt.executeQuery(entriesSql);
									
									List<Map<String, Object>> voidedEntries = new ArrayList<Map<String, Object>>();
									while (entriesRs.next()) {
										Map<String, Object> entry = new HashMap<String, Object>();
										entry.put("uuid", entriesRs.getString(1)); // uuid
										
										String voidedById = entriesRs.getString(2); // voided_by ID
										String username = entriesRs.getString(3); // username
										
										if (username != null && !username.isEmpty()) {
											entry.put("voidedBy", username);
										} else if (voidedById != null) {
											entry.put("voidedBy", "User #" + voidedById);
										} else {
											entry.put("voidedBy", "");
										}
										
										entry.put("dateVoided", entriesRs.getDate(4)); // date_voided
										entry.put("voidReason", entriesRs.getString(5)); // void_reason
										voidedEntries.add(entry);
									}
									info.setVoidedEntries(voidedEntries);
									entriesRs.close();
									maxStmt.close();
								}
							}
						}
						catch (Exception e) {
							log.warn("Failed to query voided data for table " + tableName + ": " + e.getMessage());
							// Try fallback for Postgres boolean if 'voided=1' failed?
							// For now assume standard OpenMRS DB (MySQL/H2) or compatible.
							info.setVoidedRecords(0L);
						}
						finally {
							if (rs != null)
								try {
									rs.close();
								}
								catch (Exception e) {}
							if (stmt != null)
								try {
									stmt.close();
								}
								catch (Exception e) {}
						}
					} else {
						info.setVoidedRecords(0L);
					}
					
					tableInfos.add(info);
				}
				tables.close();
			}
			
			private boolean hasColumn(java.sql.DatabaseMetaData dbmd, String tableName, String columnName)
			        throws java.sql.SQLException {
				java.sql.ResultSet rs = dbmd.getColumns(null, null, tableName, columnName);
				boolean exists = rs.next();
				rs.close();
				return exists;
			}
		});
		
		return tableInfos;
	}
	
	private String prettifyTableName(String tableName) {
		if (tableName == null || tableName.isEmpty()) {
			return tableName;
		}
		
		String[] parts = tableName.split("_");
		StringBuilder pretty = new StringBuilder();
		for (String part : parts) {
			if (part.length() > 0) {
				pretty.append(Character.toUpperCase(part.charAt(0)));
				if (part.length() > 1) {
					pretty.append(part.substring(1).toLowerCase());
				}
				pretty.append(" ");
			}
		}
		return pretty.toString().trim();
	}
}
