@ECHO OFF

CALL ..\environment.bat

CD Test


%JAVA% -jar ..\product\NetSend.jar NetSend.5001.xml
%JAVA% -jar ..\product\NetSend.jar NetSend.6001.xml
