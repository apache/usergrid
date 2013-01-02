require 'io/console'

desc 'Describe login here'
arg_name 'username'

command :login do |c|

  c.switch [:a,:admin]
  c.action do |global_options,options,args|

    help_now! unless args[0]

    password = ask('password: ') { |q| q.echo = '*' }

    if password
      if options[:admin]
        management = Ugc::Management.new
        management.login args[0], password
      else
        application = Ugc::Application.new
        application.login args[0], password
      end
    end
    puts "logged in user: #{args[0]}"
  end
end
