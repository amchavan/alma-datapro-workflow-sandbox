package alma.obops.draws.messages;

import com.fasterxml.jackson.annotation.JsonIgnore;

import alma.obops.draws.messages.Envelope;
import alma.obops.draws.messages.Message;

public class AbstractMessage implements Message {

	// Need to ignore envelope when (de)serializing -- Jackson cannot
	// cope with circular references like Envelope -> Message -> Envelope
	@JsonIgnore
	private Envelope envelope;

	@Override
	public Envelope getEnvelope() {
		// TODO Auto-generated method stub
		return this.envelope;
	}

	@Override
	public void setEnvelope(Envelope envelope) {
		this.envelope = envelope;
	}
}
