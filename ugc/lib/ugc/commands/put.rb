desc 'idempotent create or update (usually an update)'
arg_name 'url [data]'

command :put,:update do |c|
  c.flag [:d,:data]

  c.action do |global_options,options,args|
    help_now! unless args[0]

    format_response $application[args[0]].put parse_data(options[:data] || args[1])
  end

end
