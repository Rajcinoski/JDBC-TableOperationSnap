package com.snap.iw.tableCopy;

import java.math.BigInteger;
import java.sql.Connection;

import javax.sql.DataSource;

import com.snaplogic.account.api.Account;
import com.snaplogic.account.api.AccountType;
import com.snaplogic.account.api.ValidatableAccount;
import com.snaplogic.account.api.capabilities.AccountCategory;
import com.snaplogic.api.ExecutionException;
import com.snaplogic.common.SnapType;
import com.snaplogic.common.properties.SnapProperty;
import com.snaplogic.common.properties.builders.PropertyBuilder;
import com.snaplogic.snap.api.PropertyValues;
import com.snaplogic.snap.api.capabilities.General;
import com.snaplogic.snap.api.capabilities.Version;

@General(title = "InterWorks Generic JDBC Account")
@Version(snap = 1)
@AccountCategory(type = AccountType.DATABASE)
public class JdbcAccount implements Account<Connection>, ValidatableAccount<Connection> {

	private static final String AUTO_COMMIT = "autoCommit";
	private static final String VALIDATE_QUERY = "validateQuery";
	private static final String MAX_TOTAL = "maxTotal";
	private static final String MAX_IDDLE = "maxIddle";
	private static final String MIN_IDDLE = "minIddle";
	private static final String PASSWORD = "password";
	private static final String USERNAME = "username";
	private static final String URL = "url";
	private static final String DRIVER_CLASS_NAME = "driverClassName";
	private static final String DRIVER_JAR = "driverJar";

	// private static final Logger log =
	// LoggerFactory.getLogger(JdbcAccount.class);

	private DataSourceConfiguration dataSourceConfiguration;

	private static DataSourceRegistry dataSourceRegistry = new DataSourceRegistry();

	public JdbcAccount() {
		// log.info("Constructor start...");
		// log.info("Constructor end.");
	}

	@Override
	public void defineProperties(PropertyBuilder propertyBuilder) {
		// log.info("Defining properties...");
		propertyBuilder.describe(DRIVER_JAR, "Driver Jar File", "Driver Jar File").required().driverBrowsing().add();
		propertyBuilder.describe(DRIVER_CLASS_NAME, "Driver Class Name", "Driver Class Name").required().add();
		propertyBuilder.describe(URL, "Database URL", "Database URL").required().add();
		propertyBuilder.describe(USERNAME, "Username", "Database username").required()
				.sensitivity(SnapProperty.SensitivityLevel.MEDIUM).add();
		propertyBuilder.describe(PASSWORD, "Password", "Database username's password").required().obfuscate().add();
		propertyBuilder.describe(MIN_IDDLE, "Minimum Iddle Connections", "Minimum Iddle Connections")
				.type(SnapType.INTEGER).required().defaultValue(1).add();
		propertyBuilder.describe(MAX_IDDLE, "Maximum Iddle Connections", "Maximum Iddle Connections")
				.type(SnapType.INTEGER).required().defaultValue(3).add();
		propertyBuilder.describe(MAX_TOTAL, "Maximum Total Connections", "Maximum Total Connections")
				.type(SnapType.INTEGER).required().defaultValue(5).add();
		propertyBuilder.describe(VALIDATE_QUERY, "Connection Validation Query", "Connection Validation Query")
				.required().defaultValue("SELECT 1").add();
		propertyBuilder.describe(AUTO_COMMIT, "Default Auto Commit", "Default Auto Commit").type(SnapType.BOOLEAN)
				.defaultValue(Boolean.TRUE).add();
		// log.info("Properties defined.");
	}

	@Override
	public void configure(PropertyValues propertyValues) {
		// log.info("Configuring...");
		dataSourceConfiguration = new DataSourceConfiguration();
		dataSourceConfiguration.setDriverJar(propertyValues.get(DRIVER_JAR));
		dataSourceConfiguration.setDriverClassName(propertyValues.get(DRIVER_CLASS_NAME));
		dataSourceConfiguration.setUrl(propertyValues.get(URL));
		dataSourceConfiguration.setUsername(propertyValues.get(USERNAME));
		dataSourceConfiguration.setPassword(propertyValues.get(PASSWORD));
		dataSourceConfiguration.setMinIddle(((BigInteger) propertyValues.get(MIN_IDDLE)).intValue());
		dataSourceConfiguration.setMaxIddle(((BigInteger) propertyValues.get(MAX_IDDLE)).intValue());
		dataSourceConfiguration.setMaxTotal(((BigInteger) propertyValues.get(MAX_TOTAL)).intValue());
		dataSourceConfiguration.setValidationQuery(propertyValues.get(VALIDATE_QUERY));
		dataSourceConfiguration.setDefaultAutoCommit(((Boolean) propertyValues.get(AUTO_COMMIT)));
		// log.info("Configured.");
	}

	@Override
	public Connection connect() throws ExecutionException {
		// log.info("Connect start...");
		try {
			DataSource dataSource = dataSourceRegistry.getDataSource(dataSourceConfiguration);
			Connection connection = dataSource.getConnection();
			return connection;
			// log.info("Connect end.");
		} catch (Throwable t) {
			// log.error("Error", t);
			t.printStackTrace();
			throw new ExecutionException(t.getMessage());
		}
	}

	@Override
	public void disconnect() throws ExecutionException {
		// Do nothing, the Snaps that use the connection shall close the
		// connections
	}

}
