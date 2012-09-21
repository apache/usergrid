class Hash

  # recursive
  def add_dot_notation!
    add_dot_notation_recurse! self
  end

  # not recursive
  def symbolize_keys
    inject({}) do |options, (key, value)|
      options[(key.to_sym rescue key) || key] = value
      options
    end
  end

  private

  def add_dot_notation_recurse!(_hash)
    _hash.each do |k,v|
      getter = k.to_sym; setter = "#{k}=".to_sym
      _hash.define_singleton_method getter, lambda { _hash[k] } unless _hash.respond_to? getter
      _hash.define_singleton_method setter, lambda { |v| _hash[k] = v } unless _hash.respond_to? setter
      add_dot_notation_recurse!(v) if v.is_a? Hash
      v.each { |e| add_dot_notation_recurse!(e) if e.is_a? Hash } if v.is_a? Array
    end
  end
end
