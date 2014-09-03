
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

desc "set the current profile (creates if it doesn't exist)"
arg_name 'profile name'

command :profile,:profiles do |c|
  c.switch :d,:delete
  c.action do |global_options,options,args|
    if args[0]
      if options[:delete]
        $settings.delete_profile args[0]
        puts "Deleted profile: " + args[0]
      else
        $settings.active_profile_name = args[0]
        puts 'Set active profile:'
        show_profile(args[0])
      end
    else
      puts "Saved profiles:"
      $settings.profiles.each_key do |name|
        show_profile(name)
      end
    end
  end
end

def show_profile(name)
  profile = $settings.profile(name)
  print $settings.active_profile_name == name ? ' *' : '  '
  puts name
  $settings.profile(name).each_pair do |k,v|
    puts "    #{k}: #{v}"
  end
end
