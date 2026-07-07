package org.openmrs.module.kenyaemr.cashier;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.FileSystemResourceAccessor;
import org.junit.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CashierLegacyBillingMigrationTest {
	
	private static final String CHANGELOG = "liquibase.xml";
	
	private static final List<String> TARGET_CHANGESET_IDS = Arrays.asList(
	    "kenyaemr.cashier-001-v4.2.6-inline-price-editing-01",
	    "kenyaemr.cashier-001-v4.2.6-inline-price-editing-02",
	    "kenyaemr.cashier-001-v4.2.6-inline-price-editing-03",
	    "kenyaemr.cashier-001-v4.2.6-inline-price-editing-04",
	    "kenyaemr.cashier-001-v4.2.6-inline-price-editing-05",
	    "kenyaemr.cashier-001-v4.2.6-inline-price-editing-06",
	    "kenyaemr.cashier-001-v4.2.6-additional-discount-01",
	    "kenyaemr.cashier-001-v4.2.6-additional-discount-02");
	
	@Test
	public void liquibaseMigration_shouldUpgradeLegacyBillingSchema() throws Exception {
		try (Connection connection = DriverManager.getConnection(
		    "jdbc:h2:mem:cashier_legacy_billing_migration;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "")) {
			createLegacyBillingSchema(connection);
			int billCount = countRows(connection, "cashier_bill");
			int lineItemCount = countRows(connection, "cashier_bill_line_item");
			int paymentCount = countRows(connection, "cashier_bill_payment");
			
			assertFalse(columnExists(connection, "cashier_bill", "additional_discount"));
			assertFalse(columnExists(connection, "cashier_bill_line_item", "original_price"));
			assertFalse(columnExists(connection, "cashier_bill_line_item", "price_overridden"));
			assertFalse(columnExists(connection, "cashier_bill_line_item", "price_override_reason"));
			
			Path targetChangeLog = writeTargetChangeLog();
			Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
			Liquibase liquibase = new Liquibase(targetChangeLog.getFileName().toString(),
			        new FileSystemResourceAccessor(targetChangeLog.getParent().toFile()), database);
			liquibase.update(new Contexts(), new LabelExpression());
			for (String id : TARGET_CHANGESET_IDS) {
				assertTrue(id + " should execute against the legacy schema", changeSetRan(connection, id));
			}
			
			assertColumnSelectable(connection, "cashier_bill", "additional_discount",
			    describeColumns(connection, "cashier_bill") + "; " + describeTargetChangesets(connection));
			assertColumnSelectable(connection, "cashier_bill_line_item", "original_price");
			assertColumnSelectable(connection, "cashier_bill_line_item", "price_overridden");
			assertColumnSelectable(connection, "cashier_bill_line_item", "price_override_reason");
			
			assertEquals(billCount, countRows(connection, "cashier_bill"));
			assertEquals(lineItemCount, countRows(connection, "cashier_bill_line_item"));
			assertEquals(paymentCount, countRows(connection, "cashier_bill_payment"));
			assertAdditionalDiscountDefaults(connection);
			assertLineItemOverrideDefaults(connection);
		}
	}
	
	private void createLegacyBillingSchema(Connection connection) throws SQLException {
		execute(connection, "CREATE TABLE cashier_bill ("
		        + "bill_id INT PRIMARY KEY, "
		        + "receipt_number VARCHAR(255), "
		        + "provider_id INT NOT NULL, "
		        + "patient_id INT NOT NULL, "
		        + "cash_point_id INT NOT NULL, "
		        + "status INT NOT NULL, "
		        + "creator INT NOT NULL, "
		        + "date_created TIMESTAMP NOT NULL, "
		        + "voided BOOLEAN DEFAULT FALSE NOT NULL, "
		        + "uuid CHAR(38) NOT NULL)");
		execute(connection, "CREATE TABLE cashier_bill_line_item ("
		        + "bill_line_item_id INT PRIMARY KEY, "
		        + "bill_id INT NOT NULL, "
		        + "item_id INT, "
		        + "price DECIMAL(10,2) NOT NULL, "
		        + "price_name VARCHAR(255), "
		        + "quantity INT NOT NULL, "
		        + "line_item_order INT NOT NULL, "
		        + "creator INT NOT NULL, "
		        + "date_created TIMESTAMP NOT NULL, "
		        + "voided BOOLEAN DEFAULT FALSE NOT NULL, "
		        + "uuid CHAR(38) NOT NULL)");
		execute(connection, "CREATE TABLE cashier_bill_payment ("
		        + "bill_payment_id INT PRIMARY KEY, "
		        + "bill_id INT NOT NULL, "
		        + "amount_tendered DECIMAL(10,2) NOT NULL, "
		        + "creator INT NOT NULL, "
		        + "date_created TIMESTAMP NOT NULL, "
		        + "voided BOOLEAN DEFAULT FALSE NOT NULL, "
		        + "uuid CHAR(38) NOT NULL)");
		execute(connection, "INSERT INTO cashier_bill "
		        + "(bill_id, receipt_number, provider_id, patient_id, cash_point_id, status, creator, date_created, voided, uuid) "
		        + "VALUES (1, 'LEG-1', 1, 1, 1, 1, 1, CURRENT_TIMESTAMP, FALSE, '10000000-0000-0000-0000-000000000001')");
		execute(connection, "INSERT INTO cashier_bill_line_item "
		        + "(bill_line_item_id, bill_id, item_id, price, price_name, quantity, line_item_order, creator, date_created, voided, uuid) "
		        + "VALUES (1, 1, 1, 100.00, 'Legacy 100', 1, 0, 1, CURRENT_TIMESTAMP, FALSE, "
		        + "'10000000-0000-0000-0000-000000000101')");
		execute(connection, "INSERT INTO cashier_bill_line_item "
		        + "(bill_line_item_id, bill_id, item_id, price, price_name, quantity, line_item_order, creator, date_created, voided, uuid) "
		        + "VALUES (2, 1, 2, 150.50, 'Legacy 150', 2, 1, 1, CURRENT_TIMESTAMP, FALSE, "
		        + "'10000000-0000-0000-0000-000000000102')");
		execute(connection, "INSERT INTO cashier_bill_payment "
		        + "(bill_payment_id, bill_id, amount_tendered, creator, date_created, voided, uuid) "
		        + "VALUES (1, 1, 50.00, 1, CURRENT_TIMESTAMP, FALSE, '10000000-0000-0000-0000-000000000201')");
	}
	
	private void execute(Connection connection, String sql) throws SQLException {
		try (Statement statement = connection.createStatement()) {
			statement.execute(sql);
		}
	}
	
	private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
		        + "WHERE UPPER(TABLE_NAME) = UPPER(?) AND UPPER(COLUMN_NAME) = UPPER(?)")) {
			statement.setString(1, tableName);
			statement.setString(2, columnName);
			try (ResultSet columns = statement.executeQuery()) {
				columns.next();
				return columns.getInt(1) > 0;
			}
		}
	}

	private boolean changeSetRan(Connection connection, String id) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(
		    "SELECT COUNT(*) FROM DATABASECHANGELOG WHERE ID = ? AND EXECTYPE = 'EXECUTED'")) {
			statement.setString(1, id);
			try (ResultSet rows = statement.executeQuery()) {
				rows.next();
				return rows.getInt(1) > 0;
			}
		}
	}
	
	private void assertColumnSelectable(Connection connection, String tableName, String columnName) throws SQLException {
		assertColumnSelectable(connection, tableName, columnName, tableName + "." + columnName + " should be selectable");
	}
	
	private void assertColumnSelectable(Connection connection, String tableName, String columnName, String failureMessage)
	        throws SQLException {
		try (Statement statement = connection.createStatement()) {
			statement.executeQuery("SELECT " + columnName + " FROM " + tableName + " WHERE 1 = 0");
		} catch (SQLException e) {
			throw new SQLException(failureMessage, e);
		}
	}
	
	private String describeColumns(Connection connection, String tableName) throws SQLException {
		StringBuilder description = new StringBuilder(tableName).append(" columns:");
		try (PreparedStatement statement = connection.prepareStatement("SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS "
		        + "WHERE UPPER(TABLE_NAME) = UPPER(?) ORDER BY ORDINAL_POSITION")) {
			statement.setString(1, tableName);
			try (ResultSet columns = statement.executeQuery()) {
				while (columns.next()) {
					description.append(' ').append(columns.getString(1));
				}
			}
		}
		return description.toString();
	}
	
	private String describeTargetChangesets(Connection connection) throws SQLException {
		StringBuilder description = new StringBuilder("target changesets:");
		try (PreparedStatement statement = connection.prepareStatement(
		    "SELECT ID, EXECTYPE FROM DATABASECHANGELOG WHERE ID LIKE 'kenyaemr.cashier-001-v4.2.6-%' ORDER BY ID")) {
			try (ResultSet rows = statement.executeQuery()) {
				while (rows.next()) {
					description.append(' ').append(rows.getString(1)).append('=').append(rows.getString(2));
				}
			}
		}
		return description.toString();
	}
	
	private Path writeTargetChangeLog() throws Exception {
		String source = new String(Files.readAllBytes(Paths.get("target/classes", CHANGELOG)), StandardCharsets.UTF_8);
		int firstChangeSet = source.indexOf("<changeSet");
		int firstTargetChangeSet = source.indexOf("<changeSet id=\"" + TARGET_CHANGESET_IDS.get(0) + "\"");
		int closingTag = source.lastIndexOf("</databaseChangeLog>");
		if (firstChangeSet < 0 || firstTargetChangeSet < 0 || closingTag < 0) {
			throw new IllegalStateException("Unable to locate target Cashier migration changesets");
		}
		String targetChangeLog = source.substring(0, firstChangeSet)
		        + source.substring(firstTargetChangeSet, closingTag)
		        + "</databaseChangeLog>\n";
		Path path = Files.createTempFile(Paths.get("target"), "cashier-legacy-target-", ".xml");
		Files.write(path, targetChangeLog.getBytes(StandardCharsets.UTF_8));
		return path;
	}
	
	private int countRows(Connection connection, String tableName) throws SQLException {
		try (Statement statement = connection.createStatement();
		        ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
			rows.next();
			return rows.getInt(1);
		}
	}
	
	private void assertAdditionalDiscountDefaults(Connection connection) throws SQLException {
		try (Statement statement = connection.createStatement();
		        ResultSet rows = statement.executeQuery("SELECT additional_discount FROM cashier_bill ORDER BY bill_id")) {
			while (rows.next()) {
				assertEquals(0, BigDecimal.ZERO.compareTo(rows.getBigDecimal("additional_discount")));
			}
		}
	}
	
	private void assertLineItemOverrideDefaults(Connection connection) throws SQLException {
		try (Statement statement = connection.createStatement();
		        ResultSet rows = statement.executeQuery(
		            "SELECT price, original_price, price_overridden FROM cashier_bill_line_item ORDER BY bill_line_item_id")) {
			while (rows.next()) {
				assertEquals(0, rows.getBigDecimal("price").compareTo(rows.getBigDecimal("original_price")));
				assertFalse(rows.getBoolean("price_overridden"));
			}
		}
	}
}
