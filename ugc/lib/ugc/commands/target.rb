
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

desc 'set the base url, org, and app for the current profile'

command :target do |c|

  c.desc 'set base url'
  c.command [:base,:base_url] do |ep|
    ep.action do |global_options,options,args|
      $settings.base_url = args[0]
      puts "base_url = #{$settings.base_url}"
    end
  end

  c.desc 'set organization'
  c.command [:org,:organization] do |ep|
    ep.action do |global_options,options,args|
      help_now! unless args[0]

      $settings.organization = args[0]
      puts "organization = #{$settings.organization}"
    end
  end

  c.desc 'set application'
  c.command [:app,:application] do |ep|
    ep.action do |global_options,options,args|
      help_now! unless args[0]

      $settings.application = args[0]
      puts "application = #{$settings.application}"
    end
  end

  c.desc 'set full url - parses base, org, and app'
  c.command [:url] do |url|
    url.action do |global_options,options,args|
      if args[0]
        app_name = args[0].split('/')[-1]
        org_name = args[0].split('/')[-2]
        base_url = args[0][0..args[0].index(org_name)-2]
        $settings.base_url = base_url
        $settings.organization = org_name
        $settings.application = app_name
      end
      puts "base_url = #{$settings.base_url}"
      puts "organization = #{$settings.organization}"
      puts "application = #{$settings.application}"
    end
  end

  c.default_command :url
end
