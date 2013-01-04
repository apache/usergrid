def save_response(response)
  if response.multiple_entities? && response.collection.size > 1
    paths = response.entities.collect {|e| e['metadata']['path'] }
    blob = Marshal.dump paths
    $settings.save_profile_blob 'last_response', blob
  end
end

def replacement_for(replacement_parm)
  unless @last_response
    blob = $settings.load_profile_blob 'last_response'
    @last_response = Marshal.load blob
  end
  index = replacement_parm[1..-1].to_i
  @last_response[index-1]
end

def perform_substitutions(string)
  string = string.clone
  string.scan(/@[0-9]+/).uniq.each do |e|
    string.gsub! e, replacement_for(e)
  end
  string
end
