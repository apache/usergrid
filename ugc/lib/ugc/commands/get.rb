desc 'retrieve a collection or entity'

command :get,:show,:ls,:list do |c|

  c.desc 'url'
  c.command [:url] do |c2|

    c2.action do |global_options,options,args|
      help_now! unless args[0]

      format_response $application[args[0]].get
    end
  end

  c.desc 'collections'
  c.command [:collections] do |c2|

    c2.action do |global_options,options,args|
      app = $application.entity
      collections = app['metadata']['collections']
      table border: $settings.table_border? do
        row header: true do
          collections.first[1].each_key do |k|
            column k
          end
        end
        collections.each_value do |coll|
          row do
            coll.each_value do |v|
              column v
            end
          end
        end
      end
    end
  end

  default_names = %w(assets users events roles folders activities devices groups)
  default_names.each do |e|
    c.desc e
    c.command [e.to_sym] do |c2|
      c2.action do |global_options,options,args|
        response = $application[e].get
        format_collection response.collection
        save_response response
      end
    end
  end

  c.default_command :url

end
