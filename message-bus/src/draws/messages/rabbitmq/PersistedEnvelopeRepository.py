class PersistedEnvelopeRepository:
    #@Query( "select * "
    #		+ "from envelope "
    #		+ "where state = :state" )
    def findByState(state):
        raise NotImplementedError
    
    #@Query( "select * " + 
    #		"from envelope " + 
    #		"where envelope_id = :envelope_id" )
    def findByEnvelopeId(envelope_id):
        raise NotImplementedError

    def deleteAll(self):
        pass
        #raise NotImplementedError
    def findAll(self):
        pass
        #raise NotImplementedError
