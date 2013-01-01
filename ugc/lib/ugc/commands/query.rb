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

    result = $application[type].query query

    collection = result.collection
    format_collection collection, parsed_query['select']
  end

end

def parse_sql(query)
  result = {}
  keywords = %w(select from where)
  current = nil
  query.downcase.split(/[\s,*]/).each do |ea|
    next if ea == ''
    if keywords.include? ea
      current = ea
    elsif current
      if result[current]
        if result[current].is_a? Array
          result[current] << ea
        else
          result[current] = [result[current]] << ea
        end
      else
        result[current] = ea
      end
    end
  end
  result
end