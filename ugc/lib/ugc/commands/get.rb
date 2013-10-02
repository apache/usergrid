desc 'retrieve and display a collection or entity'

command :get,:show,:ls,:list do |c|

  c.desc '[path]'
  c.command [:path] do |c2|

    c2.action do |global_options,options,args|

      if args[0] && args[0] != 'collections' && args[0] != '/'
        resource = $context[args[0]]
        if $settings.show_curl?
          puts_curl(:get, resource)
        else
          format_response resource.get
        end
      else
        $application.list_collections
      end
    end
  end

  c.default_command :path

end
