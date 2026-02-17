/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.voiddataarchiver.api.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.APIException;
import org.openmrs.api.UserService;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.voiddataarchiver.api.TableInfo;
import org.openmrs.module.voiddataarchiver.api.VoidDataArchiverService;
import org.openmrs.module.voiddataarchiver.api.dao.VoidDataArchiverDao;

public class VoidDataArchiverServiceImpl extends BaseOpenmrsService implements VoidDataArchiverService {
	
	protected final Log log = LogFactory.getLog(this.getClass());
	
	VoidDataArchiverDao dao;
	
	UserService userService;
	
	/**
	 * Injected in moduleApplicationContext.xml
	 */
	public void setDao(VoidDataArchiverDao dao) {
		this.dao = dao;
	}
	
	/**
	 * Injected in moduleApplicationContext.xml
	 */
	public void setUserService(UserService userService) {
		this.userService = userService;
	}
	
	@Override
	public List<TableInfo> getAllTableInfo() throws APIException {
		return dao.getAllTableInfo();
	}
	
	@Override
	public void runArchival(String tableName) {
		// 1. Get Dependencies (Child -> [Parents])
		Map<String, List<String>> dependencies = dao.getTableDependencies();
		
		// 2. Identify Target Tables
		Set<String> tablesToProcess = new HashSet<String>();
		
		if (tableName != null && !tableName.isEmpty()) {
			if (tableName.toLowerCase().startsWith("archive_")) {
				log.warn("Cannot archive an archive table: " + tableName);
				return;
			}
			// Find transitive closure of dependency graph for this table
			// But we need to archive CHILDREN first.
			// So if I select "person", I must check everyone who points to "person".
			// i.e. "patient", "user", "provider" etc.
			// So I need the REVERSE graph (Parent -> [Children]).
			Map<String, List<String>> reverseDependencies = new HashMap<String, List<String>>();
			for (Map.Entry<String, List<String>> entry : dependencies.entrySet()) {
				String child = entry.getKey();
				for (String parent : entry.getValue()) {
					if (!reverseDependencies.containsKey(parent)) {
						reverseDependencies.put(parent, new ArrayList<String>());
					}
					reverseDependencies.get(parent).add(child);
				}
			}
			
			// BFS to find all descendants
			Queue<String> queue = new LinkedList<String>();
			queue.add(tableName);
			tablesToProcess.add(tableName);
			
			while (!queue.isEmpty()) {
				String current = queue.poll();
				if (reverseDependencies.containsKey(current)) {
					for (String child : reverseDependencies.get(current)) {
						if (!tablesToProcess.contains(child) && !child.toLowerCase().startsWith("archive_")) {
							tablesToProcess.add(child);
							queue.add(child);
						}
					}
				}
			}
		} else {
			// Global Archive: All voided tables
			List<TableInfo> allInfos = dao.getAllTableInfo();
			for (TableInfo info : allInfos) {
				if (info.isVoidable() && !info.getTableName().toLowerCase().startsWith("archive_")) {
					tablesToProcess.add(info.getTableName());
				}
			}
		}
		
		// 3. Topological Sort (Archive Children First)
		// We sort the subgraph defined by tablesToProcess based on dependencies.
		List<String> sortedTables = topologicalSort(dependencies, tablesToProcess);
		
		log.info("Archiving order: " + sortedTables);
		
		// 4. Execute Archival
		for (String tName : sortedTables) {
			if (tName.toLowerCase().startsWith("archive_")) {
				continue;
			}
			try {
				// Only process if it is voidable (some dependencies might not be voidable, e.g.
				// system tables,
				// though unlikely to be picked up unless recursive check adds them)
				// Actually, if a non-voidable table is a child of a voidable parent, we must
				// check.
				// But we can only archive voidable tables.
				// If a non-voidable child blocks a voidable parent, archival fails for the
				// parent.
				// We proceed with best effort.
				
				// Ensure archive table exists
				dao.createArchiveTable(tName);
				
				// Move data in batches
				int totalMoved = 0;
				int batchSize = 1000;
				int moved;
				do {
					moved = dao.archiveBatch(tName, batchSize);
					totalMoved += moved;
					// Safety break or yield?
					// For now, continue until done.
				} while (moved > 0);
				
				if (totalMoved > 0) {
					log.info("Archived " + totalMoved + " rows from " + tName);
				}
			}
			catch (Exception e) {
				String errorMsg = e.getMessage();
				if (e.getCause() != null) {
					errorMsg += " Caused by: " + e.getCause().getMessage();
				}
				log.error("Error archiving table " + tName + ": " + errorMsg, e);
				// Continue to next table? Or stop?
				// If child archival failed, parent archival will likely fail too.
				
			}
		}
	}
	
	public List<TableInfo> getArchivedTables() {
		return dao.getArchivedTables();
	}
	
	public void restoreTable(String tableName) {
		dao.restoreTable(tableName);
	}
	
	public Map<String, List<String>> getTableDependencies() {
		return dao.getTableDependencies();
	}
	
	public void dropArchiveTable(String tableName) {
		dao.dropArchiveTable(tableName);
	}
	
	private List<String> topologicalSort(Map<String, List<String>> allDeps, Set<String> nodes) {
		List<String> result = new ArrayList<String>();
		
		// Build subgraph adjacency list (Child -> Parent) for the nodes we care about
		Map<String, List<String>> adj = new HashMap<String, List<String>>();
		Map<String, Integer> inDegree = new HashMap<String, Integer>();
		
		for (String node : nodes) {
			inDegree.put(node, 0);
			adj.put(node, new ArrayList<String>());
		}
		
		for (String u : nodes) {
			if (u.toLowerCase().startsWith("archive_")) {
				continue;
			}
			if (allDeps.containsKey(u)) {
				for (String v : allDeps.get(u)) {
					if (nodes.contains(v)) {
						// Edge u -> v (Child -> Parent)
						// For archival, we want to archive Child BEFORE Parent.
						// So in topological sort, Child should come before Parent.
						// edge: Child -> Parent.
						adj.get(u).add(v);
						inDegree.put(v, inDegree.get(v) + 1);
					}
				}
			}
		}
		
		Queue<String> queue = new LinkedList<String>();
		for (String node : nodes) {
			if (inDegree.get(node) == 0) {
				queue.add(node);
			}
		}
		
		while (!queue.isEmpty()) {
			String u = queue.poll();
			result.add(u);
			
			if (adj.containsKey(u)) {
				for (String v : adj.get(u)) {
					inDegree.put(v, inDegree.get(v) - 1);
					if (inDegree.get(v) == 0) {
						queue.add(v);
					}
				}
			}
		}
		
		// If cycle detected, result size < nodes size.
		// We return result anyway (partial order).
		if (result.size() != nodes.size()) {
			log.warn("Dependency cycle detected or disconnected graph components. " + "Sort result size: " + result.size()
			        + ", expected: " + nodes.size());
			// Add remaining nodes arbitrarily?
			for (String node : nodes) {
				if (!result.contains(node)) {
					result.add(node);
				}
			}
		}
		
		return result;
	}
}
