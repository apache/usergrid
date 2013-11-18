desc 'non-idempotent create or update (post is usually create)'
arg_name '[path] [data]'

command :post,:create do |c|
  c.flag [:d,:data], :desc => 'does nothing: for ease of conversion from curl'
  c.flag [:f,:file], :must_match => /.*=.*/, :arg_name => 'field_name=file_name', :desc => 'will force multipart upload'

  c.action do |global_options,options,args|
    help_now! unless args[0]

    resource = $context[args[0]]
    payload = parse_data(options[:data] || args[1])
    if $settings.show_curl?
      puts_curl(:post, resource, payload, options[:file])
    else
      if options[:file]
        format_response multipart_upload resource, payload, options[:file]
      else
        format_response resource.post payload
      end
    end
  end

end
