package alma.icd.adapt.messagebus;

import com.fasterxml.jackson.annotation.JsonIgnore;

import alma.icd.adapt.messagebus.Envelope;
import alma.icd.adapt.messagebus.Message;

public abstract class AbstractMessage implements Message {

	// Need to ignore envelope when (de)serializing -- Jackson cannot
	// cope with circular references like Envelope -> Message -> Envelope
	@JsonIgnore
	private Envelope envelope;

	@Override
	public Envelope getEnvelope() {
		return this.envelope;
	}

	@Override
	public void setEnvelope(Envelope envelope) {
		this.envelope = envelope;
	}
}
