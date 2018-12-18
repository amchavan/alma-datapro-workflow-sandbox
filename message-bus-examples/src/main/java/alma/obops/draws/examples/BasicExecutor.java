package alma.obops.draws.examples;

import java.io.IOException;
import java.util.Calendar;
import java.util.TimeZone;

import org.springframework.beans.factory.annotation.Autowired;

import alma.obops.draws.messages.AbstractMessage;
import alma.obops.draws.messages.Executor;
import alma.obops.draws.messages.MessageBroker;
import alma.obops.draws.messages.MessageQueue;
import alma.obops.draws.messages.RequestMessage;
import alma.obops.draws.messages.RequestProcessor;
import alma.obops.draws.messages.ResponseMessage;
import alma.obops.draws.messages.TimeLimitExceededException;

/**
 * A basic executor, returns the current datetime in a given timezone.<br>
 * This class defines the request and response message formats.
 */
public class BasicExecutor implements Runnable {

	public static final String DATETIME_QUEUE = "datetime";

	@Autowired
	private MessageBroker broker;

	/**
	 * A datetime request, e.g. <code>{"service":"datetime", "timezone":""}</code>
	 * Our calculator expects messages of that form as requests.
	 */
	public static class DatetimeRequest extends AbstractMessage implements RequestMessage  {
		public String timezone;

		public DatetimeRequest() {
			// empty
		}

		public DatetimeRequest(String timezone) {
			this.timezone = timezone;
		}
	}

	/**
	 * Describes a result, e.g. <code>{"datetime":"2"}</code><br>
	 * Our calculator responds with this kind of message
	 */
	public static class DatetimeResponse extends AbstractMessage implements ResponseMessage {
		public String datetime;

		public DatetimeResponse() {
			// empty
		}

		public DatetimeResponse(String datetime) {
			this.datetime = datetime;
		}
	}

	/** Implements this service's logic */
	public static class DatetimeProcessor implements RequestProcessor {

		@Override
		public ResponseMessage process(RequestMessage message) {

			DatetimeRequest request = (DatetimeRequest) message;
			System.out.println(">>> Received request with TZ=" + request.timezone);
			TimeZone tz = TimeZone.getTimeZone(request.timezone);
			Calendar c = Calendar.getInstance(tz);
			return new DatetimeResponse(c.getTime().toString());
		}
	}

	@Override
	public void run() {
		MessageQueue queue = broker.messageQueue( DATETIME_QUEUE );
		RequestProcessor processor = new DatetimeProcessor();
		Executor executor = new Executor( queue, processor, 5000 );

		try {
			executor.run();
		} 
		catch( TimeLimitExceededException e ) {
			System.out.println(">>> Timed out: " + e.getMessage());
			System.exit( 0 );
		} 
		catch (IOException e) {
			e.printStackTrace();
			System.exit( 1 );
		}
	}
}
