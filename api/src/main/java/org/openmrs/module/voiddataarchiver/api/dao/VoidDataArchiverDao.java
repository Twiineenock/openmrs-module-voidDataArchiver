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

import org.openmrs.module.voiddataarchiver.Item;

/**
 * Database access for VoidDataArchiver
 */
public interface VoidDataArchiverDao {
	
	/**
	 * @return item with given uuid
	 */
	Item getItemByUuid(String uuid);
	
	/**
	 * @return saved item
	 */
	Item saveItem(Item item);
	
	/**
	 * Gets a list of names of tables (or entities) that contain voided data.
	 * 
	 * @return list of table/entity names
	 */
	List<String> getVoidedTableNames();
}
