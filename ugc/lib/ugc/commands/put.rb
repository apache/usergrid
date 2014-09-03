
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

desc 'idempotent create or update (put is usually an update)'
arg_name '[path] [data]'

command :put,:update do |c|
  c.flag [:d,:data], :desc => 'does nothing: for ease of conversion from curl'
  c.flag [:f,:file], :must_match => /.*=.*/, :arg_name => 'field_name=file_name', :desc => 'will force multipart upload'

  c.action do |global_options,options,args|
    help_now! unless args[0]

    resource = $context[args[0]]
    payload = parse_data(options[:data] || args[1])
    if $settings.show_curl?
      puts_curl(:put, resource, payload, options[:file])
    else
      if options[:file]
        format_response multipart_upload resource, payload, options[:file], :put
      else
        format_response resource.put payload
      end
    end
  end

end
