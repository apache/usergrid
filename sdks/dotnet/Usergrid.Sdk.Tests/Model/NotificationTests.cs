using NUnit.Framework;
using Usergrid.Sdk.Model;

namespace Usergrid.Sdk.Tests.Model
{
    [TestFixture]
    public class NotificationTests
    {
        [Test]
        public void AndroidNotificationShouldReturnCorrectPayload()
        {
            var notification = new AndroidNotification("notifierName", "notification message");
            var payload = notification.GetPayload();

            Assert.AreEqual("notification message", payload.GetReflectedProperty("data"));
        }

        [Test]
        public void GoogleNotificationShouldReturnCorrectPayloadWithSound()
        {
            var notification = new AppleNotification("notifierName", "notification message", "chime");
            var payload = notification.GetPayload();

            var aps = payload.GetReflectedProperty("aps");
            Assert.IsNotNull(aps);
            Assert.AreEqual("notification message", aps.GetReflectedProperty("alert"));
            Assert.AreEqual("chime", aps.GetReflectedProperty("sound"));
        }

        [Test]
        public void GoogleNotificationShouldReturnCorrectPayloadWithoutSound()
        {
            var notification = new AppleNotification("notifierName", "notification message");
            var payload = notification.GetPayload();

            Assert.AreEqual("notification message", payload);
        }
    }
}