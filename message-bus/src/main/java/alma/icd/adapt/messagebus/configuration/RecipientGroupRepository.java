package alma.obops.draws.messages.configuration;

import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;

import alma.obops.draws.messages.rabbitmq.RecipientGroup;

@Component
public interface RecipientGroupRepository extends CrudRepository<RecipientGroup, String> {
	@Query( "select * " + 
			"from Recipient_Group " + 
			"where group_name = :groupName" )
	Optional<RecipientGroup> findByName( @Param("groupName") String name );
}
