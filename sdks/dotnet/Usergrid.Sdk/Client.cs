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
using System.Net;
using Newtonsoft.Json;
using RestSharp;
using Usergrid.Sdk.Manager;
using Usergrid.Sdk.Model;
using Usergrid.Sdk.Payload;
using AuthenticationManager = Usergrid.Sdk.Manager.AuthenticationManager;

namespace Usergrid.Sdk {
    public class Client : IClient {
        private const string UserGridEndPoint = "http://api.usergrid.com";
        private readonly IUsergridRequest _request;

        private IAuthenticationManager _authenticationManager;
        private IConnectionManager _connectionManager;
        private IEntityManager _entityManager;
        private INotificationsManager _notificationsManager;

        public Client(string organization, string application)
            : this(organization, application, UserGridEndPoint, new UsergridRequest(UserGridEndPoint, organization, application)) {}

        public Client(string organization, string application, string uri = UserGridEndPoint)
            : this(organization, application, uri, new UsergridRequest(uri, organization, application)) {}

        internal Client(string organization, string application, string uri = UserGridEndPoint, IUsergridRequest request = null) {
            _request = request ?? new UsergridRequest(uri, organization, application);
        }

        internal IAuthenticationManager AuthenticationManager {
            get { return _authenticationManager ?? (_authenticationManager = new AuthenticationManager(_request)); }
            set { _authenticationManager = value; }
        }

        internal IEntityManager EntityManager {
            get { return _entityManager ?? (_entityManager = new EntityManager(_request)); }
            set { _entityManager = value; }
        }

        internal IConnectionManager ConnectionManager {
            get { return _connectionManager ?? (_connectionManager = new ConnectionManager(_request)); }
            set { _connectionManager = value; }
        }

        internal INotificationsManager NotificationsManager {
            get { return _notificationsManager ?? (_notificationsManager = new NotificationsManager(_request)); }
            set { _notificationsManager = value; }
        }

        public void Login(string loginId, string secret, AuthType authType) {
            AuthenticationManager.Login(loginId, secret, authType);
        }

        public T CreateEntity<T>(string collection, T entity) {
            return EntityManager.CreateEntity(collection, entity);
        }

        public void DeleteEntity(string collection, string name) {
            EntityManager.DeleteEntity(collection, name);
        }

        public void UpdateEntity<T>(string collection, string identifier, T entity) {
            EntityManager.UpdateEntity(collection, identifier, entity);
        }

        public T GetEntity<T>(string collectionName, string entityIdentifier) {
            return EntityManager.GetEntity<T>(collectionName, entityIdentifier);
        }

        public T GetUser<T>(string identifer /*username or uuid or email*/) where T : UsergridUser {
            var user = GetEntity<T>("users", identifer);
            if (user == null)
                return null;

            return user;
        }

        public void CreateUser<T>(T user) where T : UsergridUser {
            CreateEntity("users", user);
        }

        public void UpdateUser<T>(T user) where T : UsergridUser {
            UpdateEntity("users", user.UserName, user);
        }

        public void DeleteUser(string identifer /*username or uuid or email*/) {
            DeleteEntity("users", identifer);
        }

        public void ChangePassword(string identifer /*username or uuid or email*/, string oldPassword, string newPassword) {
            AuthenticationManager.ChangePassword(identifer, oldPassword, newPassword);
        }

        public void CreateGroup<T>(T group) where T : UsergridGroup {
            CreateEntity("groups", group);
        }

        public void DeleteGroup(string path) {
            DeleteEntity("groups", path);
        }

        public T GetGroup<T>(string identifer /*uuid or path*/) where T : UsergridGroup {
            var usergridGroup = EntityManager.GetEntity<T>("groups", identifer);
            if (usergridGroup == null)
                return null;

            return usergridGroup;
        }

        public void UpdateGroup<T>(T group) where T : UsergridGroup {
            UpdateEntity("groups", group.Path, group);
        }

        public void AddUserToGroup(string groupIdentifier, string userName) {
            EntityManager.CreateEntity<object>(string.Format("/groups/{0}/users/{1}", groupIdentifier, userName), null);
        }

        public void DeleteUserFromGroup(string groupIdentifier, string userIdentifier) {
            DeleteEntity("/groups/" + groupIdentifier + "/users", userIdentifier);
        }

        public IList<T> GetAllUsersInGroup<T>(string groupName) where T : UsergridUser {
            IRestResponse response = _request.ExecuteJsonRequest(string.Format("/groups/{0}/users", groupName), Method.GET);
            ValidateResponse(response);

            var responseObject = JsonConvert.DeserializeObject<UsergridGetResponse<T>>(response.Content);
            return responseObject.Entities;
        }

        public UsergridCollection<T> GetEntities<T>(string collection, int limit = 10, string query = null) {
            return EntityManager.GetEntities<T>(collection, limit, query);
        }

        public UsergridCollection<T> GetNextEntities<T>(string collection, string query = null) {
            return EntityManager.GetNextEntities<T>(collection, query);
        }

        public UsergridCollection<T> GetPreviousEntities<T>(string collection, string query = null) {
            return EntityManager.GetPreviousEntities<T>(collection, query);
        }

        public void CreateConnection(Connection connection) {
            ConnectionManager.CreateConnection(connection);
        }
        public IList<UsergridEntity> GetConnections(Connection connection) {
            return ConnectionManager.GetConnections(connection);
        }
        public IList<TConnectee> GetConnections<TConnectee>(Connection connection) {
            return ConnectionManager.GetConnections<TConnectee>(connection);
        }
        public void DeleteConnection(Connection connection) {
            ConnectionManager.DeleteConnection(connection);
        }

        public void PostActivity<T>(string userIdentifier, T activity) where T : UsergridActivity {
            string collection = string.Format("/users/{0}/activities", userIdentifier);
            EntityManager.CreateEntity(collection, activity);
        }

        public void PostActivityToGroup<T>(string groupIdentifier, T activity) where T : UsergridActivity {
            string collection = string.Format("/groups/{0}/activities", groupIdentifier);
            EntityManager.CreateEntity(collection, activity);
        }

        public void PostActivityToUsersFollowersInGroup<T>(string userIdentifier, string groupIdentifier, T activity) where T : UsergridActivity {
            string collection = string.Format("/groups/{0}/users/{1}/activities", groupIdentifier, userIdentifier);
            EntityManager.CreateEntity(collection, activity);
        }

        public UsergridCollection<T> GetUserActivities<T>(string userIdentifier) where T : UsergridActivity {
            string collection = string.Format("/users/{0}/activities", userIdentifier);
            return EntityManager.GetEntities<T>(collection);
        }

        public UsergridCollection<T> GetGroupActivities<T>(string groupIdentifier) where T : UsergridActivity {
            string collection = string.Format("/groups/{0}/activities", groupIdentifier);
            return EntityManager.GetEntities<T>(collection);
        }

        public T GetDevice<T>(string identifer) where T : UsergridDevice {
            var device = GetEntity<T>("devices", identifer);
            if (device == null)
                return null;

            return device;
        }

        public void UpdateDevice<T>(T device) where T : UsergridDevice {
            UpdateEntity("devices", device.Name, device);
        }

        public void CreateDevice<T>(T device) where T : UsergridDevice {
            CreateEntity("devices", device);
        }

        public void DeleteDevice(string identifer) {
            DeleteEntity("devices", identifer);
        }

        public void CreateNotifierForApple(string notifierName, string environment, string p12CertificatePath) {
            NotificationsManager.CreateNotifierForApple(notifierName, environment, p12CertificatePath);
        }

        public void CreateNotifierForAndroid(string notifierName, string apiKey) {
            NotificationsManager.CreateNotifierForAndroid(notifierName, apiKey);
        }

        public T GetNotifier<T>(string identifer /*uuid or notifier name*/) where T : UsergridNotifier {
            var usergridNotifier = EntityManager.GetEntity<T>("/notifiers", identifer);
            if (usergridNotifier == null)
                return null;

            return usergridNotifier;
        }

        public void DeleteNotifier(string notifierName) {
            EntityManager.DeleteEntity("/notifiers", notifierName);
        }

        public void PublishNotification(IEnumerable<Notification> notifications, INotificationRecipients recipients, NotificationSchedulerSettings schedulerSettings = null) {
            NotificationsManager.PublishNotification(notifications, recipients, schedulerSettings);
        }

        public void CancelNotification(string notificationIdentifier) {
            EntityManager.UpdateEntity("/notifications", notificationIdentifier, new CancelNotificationPayload {Canceled = true});
        }

        //TODO: IList?
        public UsergridCollection<T> GetUserFeed<T>(string userIdentifier) where T : UsergridActivity {
            string collection = string.Format("/users/{0}/feed", userIdentifier);
            return EntityManager.GetEntities<T>(collection);
        }

        public UsergridCollection<T> GetGroupFeed<T>(string groupIdentifier) where T : UsergridActivity {
            string collection = string.Format("/groups/{0}/feed", groupIdentifier);
            return EntityManager.GetEntities<T>(collection);
        }


        private static void ValidateResponse(IRestResponse response) {
            if (response.StatusCode != HttpStatusCode.OK) {
                var userGridError = JsonConvert.DeserializeObject<UsergridError>(response.Content);
                throw new UsergridException(userGridError);
            }
        }
    }
}
