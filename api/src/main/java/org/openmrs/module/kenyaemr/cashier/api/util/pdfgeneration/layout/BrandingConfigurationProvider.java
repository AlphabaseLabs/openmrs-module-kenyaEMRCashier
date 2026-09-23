package org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.layout;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.layout.BillingPatientInformation.Field;
import org.openmrs.util.PrivilegeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Reads shared document branding settings managed by the Alphabase admin module. */
public final class BrandingConfigurationProvider {

	static final String GP_BRANDING_CONFIGURATION = "alphabaseadmin.configurations.branding";

	private static final boolean DEFAULT_SHOW_BILLING_NOTE = true;

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private static final Logger LOG = LoggerFactory.getLogger(BrandingConfigurationProvider.class);

	private BrandingConfigurationProvider() {
	}

	public static boolean shouldShowBillingNote() {
		try {
			return resolveShowBillingNote(readConfiguration());
		}
		catch (Exception e) {
			LOG.warn("Failed to read billing print settings from the shared branding configuration", e);
			return DEFAULT_SHOW_BILLING_NOTE;
		}
	}

	public static Set<Field> getInvoicePatientFields() {
		try {
			return resolveInvoicePatientFields(readConfiguration());
		}
		catch (Exception e) {
			LOG.warn("Failed to read invoice patient information settings", e);
			return defaultInvoicePatientFields();
		}
	}

	public static Set<Field> getBillStatementPatientFields() {
		try {
			return resolveBillStatementPatientFields(readConfiguration());
		}
		catch (Exception e) {
			LOG.warn("Failed to read bill statement patient information settings", e);
			return EnumSet.allOf(Field.class);
		}
	}

	static Set<Field> resolveInvoicePatientFields(String json) throws IOException {
		return resolveInvoicePatientFields(parseObject(json));
	}

	static Set<Field> resolveBillStatementPatientFields(String json) throws IOException {
		JsonNode root = parseObject(json);
		JsonNode applySelection = root == null ? null : root.get("applyPatientInformationToBillStatement");
		if (applySelection != null && applySelection.isBoolean() && applySelection.asBoolean()) {
			return resolveInvoicePatientFields(root);
		}
		return EnumSet.allOf(Field.class);
	}

	private static Set<Field> resolveInvoicePatientFields(JsonNode root) {
		Set<Field> fields = defaultInvoicePatientFields();
		if (root == null) {
			return fields;
		}
		JsonNode configured = root.path("billingPatientInformation");
		for (Field field : Field.values()) {
			JsonNode enabled = configured.path(field.getKey());
			if (!enabled.isBoolean()) {
				continue;
			}
			if (enabled.asBoolean()) {
				fields.add(field);
			}
			else {
				fields.remove(field);
			}
		}
		return fields;
	}

	private static Set<Field> defaultInvoicePatientFields() {
		return EnumSet.of(Field.NAME, Field.MR_NUMBER, Field.PHONE, Field.ADDRESS);
	}

	static String readConfiguration() {
		try {
			Context.addProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
			return Context.getAdministrationService().getGlobalProperty(GP_BRANDING_CONFIGURATION);
		}
		finally {
			Context.removeProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
		}
	}

	static boolean resolveShowBillingNote(String json) throws IOException {
		JsonNode root = parseObject(json);
		if (root == null) {
			return DEFAULT_SHOW_BILLING_NOTE;
		}
		JsonNode showNote = root.get("showBillingNote");
		return showNote != null && showNote.isBoolean() ? showNote.asBoolean() : DEFAULT_SHOW_BILLING_NOTE;
	}

	static JsonNode parseObject(String json) throws IOException {
		if (StringUtils.isBlank(json)) {
			return null;
		}
		JsonNode root = OBJECT_MAPPER.readTree(json);
		if (root == null || !root.isObject()) {
			throw new IOException("Branding configuration must be a JSON object");
		}
		return root;
	}
}
