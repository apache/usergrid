using System;
using System.Collections.Generic;
using System.Dynamic;
using RestSharp;
using Usergrid.Sdk.Payload;

namespace Usergrid.Sdk.Model
{

    public class AppleNotification : Notification
    {
        public AppleNotification(string notifierIdentifier, string message, string sound = null)
            : base(notifierIdentifier, message)
        {
            Sound = sound;
        }

        public string Sound { get; set; }

        internal override object GetPayload()
        {

            if (Sound != null)
            {
                return new {aps = new {alert = Message, sound = Sound}};
            }
            else
            {
                return Message;
            }
        }
    }

}