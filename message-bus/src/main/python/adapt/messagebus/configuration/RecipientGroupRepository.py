class RecipientGroupRepository(object):
    #@Query( "select * " + 
    #        "from Recipient_Group " + 
    #        "where group_name = :groupName" )
    def findByName(self, name):
        return None
    def deleteAll(self):
        pass
        #raise NotImplementedError
    def findAll(self):
        return []
        #raise NotImplementedError
    def save(self, group):
        pass
