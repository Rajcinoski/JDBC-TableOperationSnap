package com.snap.iw.tableCopy;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.snap.iw.tableCopy.JdbcAccount;
import com.snap.iw.tableCopy.JdbcUtils;
import com.snaplogic.account.api.capabilities.Accounts;
import com.snaplogic.api.ConfigurationException;
import com.snaplogic.api.ExecutionException;
import com.snaplogic.api.Snap;
import com.snaplogic.common.SnapType;
import com.snaplogic.common.properties.Suggestions;
import com.snaplogic.common.properties.builders.PropertyBuilder;
import com.snaplogic.common.properties.builders.SuggestionBuilder;
import com.snaplogic.snap.api.DocumentUtility;
import com.snaplogic.snap.api.OutputViews;
import com.snaplogic.snap.api.PropertyCategory;
import com.snaplogic.snap.api.PropertyValues;
import com.snaplogic.snap.api.SnapCategory;
import com.snaplogic.snap.api.capabilities.Category;
import com.snaplogic.snap.api.capabilities.Errors;
import com.snaplogic.snap.api.capabilities.General;
import com.snaplogic.snap.api.capabilities.Inputs;
import com.snaplogic.snap.api.capabilities.Outputs;
import com.snaplogic.snap.api.capabilities.Version;
import com.snaplogic.snap.api.capabilities.ViewType;

/**
 * @author petar.rajchinoski
 *
 */

@General(title = "Generic JDBC - Table Operation", purpose = "Move or copy table from one place to another.", author = "InterWorks", docLink = "http://www.interworks.com.mk")
@Inputs(min = 0, max = 1, accepts = { ViewType.DOCUMENT })
@Outputs(min = 0, max = 1, offers = { ViewType.DOCUMENT })
@Errors(min = 1, max = 1, offers = { ViewType.DOCUMENT })
@Version(snap = 1)
@Category(snap = SnapCategory.WRITE)
@Accounts(provides = { JdbcAccount.class }, optional = false)
public class TableCopy implements Snap {

	private static final String TABLE = "Source table";

	private static final String SCHEMA = "Source schema";

	private static final String CATALOG = "Source catalog";
	
	private static final String TABLE_NEW = "Target table";

	private static final String SCHEMA_NEW = "Target schema";

	private static final String CATALOG_NEW = "Target catalog";
	
	private static final String CREATE_TABLE= "Create table if not present";
	
	private static final String DB_TYPE= "Database";	
	
	private static final String OPERATION= "Operation";	

	private final static Logger log = LoggerFactory.getLogger(TableCopy.class);

	@Inject
	private JdbcAccount jdbcAccount;

	//@Inject
	//private InputViews inputViews;
	
	@Inject
	private OutputViews outputViews;
	
	@Inject
	private DocumentUtility documentUtility;


	private String catalog;
	private String schema;
	private String table;
	
	private String new_catalog;
	private String new_schema;
	private String new_table;
	
	private boolean create_table;	
	private String database;
	private String operation;
	
	public String[] SET_VALUES = new String[] { "MS SQL", "MySQL", "PostgreSQL", "Oracle", "Redshift", "Snowflake" };
	public Set<String> allowedDBValues = new HashSet<>(Arrays.asList(SET_VALUES));
	
	public String[] OPERATION_VALUES = new String[] { "Copy", "Move" };
	public Set<String> allowedOperations = new HashSet<>(Arrays.asList(OPERATION_VALUES));

	public void setSchema(String schema) {
		this.schema = schema;
	}

	public void setTable(String table) {
		this.table = table;
	}

	@Override
	public void defineProperties(PropertyBuilder propertyBuilder) {
		log.info("Define properties start...");
		
		propertyBuilder.describe(CATALOG, "Catalog Name").type(SnapType.STRING).withSuggestions(new Suggestions() {

			@Override
			public void suggest(SuggestionBuilder suggestionBuilder, PropertyValues propertyValues) {
				String[] catalogs = getCatalogs();
				suggestionBuilder.node(CATALOG).suggestions(catalogs);
			}
		}).optional().add();
		

		propertyBuilder.describe(SCHEMA, "Schema Name").type(SnapType.STRING).withSuggestions(new Suggestions() {

			@Override
			public void suggest(SuggestionBuilder suggestionBuilder, PropertyValues propertyValues) {
				String catalog = propertyValues.get(PropertyCategory.SETTINGS, CATALOG);
				String[] schemas = getSchemas(catalog);
				suggestionBuilder.node(SCHEMA).suggestions(schemas);
			}
		}).required().add();

		propertyBuilder.describe(TABLE, "Table Name").type(SnapType.STRING).withSuggestions(new Suggestions() {
			
					@Override
					public void suggest(SuggestionBuilder suggestionBuilder, PropertyValues propertyValues) {
						String catalog = propertyValues.get(PropertyCategory.SETTINGS, CATALOG);
						String schema = propertyValues.get(PropertyCategory.SETTINGS, SCHEMA);
						String[] tables = getTables(catalog, schema);
						suggestionBuilder.node(TABLE).suggestions(tables);
					}
				}).required().add();	

		log.info("Define properties end.");
		
		log.info("Define properties for target table start...");
		
		propertyBuilder.describe(CATALOG_NEW, "Target Catalog Name").type(SnapType.STRING).withSuggestions(new Suggestions() {

			@Override
			public void suggest(SuggestionBuilder suggestionBuilder, PropertyValues propertyValues) {
				String[] catalogs = getCatalogs();
				suggestionBuilder.node(CATALOG_NEW).suggestions(catalogs);
			}
		}).optional().add();

		propertyBuilder.describe(SCHEMA_NEW, "Target Schema Name").type(SnapType.STRING).withSuggestions(new Suggestions() {

			@Override
			public void suggest(SuggestionBuilder suggestionBuilder, PropertyValues propertyValues) {
				String catalog = propertyValues.get(PropertyCategory.SETTINGS, CATALOG_NEW);
				String[] schemas = getSchemas(catalog);
				suggestionBuilder.node(SCHEMA_NEW).suggestions(schemas);
			}
		}).required().add();

		propertyBuilder.describe(TABLE_NEW, "Target Table Name").type(SnapType.STRING).withSuggestions(new Suggestions() {
			
					@Override
					public void suggest(SuggestionBuilder suggestionBuilder, PropertyValues propertyValues) {
						String catalog = propertyValues.get(PropertyCategory.SETTINGS, CATALOG_NEW);
						String schema = propertyValues.get(PropertyCategory.SETTINGS, SCHEMA_NEW);
						String[] tables = getTables(catalog, schema);
						suggestionBuilder.node(TABLE_NEW).suggestions(tables);
					}
				}).required().add();	

		log.info("Define properties for target table end.");
		
		//define Operation
		propertyBuilder.describe(OPERATION, "Operation", "Choose operation").type(SnapType.STRING).withAllowedValues(allowedOperations).required().defaultValue("Copy").add();
		
		//define Choose Database
		propertyBuilder.describe(DB_TYPE, "Database", "Choose database").type(SnapType.STRING).withAllowedValues(allowedDBValues).required().defaultValue("MS SQL").add();
		
		//define Create table if not present
		propertyBuilder.describe(CREATE_TABLE, "Create table if not present", "Create table if not present").type(SnapType.BOOLEAN)
		.defaultValue(Boolean.FALSE).add();
		
	}

	@Override
	public void configure(PropertyValues propertyValues) throws ConfigurationException {
		log.info("Configure start...");
		catalog = propertyValues.get(CATALOG);
		schema = propertyValues.get(SCHEMA);
		table = propertyValues.get(TABLE);

		new_catalog = propertyValues.get(CATALOG_NEW);
		new_schema = propertyValues.get(SCHEMA_NEW);
		new_table = propertyValues.get(TABLE_NEW);
		
		create_table = propertyValues.get(CREATE_TABLE);
		
		database = propertyValues.get(DB_TYPE);
		
		operation = propertyValues.get(OPERATION);

		Connection connection = null;
		ResultSet resultSet = null;

		try {
			connection = jdbcAccount.connect();
		} catch (ConfigurationException ce) {
			throw ce;
		} catch (Throwable t) {
			throw new ConfigurationException(t.getMessage());
		} finally {
			JdbcUtils.close(resultSet);
			JdbcUtils.close(connection);
		}
		log.info("Configure end.");
	}

	@SuppressWarnings("unchecked")
	@Override
	public void execute() throws ExecutionException {
		log.info("Execute start...");
		Connection connection = null;
		
		try {
			connection = jdbcAccount.connect();
			switch(operation){
				case "Copy":
					processExecute(connection);
					break;
				case "Move":
					processExecute(connection);
					processDropTable(connection);
					break;
			}
									
			//Write output
			Map<String, String> data = new LinkedHashMap<String, String>() {{
                put("Status", "Success");
                }};
            outputViews.write(documentUtility.newDocument(data));
			
		} catch (ExecutionException ee) {
			throw ee;
		} catch (Throwable t) {
			throw new ExecutionException(t.getMessage());
		} finally {
			JdbcUtils.close(connection);
		}
		log.info("Execute end.");
	}

	@Override
	public void cleanup() throws ExecutionException {

	}
			
	private String getDropStatementString() {
		StringBuilder dropQuery = new StringBuilder();
		dropQuery.append("DROP TABLE ");
		dropQuery.append(schema).append(".");
		dropQuery.append(table);
		
		return dropQuery.toString();
	}
	
	private String getCopyStatementString() {
		StringBuilder copyQuery = new StringBuilder();
		copyQuery.append("INSERT INTO ");
		if (new_schema != null)
		copyQuery.append(new_schema).append(".");
		copyQuery.append(new_table);
		copyQuery.append(" SELECT * FROM ");
		copyQuery.append(schema).append(".");
		copyQuery.append(table);
		
		return copyQuery.toString();
	}

	private String getCreateTableStatementString() {
		StringBuilder copyQuery = new StringBuilder();
		copyQuery.append("CREATE TABLE ");
		if (new_schema != null)
		copyQuery.append(new_schema).append(".");
		copyQuery.append(new_table);
		copyQuery.append(" SELECT * FROM ");
		copyQuery.append(schema).append(".");
		copyQuery.append(table);
		copyQuery.append(" WHERE 1=2 ");
		
		return copyQuery.toString();
	}
	
	private String getCreateMSSQLTableStatementString() {
		StringBuilder copyQuery = new StringBuilder();
		copyQuery.append("SELECT * INTO ");
		if (new_schema != null)
		copyQuery.append(new_schema).append(".");
		copyQuery.append(new_table);
		copyQuery.append(" FROM ");
		copyQuery.append(schema).append(".");
		copyQuery.append(table);
		copyQuery.append(" WHERE 1=2 ");
		
		return copyQuery.toString();
	}
	
	private void processCopyTableData(Connection connection)
	{
		PreparedStatement copyTableStatement = null;
	
		try {
			if (connection.getAutoCommit())
				connection.setAutoCommit(false);

			String copyStatementString = getCopyStatementString();
			copyTableStatement = connection.prepareStatement(copyStatementString);
			copyTableStatement.execute();
			connection.commit();
		}
		catch (Throwable t) {
			try {
				connection.rollback();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			throw new ExecutionException(t.getMessage());
		}
		finally {
			JdbcUtils.close(copyTableStatement);
		}
	}

	private void processCreateTable(Connection connection)
	{
		PreparedStatement createTableStatement = null;
		
		try {
			if (connection.getAutoCommit())
				connection.setAutoCommit(false);			

			switch(database) {
			case "MS SQL":
				String createTableMSSQLStatementString = getCreateMSSQLTableStatementString();
				createTableStatement = connection.prepareStatement(createTableMSSQLStatementString);
				createTableStatement.execute();		
				break;
			default:
				String createTableStatementString = getCreateTableStatementString();
				createTableStatement = connection.prepareStatement(createTableStatementString);
				createTableStatement.execute();	
			}
		
		}
		catch (Throwable t) {
			try {
				connection.rollback();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			throw new ExecutionException(t.getMessage());
		}
		finally {
			JdbcUtils.close(createTableStatement);
		}
	}

	private void processDropTable(Connection connection)
	{
		PreparedStatement dropTableStatement = null;
		
		try {
			if (connection.getAutoCommit())
				connection.setAutoCommit(false);			

			String dropTableStatementString = getDropStatementString();
			dropTableStatement = connection.prepareStatement(dropTableStatementString);
			dropTableStatement.execute();		
			connection.commit();
		}
		catch (Throwable t) {
			try {
				connection.rollback();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			throw new ExecutionException(t.getMessage());
		}
		finally {
			JdbcUtils.close(dropTableStatement);
		}
	}
	
	private void processExecute(Connection connection) {

		try {
			// if create table is checked
			if (create_table) {
				DatabaseMetaData dbm = connection.getMetaData();
				// check if the table is there
				ResultSet tables = dbm.getTables(new_catalog, new_schema, new_table, null);
				if (tables.next()) {
					// Table exists
					//call the process for copying data 
					processCopyTableData(connection);		
				}
				else {
					// Table does not exist
					processCreateTable(connection);	
					processCopyTableData(connection);
				}
			}
			else
			{				
				processCopyTableData(connection);
			}
			
		}
		catch (ExecutionException ee) {
			throw ee;
		} catch (Throwable t) {
			throw new ExecutionException(t.getMessage());
		} 					
	}
	
	private String[] getCatalogs() {
		try {
			Connection connection = jdbcAccount.connect();
			return JdbcUtils.getCatalogs(connection);
		} catch (Throwable t) {
			throw new ConfigurationException(t.getMessage());
		}
	}

	private String[] getSchemas(String catalog) {
		try {
			Connection connection = jdbcAccount.connect();
			return JdbcUtils.getSchemas(connection, catalog);
		} catch (Throwable t) {
			throw new ConfigurationException(t.getMessage());
		}
	}

	private String[] getTables(String catalog, String schema) {
		try {
			Connection connection = jdbcAccount.connect();
			return JdbcUtils.getTables(connection, catalog, schema);
		} catch (Throwable t) {
			throw new ConfigurationException(t.getMessage());
		}
	}

}
