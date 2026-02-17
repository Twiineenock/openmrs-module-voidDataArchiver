/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.voiddataarchiver.web.controller;

import java.util.List;

import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.voiddataarchiver.api.TableInfo;
import org.openmrs.module.voiddataarchiver.api.VoidDataArchiverService;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * This class configured as controller using annotation and mapped with the URL of
 * 'module/voiddataarchiver/voiddataarchiverLink.form'.
 */
@Controller("${rootrootArtifactId}.VoidDataArchiverController")
@RequestMapping(value = "module/voiddataarchiver/voiddataarchiver.form")
public class VoidDataArchiverController {
	
	/** Logger for this class and subclasses */
	protected final Log log = LogFactory.getLog(getClass());
	
	/** Success form view name */
	private final String VIEW = "/module/voiddataarchiver/voiddataarchiver";
	
	/** Redirect URL for POST-Redirect-GET pattern */
	private final String REDIRECT = "redirect:/module/voiddataarchiver/voiddataarchiver.form";
	
	/**
	 * Initially called after the getUsers method to get the landing form name
	 * 
	 * @return String form view name
	 */
	@RequestMapping(method = RequestMethod.GET)
	public String onGet() {
		return VIEW;
	}
	
	/**
	 * All the parameters are optional based on the necessity
	 * 
	 * @param httpSession
	 * @param anyRequestObject
	 * @param errors
	 * @return
	 */
	@RequestMapping(method = RequestMethod.POST)
	public String onPost(HttpSession httpSession, @ModelAttribute("anyRequestObject") Object anyRequestObject,
	        BindingResult errors) {
		
		if (errors.hasErrors()) {
			// return error view
		}
		
		return REDIRECT;
	}
	
	@RequestMapping(method = RequestMethod.POST, params = "archive")
	public String archive(@org.springframework.web.bind.annotation.RequestParam("archive") String tableName,
	        HttpSession httpSession) {
		try {
			Context.getService(VoidDataArchiverService.class).runArchival(tableName);
			httpSession.setAttribute(org.openmrs.web.WebConstants.OPENMRS_MSG_ATTR, "Archived table: " + tableName);
		}
		catch (Exception e) {
			log.error("Failed to archive table: " + tableName, e);
			httpSession.setAttribute(org.openmrs.web.WebConstants.OPENMRS_ERROR_ATTR, "Failed to archive table " + tableName
			        + ": " + e.getMessage());
		}
		return REDIRECT;
	}
	
	@RequestMapping(method = RequestMethod.POST, params = "restore")
	public String restore(@org.springframework.web.bind.annotation.RequestParam("restore") String tableName,
	        HttpSession httpSession) {
		try {
			Context.getService(VoidDataArchiverService.class).restoreTable(tableName);
			httpSession.setAttribute(org.openmrs.web.WebConstants.OPENMRS_MSG_ATTR, "Restored table: " + tableName);
		}
		catch (Exception e) {
			log.error("Failed to restore table: " + tableName, e);
			httpSession.setAttribute(org.openmrs.web.WebConstants.OPENMRS_ERROR_ATTR, "Failed to restore table " + tableName
			        + ": " + e.getMessage());
		}
		return REDIRECT;
	}
	
	@RequestMapping(method = RequestMethod.POST, params = "dropArchive")
	public String dropArchive(@org.springframework.web.bind.annotation.RequestParam("dropArchive") String tableName,
	        HttpSession httpSession) {
		try {
			Context.getService(VoidDataArchiverService.class).dropArchiveTable(tableName);
			httpSession.setAttribute(org.openmrs.web.WebConstants.OPENMRS_MSG_ATTR, "Dropped archive table: " + tableName);
		}
		catch (Exception e) {
			log.error("Failed to drop archive table: " + tableName, e);
			httpSession.setAttribute(org.openmrs.web.WebConstants.OPENMRS_ERROR_ATTR, "Failed to drop archive table "
			        + tableName + ": " + e.getMessage());
		}
		return REDIRECT;
	}
	
	@ModelAttribute("archivedTables")
	protected List<TableInfo> getArchivedTables() throws Exception {
		return Context.getService(VoidDataArchiverService.class).getArchivedTables();
	}
	
	@ModelAttribute("dependencyGraph")
	protected java.util.Map<String, List<String>> getDependencyGraph() throws Exception {
		return Context.getService(VoidDataArchiverService.class).getTableDependencies();
	}
	
	@ModelAttribute("nonVoidableTables")
	protected List<TableInfo> getNonVoidableTables() throws Exception {
		return getFilteredTables(false, null);
	}
	
	@ModelAttribute("voidableCleanTables")
	protected List<TableInfo> getVoidableCleanTables() throws Exception {
		return getFilteredTables(true, false);
	}
	
	@ModelAttribute("voidableDataTables")
	protected List<TableInfo> getVoidableDataTables() throws Exception {
		return getFilteredTables(true, true);
	}
	
	@ModelAttribute("allVoidableTables")
	protected List<TableInfo> getAllVoidableTables() throws Exception {
		return getFilteredTables(true, null);
	}
	
	private List<TableInfo> getFilteredTables(boolean isVoidable, Boolean hasVoidedData) {
		List<TableInfo> allTables = Context.getService(VoidDataArchiverService.class).getAllTableInfo();
		List<TableInfo> filtered = new java.util.ArrayList<TableInfo>();
		
		for (TableInfo info : allTables) {
			if (info.isVoidable() == isVoidable) {
				if (hasVoidedData == null) {
					filtered.add(info);
				} else {
					boolean hasData = info.getVoidedRecords() != null && info.getVoidedRecords() > 0;
					if (hasVoidedData == hasData) {
						filtered.add(info);
					}
				}
			}
		}
		return filtered;
	}
	
	@ModelAttribute("visGraphData")
	public String getVisGraphData() throws Exception {
		// 1. Get tables that actually have voided data
		List<TableInfo> voidedTables = getFilteredTables(true, true);
		java.util.Set<String> activeTableNames = new java.util.HashSet<String>();
		for (TableInfo t : voidedTables) {
			activeTableNames.add(t.getTableName());
		}
		
		// 2. Get all dependencies
		java.util.Map<String, List<String>> allDeps = Context.getService(VoidDataArchiverService.class)
		        .getTableDependencies();
		
		// 3. Build Nodes JSON
		StringBuilder nodes = new StringBuilder("[");
		boolean firstNode = true;
		for (TableInfo t : voidedTables) {
			if (!firstNode)
				nodes.append(",");
			// Escaping might be needed for names with special chars, but table names are
			// usually safe
			nodes.append(String.format("{id: '%s', label: '%s\\n(%d rows)', shape: 'box'}", t.getTableName(),
			    t.getPrettyName(), t.getVoidedRecords()));
			firstNode = false;
		}
		nodes.append("]");
		
		// 4. Build Edges JSON (Only show edges where BOTH ends are in the active set)
		StringBuilder edges = new StringBuilder("[");
		boolean firstEdge = true;
		for (java.util.Map.Entry<String, List<String>> entry : allDeps.entrySet()) {
			String child = entry.getKey();
			if (activeTableNames.contains(child)) {
				for (String parent : entry.getValue()) {
					if (activeTableNames.contains(parent)) {
						if (!firstEdge)
							edges.append(",");
						// Child depends on Parent (arrow from Child to Parent)
						edges.append(String.format("{from: '%s', to: '%s', arrows: 'to'}", child, parent));
						firstEdge = false;
					}
				}
			}
		}
		edges.append("]");
		
		return "{nodes: " + nodes.toString() + ", edges: " + edges.toString() + "}";
	}
	
}
