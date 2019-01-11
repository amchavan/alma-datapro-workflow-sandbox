package alma.obops.draws.dashboard.persistence;

import javax.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.validation.annotation.Validated;

/**
 * Extract CouchDB connection properties from the ALMA properties file
 * 
 * @author amchavan, 13-Nov-2018
 */

@Configuration
@PropertySource("file:${ACSDATA}/config/archiveConfig.properties")
@EnableConfigurationProperties
@ConfigurationProperties( prefix = "archive.couchdb" )
@Validated
public class CouchDbConfigurationProperties {
	
	@NotBlank
	private String connection;
	private String username = null;
	private String password = null;

	public String getConnection() {
		return connection;
	}

	public String getPassword() {
		return password;
	}

	public String getUsername() {
		return username;
	}

	public void setConnection(String connection) {
		this.connection = connection;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setUsername(String username) {
		this.username = username;
	}
}

