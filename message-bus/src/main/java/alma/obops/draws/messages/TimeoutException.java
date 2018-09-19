package alma.obops.draws.messages;

@SuppressWarnings("serial")
public class TimeoutException extends RuntimeException {

	public TimeoutException() {
		super();
	}
	
	public TimeoutException( String message ) {
		super( message );
	}
}
