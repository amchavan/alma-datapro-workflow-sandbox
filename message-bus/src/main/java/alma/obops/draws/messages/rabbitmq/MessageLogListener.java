package alma.obops.draws.messages.rabbitmq;

import static alma.obops.draws.messages.rabbitmq.RabbitMqMessageBroker.LOGGING_MESSAGE_BUS;
import static alma.obops.draws.messages.rabbitmq.RabbitMqMessageBroker.MESSAGE_STATE_ROUTING_KEY;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;

import alma.obops.draws.messages.SimpleEnvelope;

@Component
public class MessageLogListener implements Runnable {

	private Consumer consumer;
	private Channel channel;
	
	class MessageLogConsumer extends DefaultConsumer {

		private PersistedEnvelopeRepository envelopeRepository;
		public MessageLogConsumer( Channel channel, PersistedEnvelopeRepository envelopeRepository ) {
			super(channel);
			this.envelopeRepository = envelopeRepository;
		}
		
		@Override
		public void handleDelivery( String consumerTag, 
									com.rabbitmq.client.Envelope envelope,
									BasicProperties properties, 
									byte[] body ) throws IOException {

			String message = new String( body );
			String msg = ">>>> handling delivery: " + envelope.getRoutingKey() + ": " + message; 
			System.out.println( msg );
			
			PersistedEnvelope persistedEnvelope;
			
			if( envelope.getRoutingKey().equals( MESSAGE_STATE_ROUTING_KEY )) {
				
				String stateMessage = new String( body );
				String[] t = stateMessage.split( "@" );
				String id = t[0];
				String state = t[1];
				String timestamp = t[2];
				
				Iterable<PersistedEnvelope> all = envelopeRepository.findAll();
				for (PersistedEnvelope pe : all) {
					System.out.println( ">>> " + pe );
				}
				
				Optional<PersistedEnvelope> opt = envelopeRepository.findByEnvelopeId( id );
				persistedEnvelope = opt.get();
				persistedEnvelope.state = state;
				switch( state ) {
				
				case "Sent":
					persistedEnvelope.sentTimestamp = timestamp;
					break;
					
				case "Received":
					persistedEnvelope.receivedTimestamp = timestamp;
					break;
					
				case "Consumed":
					persistedEnvelope.consumedTimestamp = timestamp;
					break;
					
				case "Expired":
					persistedEnvelope.expiredTimestamp = timestamp;
					break;
				
				default:
					throw new RuntimeException( "Unknown state: '" + state + "'" );
				}
				
			}
			else {
				String json = new String(body, "UTF-8");
				System.out.println( ">>>> delivered json: " + json );
				
				ObjectMapper objectMapper = new ObjectMapper();
				SimpleEnvelope simpleEnvelope = objectMapper.readValue( json, SimpleEnvelope.class );
				persistedEnvelope = PersistedEnvelope.convert( simpleEnvelope );
			}
			envelopeRepository.save( persistedEnvelope );

			Iterable<PersistedEnvelope> all = envelopeRepository.findAll();
			for (PersistedEnvelope pe : all) {
				System.out.println( ">>> pe: " + pe );
			}
		}
	}
	
	public MessageLogListener( Channel channel, String exchangeName, PersistedEnvelopeRepository envelopeRepository ) throws IOException, TimeoutException {
		this.channel = channel;
		this.channel.queueBind( LOGGING_MESSAGE_BUS, exchangeName, "#" );
		this.consumer = new MessageLogConsumer( channel, envelopeRepository );
	}
	
	@Override
	public void run() {
		try {
			boolean autoAck = true;
			this.channel.basicConsume( LOGGING_MESSAGE_BUS, autoAck, this.consumer );
		} 
		catch( IOException e ) {
			throw new RuntimeException( e );
		}
	}
}