def puts_curl(command, resource, payload=nil, file=nil)
  headers = resource.options[:headers] || {}
  req = RestClient::Request.new(resource.options.merge(
                      :method => :get,
                      :url => resource.url,
                      :headers => headers))
  headers.delete(:content_type) if file
  curl_headers = req.make_headers headers
  curl_headers = curl_headers.map {|e| %Q[-H "#{e.join(': ')}"] }.join(' ')
  curl = "curl -X #{command.upcase} -i #{curl_headers}"
  if file
    payload = multipart_payload payload || {}, file
    payload = payload.map { |k,v|
      File.file?(v) ? %Q[-F "#{k}=@#{File.absolute_path(v)}"] : %Q[-F "#{k}=#{v}"]
    }.join ' '
    curl = "#{curl} #{payload}"
  else
    curl = %Q[#{curl} -d '#{payload}'] if (payload)
  end
  puts "#{curl} '#{resource.url}'"
end