# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

from org.apache.usergrid.persistence import EntityRef
from org.apache.usergrid.services import AbstractService
from java.util import UUID

class PytestService(AbstractService):
   def __init__(self):
      self._id = UUID(0, 0)
      AbstractService.__init__(self, "pytest")

   def getId(self):
      return self._id

   def getType(self):
      return "Python"

