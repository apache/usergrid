using System;
using System.Collections.Generic;
using System.Dynamic;
using RestSharp;
using Newtonsoft.Json;

namespace Usergrid.Sdk.Payload
{
    internal class NotificationPayload
    {
        
        public IDictionary<string, object>  Payloads { get; set; }
		[JsonProperty("deliver", NullValueHandling = NullValueHandling.Ignore)]
		public long? DeliverAt {get;set;}
		[JsonProperty("expire", NullValueHandling = NullValueHandling.Ignore)]
		public long? ExpireAt { get; set;}

        public NotificationPayload()
        {
            Payloads = new Dictionary<string, object>();
        }
    }
}