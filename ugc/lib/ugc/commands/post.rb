desc 'http post'
arg_name 'url [data]'

command :post do |c|
  c.flag [:d,:data]

  c.action do |global_options,options,args|
    help_now! unless args[0]

    format_result resource(args[0]).post (options[:data] || args[1])
  end

end
