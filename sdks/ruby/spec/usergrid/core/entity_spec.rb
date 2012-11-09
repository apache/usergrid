describe Usergrid::Entity do

  before :all do
    @application = create_random_application
    @user = create_random_user @application, true
  end

  after :all do
    @user.delete
    delete_application @application
  end

  it "should be able to retrieve its collection" do
    collection = @user.collection
    collection.should_not be_nil
    u = collection.detect {|u| u.uuid == @user.uuid }
    u.uuid.should eq @user.uuid
  end

  it "should be able to reload its data" do
    old_name = @user.name
    @user.name = 'new name'
    @user.name.should eq 'new name'
    @user.get
    @user.name.should eq old_name
  end

  it "should be able to save" do
    @user.name = 'new name'
    @user.save
    @user.name.should eq 'new name'
  end

  it "should be able to print itself" do
    p = @user.to_s
    p.start_with?('resource').should be_true
  end

  it "should know if it has data" do
    @user.data?.should be_true
    empty = Usergrid::Entity.new 'url', 'url'
    empty.data?.should be_false
  end

  it "should return nil if empty result" do
    @application['foobars'].get.entity.should be_nil
  end

  it "should not serialize reserved attributes" do
    dump = MultiJson.dump @user
    hash = MultiJson.load dump
    Usergrid::Resource::RESERVED.each { |a| hash.should_not have_key(a) }
  end

end
