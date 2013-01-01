desc 'http get'
arg_name 'url'

command :get do |c|

  c.action do |global_options,options,args|
    format_result resource(args[0]).get
  end

end
