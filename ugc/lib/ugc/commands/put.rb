desc 'http put'
arg_name 'url [data]'

command :put do |c|
  c.flag [:d,:data]

  c.action do |global_options,options,args|
    format_result resource(args[0]).put (options[:data] || args[1])
  end

end
