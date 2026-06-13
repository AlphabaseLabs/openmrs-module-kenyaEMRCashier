/*
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.1 (the "License"); you may not use this file except in
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
package org.openmrs.module.kenyaemr.cashier.api.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.security.AccessControlException;
import java.util.Collection;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.OpenmrsObject;
import org.openmrs.User;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.db.hibernate.BaseHibernateRepository;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.BillStatus;
import org.openmrs.module.kenyaemr.cashier.api.util.PrivilegeConstants;

public class BillServiceImplVoidTest {
	private TestableBillServiceImpl service;
	private BaseHibernateRepository repository;
	private User user;

	@Before
	public void before() {
		service = new TestableBillServiceImpl();
		repository = new FakeRepository();
		service.setRepository(repository);
		user = new User();
		service.user = user;
	}

	@Test(expected = AccessControlException.class)
	public void voidEntity_shouldRejectPaidBillWithoutForceDeletePrivilege() {
		withPrivileges(true, false);

		service.voidEntity(createBill(BillStatus.PAID), "entered in error");
	}

	@Test
	public void voidEntity_shouldVoidPaidBillWithForceDeletePrivilege() {
		withPrivileges(false, true);
		Bill bill = createBill(BillStatus.PAID);

		Bill result = service.voidEntity(bill, "administrator correction");

		assertSame(bill, result);
		assertVoided(bill, "administrator correction");
		assertEquals(1, ((FakeRepository)repository).saveCount);
	}

	@Test
	public void voidEntity_shouldVoidNonPaidBillWithManageBillsPrivilege() {
		withPrivileges(true, false);
		Bill bill = createBill(BillStatus.POSTED);

		Bill result = service.voidEntity(bill, "entered in error");

		assertSame(bill, result);
		assertVoided(bill, "entered in error");
		assertEquals(1, ((FakeRepository)repository).saveCount);
	}

	private void withPrivileges(boolean manageBills, boolean forceDeleteBills) {
		service.manageBills = manageBills;
		service.forceDeleteBills = forceDeleteBills;
	}

	private Bill createBill(BillStatus status) {
		Bill bill = new Bill();
		bill.setStatus(status);
		bill.setVoided(false);
		return bill;
	}

	private void assertVoided(Bill bill, String reason) {
		assertTrue(bill.getVoided());
		assertEquals(reason, bill.getVoidReason());
		assertSame(user, bill.getVoidedBy());
		assertNotNull(bill.getDateVoided());
	}

	private static class TestableBillServiceImpl extends BillServiceImpl {
		private boolean manageBills;
		private boolean forceDeleteBills;
		private User user;

		@Override
		protected boolean hasBillDeletePrivilege(String privilege) {
			if (PrivilegeConstants.MANAGE_BILLS.equals(privilege)) {
				return manageBills;
			}
			if (PrivilegeConstants.FORCE_DELETE_BILLS.equals(privilege)) {
				return forceDeleteBills;
			}
			return false;
		}

		@Override
		protected User getAuthenticatedUser() {
			return user;
		}
	}

	private static class FakeRepository implements BaseHibernateRepository {
		private int saveCount;

		@Override
		public Query createQuery(String query) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <E extends OpenmrsObject> Criteria createCriteria(Class<E> cls) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <E extends OpenmrsObject> E save(E entity) {
			saveCount++;
			return entity;
		}

		@Override
		public void saveAll(Collection<? extends OpenmrsObject> collection) {
			saveCount += collection == null ? 0 : collection.size();
		}

		@Override
		public <E extends OpenmrsObject> void delete(E entity) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T> T selectValue(Criteria criteria) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T> T selectValue(Query query) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <E extends OpenmrsObject> E selectSingle(Class<E> cls, Serializable id) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <E extends OpenmrsObject> E selectSingle(Class<E> cls, Criteria criteria) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <E extends OpenmrsObject> List<E> select(Class<E> cls) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <E extends OpenmrsObject> List<E> select(Class<E> cls, Criteria criteria) {
			throw new UnsupportedOperationException();
		}
	}
}
