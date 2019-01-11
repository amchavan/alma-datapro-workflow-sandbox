from draws.messages.Envelope.Envelope import State
from draws.messages.SimpleEnvelope import SimpleEnvelope

import json

class SimpleEnvelopeDeserializer:
    def deserialize(self, jp, ctxt):
        raise NotImplementedError #Apparently is not used. If I'm wrong we'll get an exception :)
#        ObjectMapper mapper = (ObjectMapper) jp.getCodec();
#        ObjectNode root = (ObjectNode) mapper.readTree(jp);
#        
#        envelope = SimpleEnvelope();
#        
#        JsonNode etNode = root.get( "expireTime" );
#        Long expireTime = (etNode == null || etNode.isNull()) ? null : etNode.longValue();
#
#        String messageClass = root.get("messageClass").textValue()
#        state = State.valueOf(root.get("state").textValue())
#        envelope.setExpireTime(expireTime)
#        envelope.setId(root.get("_id").textValue())
#        envelope.setSentTimestamp(root.get("sentTimestamp").textValue())
#        envelope.setReceivedTimestamp(root.get("receivedTimestamp").textValue())
#        envelope.setConsumedTimestamp(root.get("consumedTimestamp").textValue())
#        envelope.setExpiredTimestamp(root.get("expiredTimestamp").textValue())
#        envelope.setRejectedTimestamp(root.get("rejectedTimestamp").textValue())
#        envelope.setOriginIP(root.get("originIP").textValue())
#        envelope.setQueueName(root.get("queueName").textValue())
#        envelope.setToken(root.get("token").textValue())
#        envelope.setState(state)
#        envelope.setMessageClass(messageClass)
#        
#        try:
#            if(messageClass is not None):
#                ObjectMapper objectMapper = new ObjectMapper()
#                Class<?> clasz = Class.forName(messageClass)
#                Message o = (Message) objectMapper.readValue(root.get("message").toString(), clasz)
#                envelope.setMessage(o)
#                
#                // Need to set envelope manually -- Jackson cannot
#                // cope with circular references like Envelope -> Message -> Envelope
#                o.setEnvelope(envelope)
#        except ClassNotFoundException as e:
#            msg = "Deserialization of class '" + messageClass + "' failed: "
#            raise Exception(msg, e)
#        return envelope;
