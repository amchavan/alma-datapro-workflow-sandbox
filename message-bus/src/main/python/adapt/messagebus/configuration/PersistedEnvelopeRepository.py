class PersistedEnvelopeRepository(object):
    #@Query( "select * "
    #		+ "from envelope "
    #		+ "where state = :state" )
    def findByState(self, state):
        return None
    #@Query( "select * " + 
    #		"from envelope " + 
    #		"where envelope_id = :envelope_id" )
    def findByEnvelopeId(self, envelope_id):
        return None
    def deleteAll(self):
        pass
    def findAll(self):
        return []
    def save(self, envelope):
        pass
