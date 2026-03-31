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
package org.openmrs.module.kenyaemr.cashier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.GlobalProperty;
import org.openmrs.api.context.Context;
import org.openmrs.module.BaseModuleActivator;
import org.openmrs.module.Module;
import org.openmrs.module.ModuleFactory;
import org.openmrs.module.kenyaemr.cashier.api.tasks.AutoClosePaidBillsTask;
import org.openmrs.module.kenyaemr.cashier.exemptions.SampleBillingExemptionBuilder;
import org.openmrs.module.kenyaemr.cashier.web.CashierWebConstants;
import org.openmrs.scheduler.SchedulerException;
import org.openmrs.scheduler.SchedulerService;
import org.openmrs.scheduler.TaskDefinition;
import org.openmrs.module.web.WebModuleUtil;

import java.util.Calendar;
import java.util.Date;

/**
 * This class contains the logic that is run every time this module is either started or stopped.
 */
public class CashierModuleActivator extends BaseModuleActivator {
	private static final Log LOG = LogFactory.getLog(CashierModuleActivator.class);
	private static final String AUTO_CLOSE_PAID_BILLS_TASK_NAME = "Auto Close Paid Bills Task";
	private static final String AUTO_CLOSE_PAID_BILLS_TASK_UUID = "8e3f0d6b-2746-4c79-8df4-2a0dc2fd0e44";
	private static final String DEFAULT_START_TIME_PATTERN = "MM/dd/yyyy HH:mm:ss";

	/**
	 * @see BaseModuleActivator#contextRefreshed()
	 */
	@Override
	public void contextRefreshed() {
		LOG.info("OpenHMIS Cashier Module Module refreshed");
	}

	/**
	 * @see BaseModuleActivator#started()
	 */
	@Override
	public void started() {
		LOG.info("OpenHMIS Cashier Module Module started");
		SampleBillingExemptionBuilder exemptionListBuilder = new SampleBillingExemptionBuilder();
		exemptionListBuilder.buildBillingExemptionList();
		configureAutoClosePaidBillsTask();
	}

	/**
	 * @see BaseModuleActivator#stopped()
	 */
	@Override
	public void stopped() {
		Module module = ModuleFactory.getModuleById(CashierWebConstants.OPENHMIS_CASHIER_MODULE_ID);
		WebModuleUtil.unloadFilters(module);

		LOG.info("OpenHMIS Cashier Module Module stopped");
	}

	private void configureAutoClosePaidBillsTask() {
		ensureGlobalProperty(ModuleSettings.AUTO_CLOSE_PAID_BILLS_ENABLED_PROPERTY,
		        ModuleSettings.DEFAULT_AUTO_CLOSE_PAID_BILLS_ENABLED.toString());
		ensureGlobalProperty(ModuleSettings.AUTO_CLOSE_PAID_BILLS_REASON_PROPERTY,
		        ModuleSettings.DEFAULT_AUTO_CLOSE_PAID_BILLS_REASON);
		ensureGlobalProperty(ModuleSettings.AUTO_CLOSE_PAID_BILLS_REPEAT_INTERVAL_SECONDS_PROPERTY,
		        ModuleSettings.DEFAULT_AUTO_CLOSE_PAID_BILLS_REPEAT_INTERVAL_SECONDS.toString());

		SchedulerService schedulerService = Context.getSchedulerService();
		TaskDefinition task = schedulerService.getTaskByName(AUTO_CLOSE_PAID_BILLS_TASK_NAME);
		boolean enabled = ModuleSettings.isAutoClosePaidBillsEnabled();
		Long repeatIntervalSeconds = ModuleSettings.getAutoClosePaidBillsRepeatIntervalSeconds();

		if (task == null) {
			task = new TaskDefinition();
			task.setName(AUTO_CLOSE_PAID_BILLS_TASK_NAME);
			task.setUuid(AUTO_CLOSE_PAID_BILLS_TASK_UUID);
		}

		task.setDescription("Automatically closes today's paid bills that are still open");
		task.setTaskClass(AutoClosePaidBillsTask.class.getName());
		task.setStartTime(nextMidnight());
		task.setStartTimePattern(DEFAULT_START_TIME_PATTERN);
		task.setRepeatInterval(repeatIntervalSeconds);
		task.setStartOnStartup(enabled);
		task.setStarted(enabled);

		schedulerService.saveTaskDefinition(task);

		try {
			schedulerService.shutdownTask(task);
		} catch (Exception e) {
			LOG.debug("Auto-close paid bills task was not running during startup refresh.");
		}

		if (enabled) {
			try {
				schedulerService.scheduleTask(task);
				LOG.info("Configured scheduler task: " + AUTO_CLOSE_PAID_BILLS_TASK_NAME);
			} catch (SchedulerException e) {
				LOG.error("Failed to schedule auto close paid bills task", e);
			}
		} else {
			LOG.info("Auto close paid bills task is disabled by global property.");
		}
	}

	private void ensureGlobalProperty(String propertyName, String defaultValue) {
		String currentValue = Context.getAdministrationService().getGlobalProperty(propertyName);
		if (currentValue == null || currentValue.trim().isEmpty()) {
			Context.getAdministrationService().saveGlobalProperty(new GlobalProperty(propertyName, defaultValue));
		}
	}

	private Date nextMidnight() {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DAY_OF_MONTH, 1);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar.getTime();
	}
}
