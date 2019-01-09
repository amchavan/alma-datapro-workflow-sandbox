#import jwt
import json
from jose import jwk
from jose import jws
import time
import pytz
import base64
import numbers
import datetime

from draws.messages.security.TokenFactory import TokenFactory

class JWTFactory(TokenFactory):
    #Static Variables
    TIME_TO_LIVE_CLAIM = "ttl"
    EXPIRES_CLAIM = "exp"
    ROLES_CLAIM = "roles"
    TIME_TO_LIVE = 10000
    __JWKS = {"kty":"RSA","d":"g_JYrlcTrwMPWM0ONWdJsw9f6jQoyNZBAdt8CPcSQmZA4iuBnX2LwIih3NgunBWA1TfERAoyD3mRiPU-yBidgQ","e":"AQAB","use":"sig","kid":"testkey","alg":"RS256","n":"hDLlR76CTTQjnRyA0NqpBDcdq_Nc3fqPNXplLXUf9PSbolxm_SyYfUiTO2NhHy74Z13nZgWwpkJiM7K0QdlU5w"}
    __UT = pytz.utc
    ISOTIMEDATESTRING = "%Y-%m-%d'T'%H:%M:%S.%f"

    #Instance Methods
    def __init__(self):
        self.__rsaJWK = jwk.construct(JWTFactory.__JWKS)
        self.__rsaPublicJWK = self.__rsaJWK.public_key()
    
    def create(self, claims=None):
        if claims is None:
            claims = {}
            claims["sub"] = "user"
            claims["roles"] = ["MASTER/USER","OBOPS/AOD"]
            return self.create(claims)
        claims[JWTFactory.TIME_TO_LIVE_CLAIM] = JWTFactory.TIME_TO_LIVE
        #The JWT signature algorithm we will be using to sign the token
        nowMillis = time.time()
        now = datetime.datetime.fromtimestamp(nowMillis, tz=JWTFactory.__UT)
        #claims["iat"] = datetime.datetime.strftime(now, JWTFactory.ISOTIMEDATESTRING)
        #See if caller specified a TTL, convert that to an expiration time
        exp = None
        ttl = None
        if JWTFactory.TIME_TO_LIVE_CLAIM in claims:
            ttl = claims[JWTFactory.TIME_TO_LIVE_CLAIM]
        if ttl is not None and isinstance(ttl, numbers.Number):
            ttlMillis = ttl/1000
            if (ttlMillis > 0):
                expMillis = nowMillis + ttlMillis
                exp = datetime.datetime.fromtimestamp(expMillis, tz=JWTFactory.__UT)
                claims["exp"] = datetime.datetime.strftime(exp, JWTFactory.ISOTIMEDATESTRING)
            #del claims[JWTFactory.__TIME_TO_LIVE_CLAIM]
        try:
            signature = jws.sign(claims, self.__rsaJWK.prepared_key.exportKey(), algorithm='RS256')
        except Exception as e:
            print("Signing error!")
            raise e
        return signature
    
    def decode(self, token):
        if not self.isValid(token):
            raise Exception( "Invalid JWK: " + token)
        #This line will throw an exception if it is not a signed JWS (as expected)
        claims = {}
        try:
            claims = json.loads(jws.verify(token, self.__rsaPublicJWK.prepared_key.exportKey().decode(), algorithms='RS256'))
        except Exception as e:
            raise Exception("Invalid signature error!!")
        ret = {}
        for claim in claims:
            if claim in ["exp", "iat"]:
                ret[claim] = datetime.datetime.strptime(claims[claim], JWTFactory.ISOTIMEDATESTRING)
            else:
                ret[claim] = claims[claim]
        return ret
    
    def isValid(self, token):
        try:
            jws.verify(token, self.__rsaPublicJWK.prepared_key.exportKey().decode(), algorithms='RS256')
        except Exception as e:
            return False
        return True
