package alma.obops.draws.messages.configuration;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;

import alma.obops.draws.messages.rabbitmq.PersistedEnvelope;

@Component
public interface PersistedEnvelopeRepository extends CrudRepository<PersistedEnvelope, String> {
	@Query( "select * "
			+ "from envelope "
			+ "where state = :state" )
	List<PersistedEnvelope> findByState( @Param("state") String state );

	@Query( "select * " + 
			"from envelope " + 
			"where envelope_id = :envelope_id" )
	Optional<PersistedEnvelope> findByEnvelopeId( @Param("envelope_id") String envelope_id );
}