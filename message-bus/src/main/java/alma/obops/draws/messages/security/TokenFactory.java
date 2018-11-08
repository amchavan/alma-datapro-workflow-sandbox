package alma.obops.draws.messages.security;

import java.util.Map;

public interface TokenFactory {

	/** @return <code>true</code> if the input string is a valid token */
	public boolean isValid( String token );

	/**
	 * Create a token from a set of claims (properties)
	 * @param claims A set of name/value pairs to be encoded in the token
	 * @return A token
	 */
	public String create( Map<String,Object> claims );

	/**
	 * Create a token from a standard, factory-specific set of claims (properties)
	 * @return A token
	 */
	public String create();

	/**
	 * Extract a set of claims (properties) from a token
	 * @return The set of claims (name/value pairs) encoded in the token
	 * @throws InvalidSignatureException 
	 */
	public Map<String,Object> decode( String token ) throws InvalidSignatureException;

}
