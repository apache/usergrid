desc 'delete an entity'
arg_name 'url'

command :rm,:del,:delete do |c|

  c.action do |global_options,options,args|
    help_now! unless args[0]

    format_response $context[args[0]].delete
  end

end
