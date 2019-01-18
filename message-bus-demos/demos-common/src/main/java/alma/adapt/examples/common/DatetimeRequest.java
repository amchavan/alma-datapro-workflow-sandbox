package alma.adapt.examples.common;

import alma.icd.adapt.messagebus.AbstractRequestMessage;

/**
 * A datetime request, e.g. <code>{"service":"datetime", "timezone":""}</code>
 * Our calculator expects messages of that form as requests.
 */
public class DatetimeRequest extends AbstractRequestMessage  {
	
	public String timezone;

	public DatetimeRequest() {
		// empty
	}

	public DatetimeRequest(String timezone) {
		this.timezone = timezone;
	}
}