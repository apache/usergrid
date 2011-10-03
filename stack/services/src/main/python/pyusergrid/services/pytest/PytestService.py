from org.usergrid.persistence import EntityRef
from org.usergrid.services import AbstractService
from java.util import UUID

class PytestService(AbstractService):
   def __init__(self):
      self._id = UUID(0, 0)
      AbstractService.__init__(self, "pytest")

   def getId(self):
      return self._id

   def getType(self):
      return "Python"

