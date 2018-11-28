class RecipientGroup:
    __MEMBER_SEPARATOR = ","
    
    def __init__(self):
        self._id = None
        self._groupName = None
        self._groupMembers = None
    
    def __init__(self, groupName):
        self.__init__()
        self._groupName = groupName

    def addMember(newMember):
        if self._groupMembers is None:
            self._groupMembers = newMember
            return true
        
        members = self._groupMembers.split(MEMBER_SEPARATOR)
        
        #Do we already have that new member?
        for m in members:
            if m.equals(newMember):
                return false
        
        #NO, need to add the new member
        if len(members) > 0:
            #if needed, add a separator
            self._groupMembers += MEMBER_SEPARATOR
        self._groupMembers += newMember
        return true
    
    def hashCode(self):
        prime = 31
        result = 1
        result = prime * result + (0 if (self._groupMembers is None) else self._groupMembers.hashCode())
        result = prime * result + (0 if (self._groupName is None) else self._groupName.hashCode())
        return result

    def equals(self, obj):
        if (self is obj):
            return True
        if (obj is None):
            return False
        if (self != obj):
            return False
        if (self._groupMembers is None) and (obj._groupMembers is not None):
            return False
        if (self._groupMembers is not None) and (not self._groupMembers.equals(obj.groupMembers)):
            return False
        if (groupName is None) and (obj._groupName is not None):
            return False
        if (groupName is not None) and (not self._groupName.equals(other.groupName)):
            return False
        return True

    def __str__(self):
        return this.__class__.__qualname__ + "[" + self._groupName + ": " + self._groupMembers + "]"

    def getGroupMembersAsList(self):
        ret = []
        members = self._groupMembers.split(MEMBER_SEPARATOR)
        for m in members:
            ret.append(m)
        return ret
