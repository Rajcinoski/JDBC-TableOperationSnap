package com.snap.iw.tableCopy;

import java.net.URL;
import java.sql.Connection;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.snaplogic.api.ExecutionException;
import com.snaplogic.common.utilities.URLEncoder;
import com.snaplogic.util.SldbJarClassLoader;

/**
 * @author marjan.sterjev
 *
 */
public class DataSourceRegistry {

	private Cache<String, DataSourceWrapper> dataSourceCache;

	public DataSourceRegistry() {
		dataSourceCache = CacheBuilder.newBuilder().maximumSize(50).expireAfterAccess(5, TimeUnit.MINUTES)
				.removalListener(new RemovalListener<String, DataSourceWrapper>() {

					@Override
					public void onRemoval(RemovalNotification<String, DataSourceWrapper> notification) {
						JdbcUtils.close(notification.getValue().dataSource);
					}

				}).build();
	}

	public DataSource getDataSource(DataSourceConfiguration dataSourceConfiguration) {
		String key = dataSourceConfiguration.getKey();
		DataSourceWrapper dataSourceWrapper = dataSourceCache.getIfPresent(key);
		if (dataSourceWrapper != null) {
			if (!dataSourceWrapper.dataSourceConfiguration.equals(dataSourceConfiguration)) {
				// Data Source configuratioin has been changed
				dataSourceCache.invalidate(key);
				JdbcUtils.close(dataSourceWrapper.dataSource);
			} else {
				// Data Source configuration is still the same, so we can use
				// the existing pool
				return dataSourceWrapper.dataSource;
			}
		}
		//Implement the Double checked locking pattern
		//https://en.wikipedia.org/wiki/Double-checked_locking#Usage_in_Java
		synchronized (this) {
			dataSourceWrapper = dataSourceCache.getIfPresent(key);
			if(dataSourceWrapper!=null)
				return dataSourceWrapper.dataSource;
			BasicDataSource dataSource = createDataSource(dataSourceConfiguration);
			dataSourceWrapper = new DataSourceWrapper(dataSourceConfiguration, dataSource);
			dataSourceCache.put(key, dataSourceWrapper);
			return dataSource;
		}
	}

	private BasicDataSource createDataSource(DataSourceConfiguration dataSourceConfiguration) {
		ClassLoader parentClassLoader = Thread.currentThread().getContextClassLoader();
		URLEncoder urlEncoder = new URLEncoder();
		Connection connection = null;
		BasicDataSource dataSource = null;
		try {
			URL driverPath = urlEncoder.validateAndEncodeURI(dataSourceConfiguration.getDriverJar()).toURL();
			ClassLoader sldbClassLoader = new SldbJarClassLoader(new URL[] { driverPath }, parentClassLoader);
			Thread.currentThread().setContextClassLoader(sldbClassLoader);
			dataSource = new BasicDataSource();
			dataSource.setDriverClassName(dataSourceConfiguration.getDriverClassName());
			dataSource.setUrl(dataSourceConfiguration.getUrl());
			dataSource.setUsername(dataSourceConfiguration.getUsername());
			dataSource.setPassword(dataSourceConfiguration.getPassword());
			dataSource.setMinIdle(dataSourceConfiguration.getMinIddle());
			dataSource.setMaxIdle(dataSourceConfiguration.getMaxIddle());
			dataSource.setMaxTotal(dataSourceConfiguration.getMaxTotal());
			dataSource.setTestOnBorrow(true);
			dataSource.setValidationQuery(dataSourceConfiguration.getValidationQuery());
			dataSource.setDefaultAutoCommit(dataSourceConfiguration.getDefaultAutoCommit());
			dataSource.setFastFailValidation(true);
			dataSource.setRollbackOnReturn(true);
			dataSource.setMaxWaitMillis(5000);
			dataSource.setSoftMinEvictableIdleTimeMillis(60000);
			connection = dataSource.getConnection();
			return dataSource;
		} catch (Throwable t) {
			t.printStackTrace();
			JdbcUtils.close(dataSource);
			throw new ExecutionException(t.getMessage());
		} finally {
			JdbcUtils.close(connection);
			Thread.currentThread().setContextClassLoader(parentClassLoader);
		}
	}

	private static class DataSourceWrapper {

		DataSourceConfiguration dataSourceConfiguration;
		BasicDataSource dataSource;

		public DataSourceWrapper(DataSourceConfiguration dataSourceConfiguration, BasicDataSource dataSource) {
			super();
			this.dataSourceConfiguration = dataSourceConfiguration;
			this.dataSource = dataSource;
		}

	}
}
