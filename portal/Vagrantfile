Vagrant::Config.run do |config|
  puppet_dir = ".puppet"

  config.vm.box = "precise64"
  config.vm.box_url = "http://files.vagrantup.com/precise64.box"
  config.vm.host_name = "usergrid.dev"
  config.vm.network :hostonly, "10.10.4.22"
  config.vm.share_folder "templates", "/tmp/vagrant-puppet/templates", File.join(puppet_dir, "templates")

  config.vm.provision :shell, :path => File.join(puppet_dir, "bootstrap.sh")

  config.vm.provision :puppet do |puppet|
      puppet.module_path = File.join(puppet_dir, "modules")
      puppet.manifests_path = File.join(puppet_dir, "manifests")
      puppet.manifest_file = "main.pp"

      puppet.options = [
        "--templatedir", "/tmp/vagrant-puppet/templates",
        "--verbose",
        "--debug"
      ]
    end
end
