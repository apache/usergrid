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
using NSubstitute;
using NUnit.Framework;
using Usergrid.Sdk.Manager;
using Usergrid.Sdk.Model;
using Usergrid.Sdk.Payload;

namespace Usergrid.Sdk.Tests.ClientTests
{
    [TestFixture]
    public class NotificationTests
    {
        #region Setup/Teardown

        [SetUp]
        public void Setup()
        {
            _notificationsManager = Substitute.For<INotificationsManager>();
            _entityManager = Substitute.For<IEntityManager>();
            _client = new Client(null, null) {NotificationsManager = _notificationsManager, EntityManager = _entityManager};
        }

        #endregion

        private INotificationsManager _notificationsManager;
        private IEntityManager _entityManager;
        private IClient _client;

        [Test]
        public void CancelNotificationShouldDelegateToEntityManagerWithCorrectParameters()
        {
            _client.CancelNotification("notificationIdentifier");

            _entityManager.Received(1).UpdateEntity("/notifications", "notificationIdentifier", Arg.Is<CancelNotificationPayload>(p => p.Canceled));
        }

        [Test]
        public void CreateNotifierForAndroidShouldDelegateToNotificationsManager()
        {
            _client.CreateNotifierForAndroid("notifierName", "apiKey");

            _notificationsManager.Received(1).CreateNotifierForAndroid("notifierName", "apiKey");
        }

        [Test]
        [ExpectedException(ExpectedException = typeof (UsergridException), ExpectedMessage = "Exception message")]
        public void CreateNotifierForAndroidShouldPassOnTheException()
        {
            _notificationsManager
                .When(m => m.CreateNotifierForAndroid(Arg.Any<string>(), Arg.Any<string>()))
                .Do(m => { throw new UsergridException(new UsergridError {Description = "Exception message"}); });

            _client.CreateNotifierForAndroid(null, null);
        }

        [Test]
        public void CreateNotifierForAppleShouldDelegateToNotificationsManager()
        {
            _client.CreateNotifierForApple("notifierName", "development", "certificateFilePath");

            _notificationsManager.Received(1).CreateNotifierForApple("notifierName", "development", "certificateFilePath");
        }

        [Test]
        [ExpectedException(ExpectedException = typeof (UsergridException), ExpectedMessage = "Exception message")]
        public void CreateNotifierForAppleShouldPassOnTheException()
        {
            _notificationsManager
                .When(m => m.CreateNotifierForApple(Arg.Any<string>(), Arg.Any<string>(), Arg.Any<string>()))
                .Do(m => { throw new UsergridException(new UsergridError {Description = "Exception message"}); });

            _client.CreateNotifierForApple(null, null, null);
        }

        [Test]
        public void DeleteNotifierShouldDelegateToEntityManagerWithCorrectParameters()
        {
            _client.DeleteNotifier("notifierIdentifier");

            _entityManager.Received(1).DeleteEntity("/notifiers", "notifierIdentifier");
        }

        [Test]
        public void GetNotifierShouldDelegateToEntityManagerWithCorrectParameters()
        {
            _client.GetNotifier<UsergridNotifier>("notifierIdentifier");

            _entityManager.Received(1).GetEntity<UsergridNotifier>("/notifiers", "notifierIdentifier");
        }

        [Test]
        [ExpectedException(ExpectedException = typeof (UsergridException), ExpectedMessage = "Exception message")]
        public void GetNotifierShouldPassOnTheException()
        {
            _entityManager
                .When(m => m.GetEntity<UsergridNotifier>(Arg.Any<string>(), Arg.Any<string>()))
                .Do(m => { throw new UsergridException(new UsergridError {Description = "Exception message"}); });

            _client.GetNotifier<UsergridNotifier>(null);
        }

        [Test]
        public void GetNotifierShouldReturnNullForUnexistingNotifier()
        {
            _entityManager.GetEntity<UsergridNotifier>("/notifiers", "notifierIdentifier").Returns(x => null);

            var returnedEntity = _client.GetNotifier<UsergridNotifier>("notifierIdentifier");

            Assert.IsNull(returnedEntity);
        }

        [Test]
        public void GetNotifierShouldReturnUsergridNotifierFromEntityManager() {
            var entity = new UsergridNotifier();
            _entityManager.GetEntity<UsergridNotifier>("/notifiers", "notifierIdentifier").Returns(x => entity);

            var returnedEntity = _client.GetNotifier<UsergridNotifier>("notifierIdentifier");

            Assert.AreEqual(entity, returnedEntity);
        }

        [Test]
        public void PublishNotificationShouldDelegateToNotificationsManagerWithCorrectParameters()
        {
            var notifications = new Notification[] {new AppleNotification("notifierName", "message", "chime")};
            INotificationRecipients recipients = new NotificationRecipients().AddUserWithName("username");
            var schedulerSettings = new NotificationSchedulerSettings {DeliverAt = DateTime.Now.AddDays(1)};

            _client.PublishNotification(notifications, recipients, schedulerSettings);

            _notificationsManager.Received(1).PublishNotification(notifications, recipients, schedulerSettings);
        }
    }
}
