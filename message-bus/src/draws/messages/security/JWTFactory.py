import jwt
import time
import pytz
import base64
import numbers
import datetime

from draws.messages.security.TokenFactory import TokenFactory
from draws.messages.security.InvalidSignatureException import InvalidSignatureException

class JWTFactory(TokenFactory):
    __SECRET_KEY = "oeRaYY7Wo24sDqKSX3IM9ASGmdGPmkTd9jo1QTy4b7P9Ze5_9hKolVX8xNrQDcNRfVEdTZNOuOyqEGhXEbdJI-ZQ19k_o9MI0y3eZN2lp9jow55FfXMiINEdt1XR85VipRLSOkT6kSpzs2x-jbLDiz9iFVzkd81YKxMgPA7VfZeQUm4n-mOmnWMaVX30zGFU4L3oPBctYKkl4dYfqYWqRNfrgPJVi5DGFjywgxx0ASEiJHtV72paI3fDR2XwlSkyhhmY-ICjCRmsJN4fX1pdoL8a18-aQrvyu4j0Os6dVPYIoPvvY0SAZtWYKHfM15g7A3HD4cVREf9cUsprCRK93w="
    __TIME_TO_LIVE_CLAIM = "ttl";
    __signatureAlgorithm = "HS256"
    __UT = pytz.utc
    _instance = None;

    @classmethod
    def getFactory(cls):
        if JWTFactory._instance is None:
            JWTFactory._instance = JWTFactory(); 
        return JWTFactory._instance;
    
    def create(self, claims=None):
        if claims is None:
            claims = {}
            claims["sub"] = "user"
            claims["role"] = "admin"
            claims[JWTFactory.__TIME_TO_LIVE_CLAIM] = 10000
            return self.create(claims);
        #The JWT signature algorithm we will be using to sign the token
        nowMillis = time.time()
        #nowMillis = datetime.datetime.now(tz=JWTFactory.__UT)
        now = datetime.datetime.fromtimestamp(nowMillis, tz=JWTFactory.__UT)
        claims["iat"] = now
        #We will sign our JWT with our ApiKey secret
        apiKeySecretBytes = base64.b64decode(JWTFactory.__SECRET_KEY)
        #See if caller specified a TTL, convert that to an expiration time
        exp = None
        ttl = None
        if JWTFactory.__TIME_TO_LIVE_CLAIM in claims:
            ttl = claims[JWTFactory.__TIME_TO_LIVE_CLAIM]
        if ttl is not None and isinstance(ttl, numbers.Number):
            ttlMillis = ttl/1000
            if (ttlMillis > 0):
                expMillis = nowMillis + ttlMillis;
                exp = datetime.datetime.fromtimestamp(expMillis, tz=JWTFactory.__UT)
                claims["exp"] = exp
            del claims[JWTFactory.__TIME_TO_LIVE_CLAIM]
        return jwt.encode(claims, apiKeySecretBytes, algorithm=JWTFactory.__signatureAlgorithm).decode()
    
    def decode(self, encoded):
        apiKeySecretBytes = base64.b64decode(JWTFactory.__SECRET_KEY)
        #This line will throw an exception if it is not a signed JWS (as expected)
        claims = {}
        try:
            claims = jwt.decode(encoded.encode(), apiKeySecretBytes, algorithms=[JWTFactory.__signatureAlgorithm])
        except jwt.InvalidSignatureError as e:
            raise InvalidSignatureException(e)
        except jwt.ExpiredSignatureError as e:
            print("Expired message... continuing anyway.")
        ret = {}
        for claim in claims:
            ret[claim] = claims[claim]
        return ret;
    
    def isValid(self, token):
        try:
            self.decode(token)
        except InvalidSignatureException as e:
            return False;
        return True;
