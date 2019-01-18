package alma.icd.adapt.messagebus;

@SuppressWarnings("serial")
public class TimeLimitExceededException extends RuntimeException {

	public TimeLimitExceededException() {
		super();
	}
	
	public TimeLimitExceededException( String message ) {
		super( message );
	}
}
