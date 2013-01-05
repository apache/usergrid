desc 'set the base url, org, and app'

command :target do |c|

  c.desc 'set base url'
  c.command [:base,:base_url] do |ep|
    ep.action do |global_options,options,args|
      $settings.base_url = args[0]
      puts "base_url = #{$settings.base_url}"
    end
  end

  c.desc 'set organization'
  c.command [:org,:organization] do |ep|
    ep.action do |global_options,options,args|
      help_now! unless args[0]

      $settings.organization = args[0]
      puts "organization = #{$settings.organization}"
    end
  end

  c.desc 'set application'
  c.command [:app,:application] do |ep|
    ep.action do |global_options,options,args|
      help_now! unless args[0]

      $settings.application = args[0]
      puts "application = #{$settings.application}"
    end
  end

  c.desc 'set full url - parses base, org, and app'
  c.command [:url] do |url|
    url.action do |global_options,options,args|
      if args[0]
        app_name = args[0].split('/')[-1]
        org_name = args[0].split('/')[-2]
        base_url = args[0][0..args[0].index(org_name)-2]
        $settings.base_url = base_url
        $settings.organization = org_name
        $settings.application = app_name
      end
      puts "base_url = #{$settings.base_url}"
      puts "organization = #{$settings.organization}"
      puts "application = #{$settings.application}"
    end
  end

  c.default_command :url
end
