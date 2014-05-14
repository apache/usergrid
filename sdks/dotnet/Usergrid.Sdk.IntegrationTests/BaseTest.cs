// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements.  See the NOTICE file distributed with
// this work for additional information regarding copyright ownership.
// The ASF licenses this file to You under the Apache License, Version 2.0
// (the "License"); you may not use this file except in compliance with
// the License.  You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

using System;
using System.Configuration;
using Usergrid.Sdk.Model;

namespace Usergrid.Sdk.IntegrationTests
{
	public class BaseTest
	{
		private static Random random = new Random(DateTime.Now.Millisecond);

	    private readonly Configuration _config;
	    public BaseTest()
	    {
            var configMap = new ExeConfigurationFileMap {ExeConfigFilename = "MySettings.config"};
			Configuration config = ConfigurationManager.OpenMappedExeConfiguration(configMap, ConfigurationUserLevel.None);
	        if (config.HasFile && config.AppSettings.Settings.Count > 0)
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

		protected string ApplicationId
		{
			get {return GetAppSetting("applicationId");}
		}

		protected string ApplicationSecret
		{
			get {return GetAppSetting ("applicationSecret");}
		}

		protected string UserId
		{
			get { return GetAppSetting("userId");}
		}

		protected string UserSecret
		{
			get{ return GetAppSetting("userSecret");}
		}

        protected string P12CertificatePath
		{
            get { return GetAppSetting("p12CertificatePath"); }
		}

        protected string GoogleApiKey
		{
            get { return GetAppSetting("googleApiKey"); }
		}

        private string GetAppSetting(string key)
        {
            return _config == null ? ConfigurationManager.AppSettings[key] : _config.AppSettings.Settings[key].Value;
        }

        protected IClient InitializeClientAndLogin(AuthType authType)
        {
            var client = new Client(Organization, Application);
            if (authType == AuthType.Application || authType == AuthType.Organization)
                client.Login(ClientId, ClientSecret, authType);
            else if (authType == AuthType.User)
                client.Login(UserId, UserSecret, authType);

            return client;
        }

		protected static int GetRandomInteger(int minValue, int maxValue)
		{
			return random.Next (minValue, maxValue);
		}

	    protected void DeleteEntityIfExists<TEntity>(IClient client, string collectionName, string entityIdentifier)
	    {
	        TEntity customer = client.GetEntity<TEntity>(collectionName, entityIdentifier);

	        if (customer != null)
	            client.DeleteEntity(collectionName, entityIdentifier);
	    }

	    protected void DeleteDeviceIfExists(IClient client, string deviceName)
	    {
	        UsergridDevice usergridDevice = client.GetDevice<UsergridDevice>(deviceName);
	        if (usergridDevice != null)
	            client.DeleteDevice(usergridDevice.Uuid);
	    }

        protected void DeleteUserIfExists(IClient client, string userName)
	    {
            UsergridUser existingUser = client.GetUser<UsergridUser>(userName);
            if (existingUser != null)
                client.DeleteUser(existingUser.Uuid);
        }

        protected UsergridUser SetupUsergridUser(IClient client, UsergridUser user)
	    {
            DeleteUserIfExists(client, user.UserName);
            client.CreateUser(user);
            return client.GetUser<UsergridUser>(user.UserName);
	    }
        
        protected UsergridGroup SetupUsergridGroup(IClient client, UsergridGroup @group)
	    {
	        var existingGroup = client.GetGroup<UsergridGroup>(@group.Path);
            if (existingGroup != null)
                client.DeleteGroup(existingGroup.Path);
            client.CreateGroup(@group);
            return client.GetGroup<UsergridGroup>(@group.Path);

	    }
	}
}

