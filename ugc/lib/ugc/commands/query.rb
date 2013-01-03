desc 'query'
long_desc 'note: query may contain a "from" clause instead of specifying collection_name'
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

    params = {}
    if parsed_query['limit']
      limit = parsed_query['limit']
      query.gsub! /limit\s+#{limit}/i, ''
      params[:limit] = limit
    end

    response = $application[type].query query, params

    format_collection response.collection, parsed_query['select']
  end

end
