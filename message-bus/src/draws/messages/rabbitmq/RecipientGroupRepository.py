class RecipientGroupRepository:
    #@Query( "select * " + 
    #        "from Recipient_Group " + 
    #        "where group_name = :groupName" )
    def findByName(name):
        raise NotImplementedError
    def deleteAll(self):
        pass
        #raise NotImplementedError
    def findAll(self):
        pass
        #raise NotImplementedError
