export CPPATH=
java -cp "$(CPPATH)/xalan.jar:$(CPPATH)/xalan_serializer.jar" org.apache.xalan.xslt.Process -XSL ../config/Msg2Java.xslt -IN TestMsg.xml -OUT TestMsg.java
java -cp "$(CPPATH)/xalan.jar:$(CPPATH)/xalan_serializer.jar" org.apache.xalan.xslt.Process -XSL ../config/Msg2Py.xslt -IN TestMsg.xml -OUT TestMsg.py
