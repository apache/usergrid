from java.lang import System

start = flow.getVariable('client.received.start.timestamp')
end=System.currentTimeMillis()
flow.setVariable('max.responsetime',(end.subtract(start).longValue()));