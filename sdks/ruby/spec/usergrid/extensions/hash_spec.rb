require_relative '../../../lib/usergrid/extensions/hash'

describe Hash do

  it "should add dot notation recursively" do
    h = { test1: 'test1', test2: { test2a: 'test2a' }, test3: [ { test3a: 'test3a' }] }
    expect { h.test1 }.should raise_error
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