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

require_relative '../../../lib/usergrid/extensions/hash'

describe Hash do

  it "should add dot notation recursively" do
    h = { test1: 'test1', test2: { test2a: 'test2a' }, test3: [ { test3a: 'test3a' }] }
    expect { h.test1 }.to raise_error
    h.add_dot_notation!
    h.test1.should eq 'test1'
    h.test2.test2a.should eq 'test2a'
    h.test3.first.test3a.should eq 'test3a'
    h.test1 = 'testx'
    h.test1.should eq 'testx'
  end

  it "should symbolize keys at top level" do
    h = { 'test1' => 'test1', 'test2' => { 'test2a' => 'test2a' }, 'test3' => [ { 'test3a' => 'test3a' }] }
    h['test1'].should eq 'test1'
    h = h.symbolize_keys
    h['test1'].should be_nil
    h[:test1].should eq 'test1'
    h[:test2][:test2a].should be_nil
    h[:test3].first['test3a'].should eq 'test3a'
  end
end