from com.apigee.util import Crypto
request.setVariable('content',Crypto.urlencode(request.getVariable('content')))
