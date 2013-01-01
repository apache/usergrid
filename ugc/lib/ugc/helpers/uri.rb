def resource(uri)
  if URI.parse(uri).host
    Usergrid::Resource.new(uri, nil, $application.options) # absolute
  else
    $application[uri] # relative
  end
end
