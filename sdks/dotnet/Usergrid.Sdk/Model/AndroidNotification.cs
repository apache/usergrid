using System;
using System.Collections.Generic;
using System.Dynamic;
using RestSharp;
using Usergrid.Sdk.Payload;

namespace Usergrid.Sdk.Model
{

    public class AndroidNotification : Notification
    {
        public AndroidNotification(string notifierIdentifier, string message) : base(notifierIdentifier, message)
        {
        }

        internal override object GetPayload()
        {
            return new {data = Message};
        }
    }
    
}