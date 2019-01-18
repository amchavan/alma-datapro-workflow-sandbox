package alma.adapt.examples.common;

import alma.icd.adapt.messagebus.AbstractMessage;
import alma.icd.adapt.messagebus.ResponseMessage;

/**
 * Describes a result, e.g. <code>{"datetime":"2"}</code><br>
 * Our calculator responds with this kind of message
 */
public class DatetimeResponse extends AbstractMessage implements ResponseMessage {
	public String datetime;

	public DatetimeResponse() {
		// empty
	}

	public DatetimeResponse(String datetime) {
		this.datetime = datetime;
	}
}
