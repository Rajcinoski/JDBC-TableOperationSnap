package com.snap.iw.tableCopy;

import com.snaplogic.api.ExecutionException;

/**
 * @author marjan.sterjev
 *
 */
public class DataSourceConfiguration {

	private String driverJar;
	
	private String driverClassName;

	private String url;

	private String username;

	private String password;

	private int minIddle;

	private int maxIddle;

	private int maxTotal;

	private String validationQuery;

	private boolean defaultAutoCommit;

	public String getDriverJar() {
		return driverJar;
	}

	public void setDriverJar(String driverJar) {
		this.driverJar = driverJar;
	}

	public String getDriverClassName() {
		return driverClassName;
	}

	public void setDriverClassName(String driverClassName) {
		this.driverClassName = driverClassName;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public int getMinIddle() {
		return minIddle;
	}

	public void setMinIddle(int minIddle) {
		this.minIddle = minIddle;
	}

	public int getMaxIddle() {
		return maxIddle;
	}

	public void setMaxIddle(int maxIddle) {
		this.maxIddle = maxIddle;
	}

	public int getMaxTotal() {
		return maxTotal;
	}

	public void setMaxTotal(int maxTotal) {
		this.maxTotal = maxTotal;
	}

	public String getValidationQuery() {
		return validationQuery;
	}

	public void setValidationQuery(String validationQuery) {
		this.validationQuery = validationQuery;
	}

	public boolean getDefaultAutoCommit() {
		return defaultAutoCommit;
	}

	public void setDefaultAutoCommit(boolean defaultAutoCommit) {
		this.defaultAutoCommit = defaultAutoCommit;
	}

	public String getKey() {
		StringBuilder sb = new StringBuilder();
		check(url, "URL");
		check(username, "Username");
		sb.append(url).append("_").append(username);
		return sb.toString();
	}

	private void check(String s, String propertyName) {
		if (s == null || s.trim().length() == 0)
			throw new ExecutionException(String.format("%s not set!!!", propertyName));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (defaultAutoCommit ? 1231 : 1237);
		result = prime * result + ((driverClassName == null) ? 0 : driverClassName.hashCode());
		result = prime * result + ((driverJar == null) ? 0 : driverJar.hashCode());
		result = prime * result + maxIddle;
		result = prime * result + maxTotal;
		result = prime * result + minIddle;
		result = prime * result + ((password == null) ? 0 : password.hashCode());
		result = prime * result + ((url == null) ? 0 : url.hashCode());
		result = prime * result + ((username == null) ? 0 : username.hashCode());
		result = prime * result + ((validationQuery == null) ? 0 : validationQuery.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DataSourceConfiguration other = (DataSourceConfiguration) obj;
		if (defaultAutoCommit != other.defaultAutoCommit)
			return false;
		if (driverClassName == null) {
			if (other.driverClassName != null)
				return false;
		} else if (!driverClassName.equals(other.driverClassName))
			return false;
		if (driverJar == null) {
			if (other.driverJar != null)
				return false;
		} else if (!driverJar.equals(other.driverJar))
			return false;
		if (maxIddle != other.maxIddle)
			return false;
		if (maxTotal != other.maxTotal)
			return false;
		if (minIddle != other.minIddle)
			return false;
		if (password == null) {
			if (other.password != null)
				return false;
		} else if (!password.equals(other.password))
			return false;
		if (url == null) {
			if (other.url != null)
				return false;
		} else if (!url.equals(other.url))
			return false;
		if (username == null) {
			if (other.username != null)
				return false;
		} else if (!username.equals(other.username))
			return false;
		if (validationQuery == null) {
			if (other.validationQuery != null)
				return false;
		} else if (!validationQuery.equals(other.validationQuery))
			return false;
		return true;
	}
	
	

}
