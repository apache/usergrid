# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

class Hash

  # not recursive
  def symbolize_keys
    inject({}) do |options, (key, value)|
      options[(key.to_sym rescue key) || key] = value
      options
    end
  end

  def except(*keys)
    dup.except! *keys
  end

  def except!(*keys)
    keys.each { |key| key.is_a?(Array) ? except!(*key) : delete(key) }
    self
  end

  # recursive
  def add_dot_notation!(_hash=self)
    _hash.each do |k,v|
      getter = k.to_sym; setter = "#{k}=".to_sym
      _hash.define_singleton_method getter, lambda { _hash[k] } unless _hash.respond_to? getter
      _hash.define_singleton_method setter, lambda { |v| _hash[k] = v } unless _hash.respond_to? setter
      add_dot_notation!(v) if v.is_a? Hash
      v.each { |e| add_dot_notation!(e) if e.is_a? Hash } if v.is_a? Array
    end
  end
end
