using System;
using System.Configuration;

namespace Usergrid.Sdk.IntegrationTests
{
	public class BaseTest
	{
	    private readonly Configuration _config;
	    public BaseTest()
	    {
            var configMap = new ExeConfigurationFileMap {ExeConfigFilename = "MySettings.config"};
	        Configuration config = ConfigurationManager.OpenMappedExeConfiguration(configMap, ConfigurationUserLevel.None);
	        if (config.HasFile)
	            _config = config;
	    }

	    protected string Organization
		{
			get{ return GetAppSetting("organization");}
		}

		protected string Application
		{
			get{ return GetAppSetting("application");}
		}

		protected string ClientId
		{
			get { return GetAppSetting("clientId");}
		}

		protected string ClientSecret
		{
			get{ return GetAppSetting("clientSecret");}
		}

		protected string UserId
		{
			get { return GetAppSetting("userId");}
		}

		protected string UserSecret
		{
			get{ return GetAppSetting("userSecret");}
		}

        private string GetAppSetting(string key)
        {
            return _config == null ? ConfigurationManager.AppSettings[key] : _config.AppSettings.Settings[key].Value;
        }
	}
}

