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
import org.openmrs.module.voiddataarchiver.api.TableInfo;
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
					
					log.info("Found table: " + tableName);
					
					if (tableName.toLowerCase().startsWith("liquibase")
					        || tableName.toLowerCase().startsWith("databasechangelog")
					        || tableName.toLowerCase().startsWith("archive_")) {
						log.info("Skipping table: " + tableName);
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
						java.sql.Statement stmt = null;
						java.sql.ResultSet rs = null;
						try {
							stmt = connection.createStatement();
							
							// Count total records (live + voided)
							String totalCountSql = "SELECT COUNT(*) FROM " + tableName;
							rs = stmt.executeQuery(totalCountSql);
							if (rs.next()) {
								info.setTotalRecords(rs.getLong(1));
							}
							rs.close();
							
							// Count voided records
							String countSql = "SELECT COUNT(*) FROM " + tableName + " WHERE voided = 1";
							rs = stmt.executeQuery(countSql);
							if (rs.next()) {
								long count = rs.getLong(1);
								info.setVoidedRecords(count);
								
								if (count > 0) {
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
									if (hasVoidedBy)
										selectCols.append("t.voided_by");
									else
										selectCols.append("null as voided_by");
									selectCols.append(", ");
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
										entriesSql += "left join users u on t.voided_by = u.user_id ";
									}
									entriesSql += "where t.voided = 1";
									
									if (hasDateVoided)
										entriesSql += " order by t.date_voided desc";
									
									java.sql.Statement maxStmt = connection.createStatement();
									maxStmt.setMaxRows(50);
									java.sql.ResultSet entriesRs = maxStmt.executeQuery(entriesSql);
									
									List<Map<String, Object>> voidedEntries = new ArrayList<Map<String, Object>>();
									while (entriesRs.next()) {
										Map<String, Object> entry = new HashMap<String, Object>();
										entry.put("uuid", entriesRs.getString(1));
										String voidedById = entriesRs.getString(2);
										String username = entriesRs.getString(3);
										if (username != null && !username.isEmpty()) {
											entry.put("voidedBy", username);
										} else if (voidedById != null) {
											entry.put("voidedBy", "User #" + voidedById);
										} else {
											entry.put("voidedBy", "");
										}
										entry.put("dateVoided", entriesRs.getDate(4));
										entry.put("voidReason", entriesRs.getString(5));
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
	
	@Override
	public void createArchiveTable(final String tableName) {
		sessionFactory.getCurrentSession().doWork(new org.hibernate.jdbc.Work() {
			
			@Override
			public void execute(java.sql.Connection connection) throws java.sql.SQLException {
				String archiveTableName = "archive_" + tableName;
				java.sql.DatabaseMetaData dbmd = connection.getMetaData();
				java.sql.ResultSet tables = dbmd.getTables(null, null, archiveTableName, new String[] { "TABLE" });
				boolean archiveExists = tables.next();
				tables.close();
				
				if (!archiveExists) {
					log.info("Creating archive table: " + archiveTableName);
					String dbProduct = dbmd.getDatabaseProductName().toLowerCase();
					String createSql;
					if (dbProduct.contains("mysql")) {
						createSql = "CREATE TABLE " + archiveTableName + " LIKE " + tableName;
					} else if (dbProduct.contains("postgresql")) {
						createSql = "CREATE TABLE " + archiveTableName + " (LIKE " + tableName
						        + " INCLUDING DEFAULTS INCLUDING IDENTITY INCLUDING INDEXES)";
					} else {
						createSql = "CREATE TABLE " + archiveTableName + " AS SELECT * FROM " + tableName + " WHERE 1=0";
					}
					java.sql.Statement stmt = connection.createStatement();
					stmt.execute(createSql);
					stmt.close();
				} else {
					List<String> sourceColumns = getColumnNames(dbmd, tableName);
					List<String> archiveColumns = getColumnNames(dbmd, archiveTableName);
					sourceColumns.removeAll(archiveColumns);
					if (!sourceColumns.isEmpty()) {
						log.info("Syncing schema for " + archiveTableName + ". Adding columns: " + sourceColumns);
						java.sql.Statement stmt = connection.createStatement();
						for (String missingCol : sourceColumns) {
							String colDef = getColumnDefinition(dbmd, tableName, missingCol);
							if (colDef != null) {
								try {
									stmt.execute("ALTER TABLE " + archiveTableName + " ADD COLUMN " + missingCol + " "
									        + colDef);
								}
								catch (Exception e) {
									log.error("Failed to sync column " + missingCol, e);
								}
							}
						}
						stmt.close();
					}
				}
			}
			
			private List<String> getColumnNames(java.sql.DatabaseMetaData dbmd, String tName) throws java.sql.SQLException {
				List<String> cols = new ArrayList<String>();
				java.sql.ResultSet rs = dbmd.getColumns(null, null, tName, "%");
				while (rs.next()) {
					cols.add(rs.getString("COLUMN_NAME"));
				}
				rs.close();
				return cols;
			}
			
			private String getColumnDefinition(java.sql.DatabaseMetaData dbmd, String tName, String cName)
			        throws java.sql.SQLException {
				java.sql.ResultSet rs = dbmd.getColumns(null, null, tName, cName);
				if (rs.next()) {
					String type = rs.getString("TYPE_NAME");
					int size = rs.getInt("COLUMN_SIZE");
					return type + "(" + size + ")";
				}
				return null;
			}
		});
	}
	
	@Override
	public int archiveBatch(final String tableName, final int limit) {
		final List<Object> idsToMove = new ArrayList<Object>();
		final String[] pkColumn = new String[1];
		
		sessionFactory.getCurrentSession().doWork(new org.hibernate.jdbc.Work() {
			
			@Override
			public void execute(java.sql.Connection connection) throws java.sql.SQLException {
				java.sql.DatabaseMetaData dbmd = connection.getMetaData();
				java.sql.ResultSet pkRs = dbmd.getPrimaryKeys(null, null, tableName);
				if (pkRs.next()) {
					pkColumn[0] = pkRs.getString("COLUMN_NAME");
				} else {
					pkColumn[0] = "uuid";
				}
				pkRs.close();
				
				String selectSql = "SELECT " + pkColumn[0] + " FROM " + tableName + " WHERE voided = 1 ORDER BY "
				        + pkColumn[0] + " DESC LIMIT " + limit;
				java.sql.Statement stmt = connection.createStatement();
				java.sql.ResultSet rs = stmt.executeQuery(selectSql);
				while (rs.next()) {
					idsToMove.add(rs.getObject(1));
				}
				rs.close();
				stmt.close();
			}
		});
		
		if (idsToMove.isEmpty()) {
			return 0;
		}
		
		sessionFactory.getCurrentSession().doWork(new org.hibernate.jdbc.Work() {
			
			@Override
			public void execute(java.sql.Connection connection) throws java.sql.SQLException {
				boolean oldAutoCommit = connection.getAutoCommit();
				java.sql.Statement stmt = null;
				try {
					connection.setAutoCommit(false);
					stmt = connection.createStatement();
					
					// CRITICAL: Disable FK checks so that cross-table references
					// (e.g. orders -> encounter, obs -> encounter) don't block deletion.
					// This is the standard approach for MySQL data migration/archival.
					stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
					
					String archiveTableName = "archive_" + tableName;
					StringBuilder idListBuilder = new StringBuilder();
					for (Object id : idsToMove) {
						if (idListBuilder.length() > 0)
							idListBuilder.append(",");
						if (id instanceof String) {
							idListBuilder.append("'").append(id).append("'");
						} else {
							idListBuilder.append(id);
						}
					}
					String idList = idListBuilder.toString();
					
					// 1. Copy data to archive (INSERT IGNORE to skip any duplicates from prior
					// failed attempts)
					String insertSql = "INSERT IGNORE INTO " + archiveTableName + " SELECT * FROM " + tableName + " WHERE "
					        + pkColumn[0] + " IN (" + idList + ")";
					int inserted = stmt.executeUpdate(insertSql);
					log.info("Copied " + inserted + " rows from " + tableName + " to " + archiveTableName);
					
					// 2. Delete data from source
					String deleteSql = "DELETE FROM " + tableName + " WHERE " + pkColumn[0] + " IN (" + idList + ")";
					int deleted = stmt.executeUpdate(deleteSql);
					log.info("Deleted " + deleted + " rows from " + tableName);
					
					// 3. Re-enable FK checks BEFORE commit
					stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
					
					connection.commit();
				}
				catch (Exception e) {
					log.error("Failed to archive batch for " + tableName + ". Rolling back.", e);
					try {
						if (stmt != null) {
							stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
						}
					}
					catch (Exception ignored) {}
					connection.rollback();
					throw new java.sql.SQLException("Failed to archive batch " + tableName, e);
				}
				finally {
					if (stmt != null) {
						try {
							stmt.close();
						}
						catch (Exception ignored) {}
					}
					connection.setAutoCommit(oldAutoCommit);
				}
			}
		});
		
		return idsToMove.size();
	}
	
	@Override
	public Map<String, List<String>> getTableDependencies() {
		final Map<String, List<String>> dependencies = new HashMap<String, List<String>>();
		
		sessionFactory.getCurrentSession().doWork(new org.hibernate.jdbc.Work() {
			
			@Override
			public void execute(java.sql.Connection connection) throws java.sql.SQLException {
				java.sql.DatabaseMetaData dbmd = connection.getMetaData();
				// Get all tables first
				java.sql.ResultSet tables = dbmd.getTables(null, null, "%", new String[] { "TABLE" });
				List<String> tableNames = new ArrayList<String>();
				while (tables.next()) {
					String t = tables.getString("TABLE_NAME");
					if (!t.toLowerCase().startsWith("liquibase") && !t.toLowerCase().startsWith("databasechangelog")
					        && !t.toLowerCase().startsWith("archive_")) {
						tableNames.add(t);
					}
				}
				tables.close();
				
				for (String childTable : tableNames) {
					java.sql.ResultSet importedKeys = null;
					try {
						importedKeys = dbmd.getImportedKeys(null, null, childTable);
					}
					catch (Exception e) {
						// Ignore errors for specific tables if any
						continue;
					}
					
					List<String> parents = new ArrayList<String>();
					while (importedKeys.next()) {
						String parentTable = importedKeys.getString("PKTABLE_NAME");
						// Avoid self-references or duplicates
						if (parentTable != null && !parentTable.equalsIgnoreCase(childTable)
						        && !parents.contains(parentTable)) {
							parents.add(parentTable);
						}
					}
					importedKeys.close();
					if (!parents.isEmpty()) {
						dependencies.put(childTable, parents);
					}
				}
			}
		});
		return dependencies;
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
	
	@Override
	public List<TableInfo> getArchivedTables() {
		final List<TableInfo> archivedTables = new ArrayList<TableInfo>();
		
		sessionFactory.getCurrentSession().doWork(new org.hibernate.jdbc.Work() {
			
			@Override
			public void execute(java.sql.Connection connection) throws java.sql.SQLException {
				java.sql.DatabaseMetaData dbmd = connection.getMetaData();
				java.sql.ResultSet tables = dbmd.getTables(null, null, "archive_%", new String[] { "TABLE" });
				
				// First pass: collect all archive table info
				List<String> allArchiveNames = new ArrayList<String>();
				while (tables.next()) {
					allArchiveNames.add(tables.getString("TABLE_NAME"));
				}
				tables.close();
				
				// Second pass: count rows and skip empties
				
				for (String archiveTableName : allArchiveNames) {
					String sourceTableName = archiveTableName.substring("archive_".length());
					
					java.sql.Statement stmt = connection.createStatement();
					java.sql.ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + archiveTableName);
					long rowCount = 0;
					if (rs.next()) {
						rowCount = rs.getLong(1);
					}
					rs.close();
					stmt.close();
					
					// Skip empty archive tables
					if (rowCount == 0) {
						continue;
					}
					
					TableInfo info = new TableInfo(sourceTableName, true);
					info.setPrettyName(prettifyTableName(sourceTableName) + " (Archived)");
					info.setTotalRecords(rowCount);
					archivedTables.add(info);
				}
			}
		});
		return archivedTables;
	}
	
	@Override
	public void restoreTable(final String tableName) {
		sessionFactory.getCurrentSession().doWork(new org.hibernate.jdbc.Work() {
			
			@Override
			public void execute(java.sql.Connection connection) throws java.sql.SQLException {
				String archiveTableName = "archive_" + tableName;
				
				// 0. Get PK column
				String pkColumn = "uuid";
				java.sql.DatabaseMetaData dbmd = connection.getMetaData();
				java.sql.ResultSet pkRs = dbmd.getPrimaryKeys(null, null, tableName);
				if (pkRs.next()) {
					pkColumn = pkRs.getString("COLUMN_NAME");
				}
				pkRs.close();
				
				// 1. Restore data (ignoring duplicates if they already exist in source)
				// Using standard SQL: INSERT INTO target SELECT * FROM source WHERE pk NOT IN
				// (SELECT pk FROM target)
				String restoreSql = "INSERT INTO " + tableName + " SELECT * FROM " + archiveTableName + " WHERE " + pkColumn
				        + " NOT IN (SELECT " + pkColumn + " FROM " + tableName + ")";
				
				java.sql.Statement stmt = connection.createStatement();
				try {
					// Disable FK checks for the same reason as archiveBatch
					stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
					
					log.info("Restoring table " + tableName + " from " + archiveTableName);
					int rows = stmt.executeUpdate(restoreSql);
					log.info("Restored " + rows + " rows. (Skipped duplicates)");
					
					// 2. Drop archive table after successful restore
					String dropSql = "DROP TABLE " + archiveTableName;
					stmt.execute(dropSql);
					
					stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
				}
				finally {
					try {
						stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
					}
					catch (Exception ignored) {}
					stmt.close();
				}
			}
		});
	}
	
	@Override
	public void dropArchiveTable(final String tableName) {
		sessionFactory.getCurrentSession().doWork(new org.hibernate.jdbc.Work() {
			
			@Override
			public void execute(java.sql.Connection connection) throws java.sql.SQLException {
				if (!tableName.toLowerCase().startsWith("archive_")) {
					log.warn("Attempted to drop non-archive table: " + tableName + ". Operation blocked.");
					return;
				}
				java.sql.Statement stmt = connection.createStatement();
				stmt.execute("DROP TABLE " + tableName);
				stmt.close();
				log.info("Dropped table: " + tableName);
			}
		});
	}
}
