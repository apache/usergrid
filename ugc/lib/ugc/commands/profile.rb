desc 'set the current profile'
arg_name 'profile name'

command :profile,:profiles do |c|
  c.switch :d,:delete
  c.action do |global_options,options,args|
    if args[0]
      if options[:delete]
        $settings.delete_profile args[0]
        puts "Deleted profile: " + args[0]
      else
        $settings.active_profile_name = args[0]
        puts 'Set active profile:'
        show_profile(args[0])
      end
    else
      puts "Saved profiles:"
      $settings.profiles.each_key do |name|
        show_profile(name)
      end
    end
  end
end

def show_profile(name)
  profile = $settings.profile(name)
  print $settings.active_profile_name == name ? ' *' : '  '
  puts name
  $settings.profile(name).each_pair do |k,v|
    puts "    #{k}: #{v}"
  end
end
