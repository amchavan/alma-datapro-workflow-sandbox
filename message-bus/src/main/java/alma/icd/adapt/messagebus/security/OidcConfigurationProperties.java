package alma.obops.draws.messages.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.validation.annotation.Validated;

/**
 * Extract OIDC authentication server connection properties from the OBOPS properties file
 * 
 * @author amchavan, 13-Nov-2018
 */

@Configuration
@ConfigurationProperties( prefix = "obops.oidc" )
@EnableConfigurationProperties()
@PropertySource("file:${ACSDATA}/config/obopsConfig.properties")
@Validated
public class OidcConfigurationProperties {
	
	/** OIDC server URL, something like <code>https://ma24088.ads.eso.org:8019/cas</code> */
//	@NotBlank
	private String serverUrl;

	/** OIDC service ID, something like <code>demoOIDC</code> */
//	@NotBlank
	private String serviceId;

	/** OIDC resource owner ID, something like <code>obops</code> */
//	@NotBlank
	private String resourceOwnerId;

	/** OIDC resource owner password, something like <code>8q76gaj</code> */
//	@NotBlank
	private String resourceOwnerPassword;

	public String getResourceOwnerId() {
		return resourceOwnerId;
	}

	public String getResourceOwnerPassword() {
		return resourceOwnerPassword;
	}

	public String getServerUrl() {
		return serverUrl;
	}

	public String getServiceId() {
		return serviceId;
	}

	public void setResourceOwnerId(String resourceOwnerId) {
		this.resourceOwnerId = resourceOwnerId;
	}

	public void setResourceOwnerPassword(String resourceOwnerPassword) {
		this.resourceOwnerPassword = resourceOwnerPassword;
	}

	public void setServerUrl(String serverUrl) {
		this.serverUrl = serverUrl;
	}

	public void setServiceId(String serviceId) {
		this.serviceId = serviceId;
	}
}

