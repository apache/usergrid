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

using System.Collections.Generic;
using Usergrid.Sdk.Model;

namespace Usergrid.Sdk
{
    public interface IClient
    {
        void Login(string loginId, string secret, AuthType authType);
        T CreateEntity<T>(string collection, T entity);
        void DeleteEntity(string collection, string name);
        void UpdateEntity<T>(string collection, string identifier, T entity);
        T GetEntity<T>(string collectionName, string identifer);
        
        T GetUser<T>(string identifer /*username or uuid or email*/) where T : UsergridUser;
        void CreateUser<T>(T user) where T : UsergridUser;
        void UpdateUser<T>(T user) where T : UsergridUser;
        void DeleteUser(string identifer /*username or uuid or email*/);
        void ChangePassword(string identifer /*username or uuid or email*/, string oldPassword, string newPassword);
        void CreateGroup<T>(T group) where T : UsergridGroup;
        void DeleteGroup(string path);
        T GetGroup<T>(string identifer /*uuid or path*/) where T : UsergridGroup;
        void UpdateGroup<T>(T group) where T : UsergridGroup;
        void AddUserToGroup(string groupIdentifier, string userName);
        void DeleteUserFromGroup(string groupIdentifier, string userIdentifier);
        IList<T> GetAllUsersInGroup<T>(string groupName) where T : UsergridUser;
        
        UsergridCollection<T> GetEntities<T>(string collection, int limit = 10, string query = null);
        UsergridCollection<T> GetNextEntities<T>(string collection, string query = null);
        UsergridCollection<T> GetPreviousEntities<T>(string collection, string query = null);

        void CreateConnection(Connection connection);
        IList<UsergridEntity> GetConnections(Connection connection);
        IList<TConnectee> GetConnections<TConnectee>(Connection connection);
        void DeleteConnection(Connection connection);
        
        void PostActivity<T>(string userIdentifier, T activity) where T:UsergridActivity;
        void PostActivityToGroup<T>(string groupIdentifier, T activity) where T:UsergridActivity;
        void PostActivityToUsersFollowersInGroup<T>(string userIdentifier, string groupIdentifier, T activity) where T:UsergridActivity;
        UsergridCollection<T> GetUserActivities<T>(string userIdentifier) where T:UsergridActivity;
        UsergridCollection<T> GetGroupActivities<T>(string groupIdentifier) where T:UsergridActivity;
        UsergridCollection<T> GetUserFeed<T>(string userIdentifier) where T : UsergridActivity;
        UsergridCollection<T> GetGroupFeed<T>(string groupIdentifier) where T : UsergridActivity;


        void CreateNotifierForApple(string notifierName, string environment, string p12CertificatePath);
        void CreateNotifierForAndroid(string notifierName, string apiKey);
        T GetNotifier<T>(string identifer/*uuid or notifier name*/) where T : UsergridNotifier;
        void DeleteNotifier(string notifierName);
        
        
        T GetDevice<T>(string identifer) where T : UsergridDevice;
        void UpdateDevice<T>(T device) where T : UsergridDevice;
        void CreateDevice<T>(T device) where T : UsergridDevice;
        void DeleteDevice(string identifer);
        void PublishNotification (IEnumerable<Notification> notifications, INotificationRecipients recipients, NotificationSchedulerSettings schedulerSettings = null );
        void CancelNotification(string notificationIdentifier);
    }
}
