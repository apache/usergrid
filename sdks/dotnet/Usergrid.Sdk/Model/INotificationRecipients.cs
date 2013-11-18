using System;

namespace Usergrid.Sdk.Model
{
	public interface INotificationRecipients
	{
		INotificationRecipients AddUserWithName(string name);
		INotificationRecipients AddUserWithUuid(string uuid);
		INotificationRecipients AddUserWithQuery(string query);
		INotificationRecipients AddGroupWithPath(string path);
		INotificationRecipients AddGroupWithQuery(string query);
		INotificationRecipients AddDeviceWithName(string name);
		INotificationRecipients AddDeviceWithQuery(string query);
		string BuildQuery();
	}
}

