desc 'delete an entity'
arg_name 'url'

command :delete do |c|

  c.action do |global_options,options,args|
    help_now! unless args[0]

    format_response $application[args[0]].delete
  end

end
