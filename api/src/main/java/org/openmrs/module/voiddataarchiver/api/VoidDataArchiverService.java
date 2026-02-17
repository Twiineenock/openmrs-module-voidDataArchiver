/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.voiddataarchiver.api;

import java.util.List;

import org.openmrs.api.OpenmrsService;
import org.springframework.transaction.annotation.Transactional;

/**
 * The main service of this module, which is exposed for other modules. See
 * moduleApplicationContext.xml on how it is wired up.
 */
public interface VoidDataArchiverService extends OpenmrsService {
	
	/**
	 * Returns an item by uuid. It can be called by any authenticated user. It is fetched in read
	 * only transaction.
	 * 
	 * @param uuid
	 * @return
	 * @throws APIException
	 */
	
	/**
	 * Gets a list of information about all tables, including voided data counts for voidable
	 * tables.
	 * 
	 * @return list of TableInfo objects
	 */
	@Transactional(readOnly = true)
	List<TableInfo> getAllTableInfo();
	
	/**
	 * Archives voided data for the specified table (and its hierarchy based on dependencies). If
	 * tableName is null, archives ALL voided data in the system.
	 * 
	 * @param tableName the name of the table to archive, or null for global archive.
	 */
	@Transactional
	void runArchival(String tableName);
	
	/**
	 * Gets a list of all archived tables.
	 * 
	 * @return list of TableInfo objects
	 */
	@Transactional(readOnly = true)
	List<TableInfo> getArchivedTables();
	
	/**
	 * Restores an archived table to its original state.
	 * 
	 * @param tableName the name of the source table (e.g. "visit")
	 */
	@Transactional
	void restoreTable(String tableName);
	
	/**
	 * Drops an archive table.
	 * 
	 * @param tableName the name of the archive table to drop (e.g. "archive_visit")
	 */
	@Transactional
	void dropArchiveTable(String tableName);
	
	@Transactional(readOnly = true)
	java.util.Map<String, List<String>> getTableDependencies();
}
