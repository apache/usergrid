#Licensed to the Apache Software Foundation (ASF) under one or more contributor
#license agreements.  See the NOTICE.txt file distributed with this work for
#additional information regarding copyright ownership.  The ASF licenses this
#file to you under the Apache License, Version 2.0 (the "License"); you may not
#use this file except in compliance with the License.  You may obtain a copy of
#the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
#Unless required by applicable law or agreed to in writing, software
#distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
#WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
#License for the specific language governing permissions and limitations under
#the License.

desc 'delete an entity'
arg_name 'url'

command :rm,:del,:delete do |c|

  c.action do |global_options,options,args|
    help_now! unless args[0]

    resource = $context[args[0]]
    if $settings.show_curl?
      puts_curl(:delete, resource)
    else
      format_response resource.delete
    end
  end

end
