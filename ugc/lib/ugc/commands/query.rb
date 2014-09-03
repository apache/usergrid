
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

desc 'query (uses sql-like syntax)'
long_desc 'query may contain "from" clause instead of specifying collection_name'
arg_name '[collection_name] query'

command :query do |c|
  c.action do |global_options,options,args|

    case args.size
      when 2
        type = args[0]
        query = args[1]
      when 1
        query = args[0]
      else
        help_now!
    end

    parsed_query = parse_sql query

    if type == nil && parsed_query['from']
      type = parsed_query['from']
      query.gsub! /from\s+#{type}/i, ''
    end
    help_now! 'collection_name or sql from clause is required' unless type

    params = {}
    if parsed_query['limit']
      limit = parsed_query['limit']
      query.gsub! /limit\s+#{limit}/i, ''
      params[:limit] = limit
    end

    resource = $application[type]
    if $settings.show_curl?
      options = options.merge({ql: query}) if query
      puts_curl(:get, resource)
    else
      response = resource.query query, params
      format_collection response.collection, parsed_query['select']
      save_response response
    end
  end

end
