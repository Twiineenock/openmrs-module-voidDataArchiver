package org.openmrs.module.voiddataarchiver;

/**
 * Holds information about a database table/entity.
 */
public class TableInfo {
	
	private String tableName;
	
	private boolean voidable;
	
	private Long totalRecords;
	
	private Long voidedRecords;
	
	public TableInfo() {
	}
	
	public TableInfo(String tableName, boolean voidable) {
		this.tableName = tableName;
		this.voidable = voidable;
	}
	
	public String getTableName() {
		return tableName;
	}
	
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	
	public boolean isVoidable() {
		return voidable;
	}
	
	public void setVoidable(boolean voidable) {
		this.voidable = voidable;
	}
	
	public Long getTotalRecords() {
		return totalRecords;
	}
	
	public void setTotalRecords(Long totalRecords) {
		this.totalRecords = totalRecords;
	}
	
	public Long getVoidedRecords() {
		return voidedRecords;
	}
	
	public void setVoidedRecords(Long voidedRecords) {
		this.voidedRecords = voidedRecords;
	}
	
	private java.util.List<java.util.Map<String, Object>> voidedEntries;
	
	public java.util.List<java.util.Map<String, Object>> getVoidedEntries() {
		return voidedEntries;
	}
	
	public void setVoidedEntries(java.util.List<java.util.Map<String, Object>> voidedEntries) {
		this.voidedEntries = voidedEntries;
	}
	
	private String prettyName;
	
	public String getPrettyName() {
		return prettyName;
	}
	
	public void setPrettyName(String prettyName) {
		this.prettyName = prettyName;
	}
}
