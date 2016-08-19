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

org_url_template = "{base_url}/{org_id}"
app_url_template = "%s/{app_id}" % org_url_template

app_token_url_template = "%s/token" % app_url_template

collection_url_template = "%s/{collection}" % app_url_template
collection_query_url_template = "%s?ql={ql}&limit={limit}" % collection_url_template

post_collection_url_template = collection_url_template
entity_url_template = "%s/{uuid_name}" % collection_url_template
get_entity_url_template = "%s?connections={connections}" % entity_url_template
put_entity_url_template = entity_url_template
delete_entity_url_template = entity_url_template

assign_role_url_template = '%s/roles/{role_uuid_name}/{entity_type}/{entity_uuid_name}' % app_url_template

connect_entities_by_type_template = '%s/{from_collection}/{from_uuid_name}/{relationship}/{to_collection}/{to_uuid_name}' % app_url_template