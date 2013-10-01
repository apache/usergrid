using System;
using Newtonsoft.Json;

namespace Usergrid.Sdk.Model
{
	public class NotificationSchedulerSettings
	{
		[JsonProperty("deliver")]
		public DateTime DeliverAt {get;set;}
		[JsonProperty("expire")]
		public DateTime ExpireAt { get; set;}

		public NotificationSchedulerSettings()
		{
			DeliverAt = DateTime.MinValue;
			ExpireAt = DateTime.MinValue;
		}
	}
}

