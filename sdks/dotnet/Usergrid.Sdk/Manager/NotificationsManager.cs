using System;
using System.Collections.Generic;
using RestSharp;
using Usergrid.Sdk.Model;
using Usergrid.Sdk.Payload;

namespace Usergrid.Sdk.Manager
{
    internal class NotificationsManager : ManagerBase, INotificationsManager
    {
        public NotificationsManager(IUsergridRequest request) : base(request)
        {
        }

        public void CreateNotifierForApple(string notifierName, string environment, string p12CertificatePath)
        {
            var formParameters = new Dictionary<string, object>
                {
                    {"name", notifierName},
                    {"provider", "apple"},
                    {"environment", environment}
                };

            var fileParameters = new Dictionary<string, string>
                {
                    {"p12Certificate", p12CertificatePath}
                };


            IRestResponse response = Request.ExecuteMultipartFormDataRequest("/notifiers", Method.POST, formParameters, fileParameters);
            ValidateResponse(response);
        }

        public void CreateNotifierForAndroid(string notifierName, string apiKey)
        {
            IRestResponse response = Request.ExecuteJsonRequest("/notifiers", Method.POST, new AndroidNotifierPayload {ApiKey = apiKey, Name = notifierName});
            ValidateResponse(response);
        }

        public void PublishNotification(IEnumerable<Notification> notifications, INotificationRecipients recipients, NotificationSchedulerSettings schedulerSettings = null)
        {
            var payload = new NotificationPayload();
            foreach (Notification notification in notifications)
            {
                payload.Payloads.Add(notification.NotifierIdentifier, notification.GetPayload());
            }

            if (schedulerSettings != null)
            {
                if (schedulerSettings.DeliverAt != DateTime.MinValue)
                    payload.DeliverAt = schedulerSettings.DeliverAt.ToUnixTime();
                if (schedulerSettings.ExpireAt != DateTime.MinValue)
                    payload.ExpireAt = schedulerSettings.ExpireAt.ToUnixTime();
            }

            string query = recipients.BuildQuery();

            IRestResponse response = Request.ExecuteJsonRequest(query, Method.POST, payload);
            ValidateResponse(response);
        }
    }
}