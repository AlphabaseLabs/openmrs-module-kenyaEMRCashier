package org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.layout;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.openmrs.api.context.Context;
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
