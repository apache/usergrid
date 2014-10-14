
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

def puts_curl(command, resource, payload=nil, file=nil)
  headers = resource.options[:headers] || {}
  req = RestClient::Request.new(resource.options.merge(
                      :method => :get,
                      :url => resource.url,
                      :headers => headers))
  headers.delete(:content_type) if file
  curl_headers = req.make_headers headers
  curl_headers = curl_headers.map {|e| %Q[-H "#{e.join(': ')}"] }.join(' ')
  curl = "curl -X #{command.upcase} -i #{curl_headers}"
  if file
    payload = multipart_payload payload || {}, file
    payload = payload.map { |k,v|
      File.file?(v) ? %Q[-F "#{k}=@#{File.absolute_path(v)}"] : %Q[-F "#{k}=#{v}"]
    }.join ' '
    curl = "#{curl} #{payload}"
  else
    curl = %Q[#{curl} -d '#{payload}'] if (payload)
  end
  puts "#{curl} '#{URI::encode resource.url}'"
end
