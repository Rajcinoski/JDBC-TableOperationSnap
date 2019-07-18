package com.snap.iw.tableCopy;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.dbcp2.BasicDataSource;

/**
 * @author marjan.sterjev
 *
 */
public class JdbcUtils {

	public static void close(Connection connection) {
		if (connection != null) {
			try {
				connection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public static void close(PreparedStatement statement) {
		if (statement != null) {
			try {
				statement.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public static void close(ResultSet resultSet) {
		if (resultSet != null) {
			try {
				resultSet.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public static void close(BasicDataSource dataSource) {
		if (dataSource != null) {
			try {
				dataSource.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public static String[] getCatalogs(Connection connection) {
		ResultSet resultSet = null;
		List<String> catalogs = new ArrayList<String>();
		try {
			DatabaseMetaData databaseMetaData = connection.getMetaData();
			resultSet = databaseMetaData.getCatalogs();
			while (resultSet.next()) {
				String catalog = resultSet.getString("TABLE_CAT");
				if (catalog != null)
					catalogs.add(catalog);
			}
			return catalogs.toArray(new String[0]);
		} catch (Throwable t) {
			throw new RuntimeException(t.getMessage());
		} finally {
			JdbcUtils.close(resultSet);
			JdbcUtils.close(connection);
		}
	}

	public static String[] getSchemas(Connection connection, String catalog) {
		ResultSet resultSet = null;
		List<String> schemas = new ArrayList<String>();
		try {
			DatabaseMetaData databaseMetaData = connection.getMetaData();
			resultSet = databaseMetaData.getSchemas();

			while (resultSet.next()) {
				String cat = resultSet.getString("TABLE_CATALOG");
				String schema = resultSet.getString("TABLE_SCHEM");
				if (catalog == null) {
					if (cat == null)
						if (schema != null)
							schemas.add(schema);
				} else {
					if (catalog.equals(cat) && schema != null)
						schemas.add(schema);
				}
			}
			return schemas.toArray(new String[0]);
		} catch (Throwable t) {
			throw new RuntimeException(t.getMessage());
		} finally {
			JdbcUtils.close(resultSet);
			JdbcUtils.close(connection);
		}
	}

	public static String[] getTables(Connection connection, String catalog, String schema) {
		ResultSet resultSet = null;
		List<String> tables = new ArrayList<String>();
		try {
			DatabaseMetaData databaseMetaData = connection.getMetaData();
			resultSet = databaseMetaData.getSchemas();

			while (resultSet.next()) {
				String table = resultSet.getString("TABLE_NAME");
				if (table != null)
					tables.add(table);
			}
			return tables.toArray(new String[0]);
		} catch (Throwable t) {
			throw new RuntimeException(t.getMessage());
		} finally {
			JdbcUtils.close(resultSet);
			JdbcUtils.close(connection);
		}

	}

}
