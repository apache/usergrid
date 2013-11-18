desc 'Performs a logout on the current profile'

command :logout do |c|

  c.action do |global_options,options,args|

    $settings.access_token = nil
    puts "logged out of #{$settings.organization}/#{$settings.application}"

  end
end
