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

__author__ = 'Jeff.West@yahoo.com'

management_base_url = '{base_url}/management'
management_org_url_template = "%s/organizations/{org_id}" % management_base_url
management_org_list_apps_url_template = "%s/applications" % management_org_url_template
management_app_url_template = "%s/applications/{app_id}" % management_org_url_template

org_token_url_template = "%s/token" % management_base_url
