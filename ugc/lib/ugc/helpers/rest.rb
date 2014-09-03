
# Licensed to the Apache Software Foundation (ASF) under one or more contributor
# license agreements.  See the NOTICE.txt file distributed with this work for
# additional information regarding copyright ownership.  The ASF licenses this
# file to you under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License.  You may obtain a copy of
# the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
# License for the specific language governing permissions and limitations under
# the License.

def multipart_payload(payload, file)
  payload = payload.is_a?(Hash) ? payload : MultiJson.load(payload)
  filename = 'file'
  filename, file = file.split '=' if file.is_a?(String)
  file = file[1..-1] if file.start_with? '@' # be kind to curl users
  payload[filename] = file.is_a?(File) ? file : File.new(file, 'rb')
  payload
end

def multipart_upload(resource, payload, file, method=:post, additional_headers = {})
  payload = multipart_payload payload, file
  payload[:multipart] = true
  headers = (resource.options[:headers] || {}).merge(additional_headers)
  response = RestClient::Request.execute(resource.options.merge(
                                             :method => method,
                                             :url => resource.url,
                                             :payload => payload,
                                             :headers => headers))
  resource.instance_variable_set :@response, response
  response.resource = resource
  response
end
