desc 'delete an entity'
arg_name 'url'

command :rm,:del,:delete do |c|

  c.action do |global_options,options,args|
    help_now! unless args[0]

    resource = $context[args[0]]
    if $settings.show_curl?
      puts_curl(:delete, resource)
    else
      format_response resource.delete
    end
  end

end
