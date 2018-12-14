class Record:
    def getId(self):
        raise NotImplementedError
    def getVersion(self):
        raise NotImplementedError
    def setVersion(self, version):
        raise NotImplementedError
