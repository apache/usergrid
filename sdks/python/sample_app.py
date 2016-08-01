# */
# * Licensed to the Apache Software Foundation (ASF) under one
# * or more contributor license agreements.  See the NOTICE file
# * distributed with this work for additional information
# * regarding copyright ownership.  The ASF licenses this file
# * to you under the Apache License, Version 2.0 (the
# * "License"); you may not use this file except in compliance
# * with the License.  You may obtain a copy of the License at
# *
# *   http://www.apache.org/licenses/LICENSE-2.0
# *
# * Unless required by applicable law or agreed to in writing,
# * software distributed under the License is distributed on an
# * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# * KIND, either express or implied.  See the License for the
#    * specific language governing permissions and limitations
# * under the License.
# */

from usergrid import Usergrid

__author__ = 'Jeff West @ ApigeeCorporation'


def main():
    Usergrid.init(org_id='jwest1',
                  app_id='sandbox')

    response = Usergrid.DELETE('pets', 'max')

    if not response.ok:
        print 'Failed to delete max: %s' % response
        exit()

    response = Usergrid.DELETE('owners', 'jeff')

    if not response.ok:
        print 'Failed to delete Jeff: %s' % response
        exit()

    response = Usergrid.POST('pets', {'name': 'max'})

    if response.ok:
        pet = response.first()

        print pet

        response = Usergrid.POST('owners', {'name': 'jeff'})

        if response.ok:
            owner = response.first()

            print owner

            response = pet.connect('ownedBy', owner)

            if response.ok:
                print 'Connected!'

                response = pet.disconnect('ownedBy', owner)

                if response.ok:
                    print 'all done!'
                else:
                    print response

            else:
                print 'failed to connect: %s' % response

        else:
            print 'Failed to create Jeff: %s' % response

    else:
        print response


main()
