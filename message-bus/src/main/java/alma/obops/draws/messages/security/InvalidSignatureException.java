package alma.obops.draws.messages.security;

import io.jsonwebtoken.SignatureException;

public class InvalidSignatureException extends RuntimeException {
	private static final long serialVersionUID = -3137236729723396858L;

	public InvalidSignatureException() {
		super();
	}

	public InvalidSignatureException(SignatureException e) {
		super( e );
	}
}
