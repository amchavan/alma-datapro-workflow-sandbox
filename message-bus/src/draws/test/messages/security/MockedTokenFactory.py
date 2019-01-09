from draws.messages.security.TokenFactory import TokenFactory

import base64

class MockedTokenFactory(TokenFactory):
    #Static variables
    __HEADER = "header"
    __VALID_SIGNATURE = "valid-signature"
    __INVALID_SIGNATURE = "invalid-signature"
    _instance = None

    #Static Methods
    @classmethod
    def getFactory(cls):
        if MockedTokenFactory._instance is None:
            MockedTokenFactory._instance = MockedTokenFactory()
        return MockedTokenFactory._instance

    #Instance methods
    def create(self, properties=None):
        if properties is None:
            claims = {}
            claims["sub"] = "user"
            claims["role"] = "admin"
            claims["ttl"] = "10000"
            return self.create(claims)
        if properties is None:
            raise IllegalArgumentException( "No properties given" )
        header = MockedTokenFactory.__HEADER
        signature = MockedTokenFactory.__VALID_SIGNATURE
        valid = None
        if "valid" in properties:
            valid = properties["valid"]
        if valid is not None and valid == "false":
            signature = MockedTokenFactory.__INVALID_SIGNATURE
        bodyBuilder = ""
        for propName in properties:
            propValue = properties.get(propName)
            if len(bodyBuilder) > 0:
                bodyBuilder += ","
            bodyBuilder += propName + "=" + propValue
        body = bodyBuilder
        encodedBody = base64.b64encode(body.encode("utf-8")).decode()
        return header + "." + encodedBody + "." + signature

    def decode(self, token):
        encodedBody = self.__isValidInternal( token )
        if encodedBody is None:
            raise Exception("Invalid signature...")
        body = base64.b64decode(encodedBody.encode()).decode()
        properties = {}
        claims = body.split(",")
        for claim in claims:
            t2 = claim.split( "=" )
            properties[t2[0]] = t2[1]
        return properties

    def isValid(self, token):
        return self.__isValidInternal(token) is not None

    def __isValidInternal(self, token):
        if token is None or len(token) == 0:
            return None
        
        t = token.split(".")
        if len(t) != 3:
            return None

#       header    = t[0]    IGNORED
        body      = t[1]
        signature = t[2]
        
        if signature != MockedTokenFactory.__VALID_SIGNATURE:
            return None
        return body
