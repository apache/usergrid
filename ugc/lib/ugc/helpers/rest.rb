def multipart_upload(resource, payload, file, method=:post, additional_headers = {})
  payload = payload.is_a?(Hash) ? payload : MultiJson.load(payload)
  filename = 'file'
  filename, file = file.split '=' if file.is_a?(String)
  file = file[1..-1] if file.start_with? '@' # be kind to curl users
  payload[filename] = file.is_a?(File) ? file : File.new(file, 'rb')
  payload[:multipart] = true
  headers = (resource.options[:headers] || {}).merge(additional_headers)
  response = RestClient::Request.execute(resource.options.merge(
                                             :method => method,
                                             :url => resource.url,
                                             :payload => payload,
                                             :headers => headers))
  resource.instance_variable_set :@response, response
  response.resource = resource
  response
end