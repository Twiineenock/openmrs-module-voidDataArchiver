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
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.openmrs.Voidable;
import org.openmrs.module.voiddataarchiver.Item;
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
	public List<String> getVoidedTableNames() {
		List<String> voidedTables = new ArrayList<String>();
		
		try {
			// Use JPA Metamodel to retrieve entities (replacing deprecated
			// getAllClassMetadata)
			// Need to cast to EntityManagerFactory to access Metamodel in some Hibernate
			// versions/configs
			javax.persistence.EntityManagerFactory emf = (javax.persistence.EntityManagerFactory) sessionFactory;
			for (javax.persistence.metamodel.EntityType<?> entity : emf.getMetamodel().getEntities()) {
				if (entity.getJavaType() == null) {
					continue;
				}
				
				Class<?> mappedClass = entity.getJavaType();
				String entityName = entity.getName();
				
				// Check if the class implements Voidable or has a voided property
				if (Voidable.class.isAssignableFrom(mappedClass)) {
					// It's a Voidable OpenMRS object
					
					// Create a simple query to check if there are any voided rows
					String hql = "select count(*) from " + entityName + " where voided = true";
					try {
						Long count = (Long) sessionFactory.getCurrentSession().createQuery(hql).uniqueResult();
						if (count != null && count > 0) {
							voidedTables.add(mappedClass.getSimpleName());
						}
					}
					catch (Exception e) {
						log.warn("Error checking for voided data in " + entityName, e);
					}
				}
			}
		}
		catch (Exception e) {
			log.error("Error retrieving voided tables using Metamodel", e);
		}
		
		return voidedTables;
	}
	
	@Override
	public Item getItemByUuid(String uuid) {
		return (Item) sessionFactory.getCurrentSession().createCriteria(Item.class).add(Restrictions.eq("uuid", uuid))
		        .uniqueResult();
	}
	
	@Override
	public Item saveItem(Item item) {
		sessionFactory.getCurrentSession().saveOrUpdate(item);
		return item;
	}
}
