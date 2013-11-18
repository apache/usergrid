from org.usergrid.persistence import EntityRef
from java.util import UUID

class Test(EntityRef):
   def __init__(self):
      self._id = UUID(0, 0)

   def getId(self):
      return self._id

   def getType(self):
      return "Python"

