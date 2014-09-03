
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

def save_response(response)
  if response && response.multiple_entities? && response.collection.size > 1
    paths = response.entities.collect {|e| e['metadata']['path'][1..-1] } rescue []
    blob = Marshal.dump paths
    $settings.save_profile_blob 'last_response', blob
  end
end

def replacement_for(replacement_parm)
  unless @last_response
    blob = $settings.load_profile_blob 'last_response'
    @last_response = Marshal.load blob
  end
  index = replacement_parm[1..-1].to_i
  raise "no data for replacement param: #{replacement_parm}" unless @last_response[index-1]
  @last_response[index-1]
end

def perform_substitutions(string)
  string = string.clone
  string.scan(/@[0-9]+/).uniq.each do |e|
    string.gsub! e, replacement_for(e)
  end
  string
end
