package alma.obops.draws.messages.examples;

import static alma.obops.draws.messages.examples.ExampleUtils.*;

import java.io.IOException;
import java.util.Calendar;
import java.util.TimeZone;

import alma.obops.draws.messages.CouchDbConfig;
import alma.obops.draws.messages.CouchDbMessageBus;
import alma.obops.draws.messages.Executor;
import alma.obops.draws.messages.Message;
import alma.obops.draws.messages.MessageBus;
import alma.obops.draws.messages.MessageQueue;
import alma.obops.draws.messages.RequestMessage;
import alma.obops.draws.messages.RequestProcessor;
import alma.obops.draws.messages.TimeoutException;

/**
 * A basic executor, returns the current datetime in a given timezone.<br>
 * This class defines the request and response message formats.
 */
public class BasicExecutor {

	public static final String DATETIME_QUEUE = "datetime";

	/**
	 * A datetime request, e.g. <code>{"service":"datetime", "timezone":""}</code>
	 * Our calculator expects messages of that form as requests.
	 */
	public static class DatetimeRequest implements RequestMessage {
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
	public static class DatetimeResponse implements Message {
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
		public Message process(RequestMessage message) {

			DatetimeRequest request = (DatetimeRequest) message;
			System.out.println(">>> Received request with TZ=" + request.timezone);
			TimeZone tz = TimeZone.getTimeZone(request.timezone);
			Calendar c = Calendar.getInstance(tz);
			return new DatetimeResponse(c.getTime().toString());
		}
	}

	public static void main(String[] args) throws IOException {
		CouchDbConfig config = new CouchDbConfig();
		MessageBus bus = new CouchDbMessageBus(config, MESSAGE_BUS_NAME);
		MessageQueue queue = bus.messageQueue(DATETIME_QUEUE);
		RequestProcessor processor = new DatetimeProcessor();
		Executor executor = new Executor(queue, processor, 5000);

		try {
			executor.run();
		} catch (TimeoutException e) {
			System.out.println(">>> Timed out: " + e.getMessage());
		}
	}
}
