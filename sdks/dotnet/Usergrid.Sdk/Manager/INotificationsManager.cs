using System.Collections.Generic;
using Usergrid.Sdk.Model;

namespace Usergrid.Sdk.Manager
{
    internal interface INotificationsManager
    {
        void CreateNotifierForApple(string notifierName, string environment, string p12CertificatePath);
        void CreateNotifierForAndroid(string notifierName, string apiKey);
		void PublishNotification (IEnumerable<Notification> notification, INotificationRecipients recipients, NotificationSchedulerSettings schedulingSettings = null);
    }
}