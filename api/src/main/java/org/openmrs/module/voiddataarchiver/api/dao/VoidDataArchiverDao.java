/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.voiddataarchiver.api.dao;

import java.util.List;

import org.openmrs.module.voiddataarchiver.api.TableInfo;

/**
 * Database access for VoidDataArchiver
 */
public interface VoidDataArchiverDao {
	
	/**
	 * Gets a list of information about all tables, including voided data counts for voidable
	 * tables.
	 * 
	 * @return list of TableInfo objects
	 */
	List<TableInfo> getAllTableInfo();
	
	/**
	 * Creates (if invalid/missing) the archive table for the given source table. Also performs
	 * schema sync if the table exists but is missing columns.
	 * 
	 * @param tableName the name of the source table (e.g. "visit")
	 */
	void createArchiveTable(String tableName);
	
	/**
	 * Moves a batch of voided rows from source to archive.
	 * 
	 * @param tableName the name of the source table
	 * @param limit batch size (e.g. 1000)
	 * @return number of rows moved
	 */
	int archiveBatch(String tableName, int limit);
	
	/**
	 * Retrieves the foreign key relationships between tables.
	 * 
	 * @return Map where Key = Child Table, Value = List of Parent Tables
	 */
	java.util.Map<String, List<String>> getTableDependencies();
	
	/**
	 * Gets a list of all archived tables (tables starting with 'archive_').
	 * 
	 * @return list of TableInfo objects representing archived tables
	 */
	List<TableInfo> getArchivedTables();
	
	/**
	 * Restores data from the archive table back to the source table.
	 * 
	 * @param tableName the name of the source table (e.g. "visit")
	 */
	void restoreTable(String tableName);
	
	/**
	 * Drops the specified table. Use with caution!
	 * 
	 * @param tableName the name of the table to drop
	 */
	void dropArchiveTable(String tableName);
}
