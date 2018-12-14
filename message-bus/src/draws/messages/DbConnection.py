class DbConnection:
    def dbCreate(self, dbName):
        raise NotImplementedError
    def dbDelete(self, dbName):
        raise NotImplementedError
    def dbExists(self, dbName):
        raise NotImplementedError
    def delete(self, dbName, record):
        raise NotImplementedError
    def delete(self, dbName, _id, version):
        raise NotImplementedError
    def find(self, dbName, arrayClass, query):
        raise NotImplementedError
    def findAll(self, dbName, arrayClass):
        raise NotImplementedError
    def findOne(self, dbName, clasz, _id):
        raise NotImplementedError
    def findOne(self, dbName, rec):
        raise NotImplementedError
    def save(self, dbName, record):
        raise NotImplementedError
