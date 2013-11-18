using NUnit.Framework;
using Usergrid.Sdk.Model;

namespace Usergrid.Sdk.IntegrationTests
{
    [TestFixture]
    public class DeviceTests : BaseTest
    {
        [Test]
        public void ShouldCrudDevices()
        {
            const string deviceName = "test_device";
            var client = InitializeClientAndLogin(AuthType.Organization);
            DeleteDeviceIfExists(client, deviceName);

            client.CreateDevice(new UsergridDevice() {Name = deviceName});
            //get device and assert
            UsergridDevice device = client.GetDevice<UsergridDevice>(deviceName);
            Assert.That(device.Name, Is.EqualTo(deviceName));

            //create a custom device
            DeleteDeviceIfExists(client, deviceName);
            const string deviceTypeiPhone = "iPhone";

            client.CreateDevice(new MyCustomUserGridDevice() { Name = deviceName, DeviceType = deviceTypeiPhone });
            //get device and assert
            MyCustomUserGridDevice myCustomDevice = client.GetDevice<MyCustomUserGridDevice>(deviceName);
            Assert.That(myCustomDevice.Name, Is.EqualTo(deviceName));
            Assert.That(myCustomDevice.DeviceType, Is.EqualTo(deviceTypeiPhone));

            //update device type
            const string deviceTypeAndroid = "Android";

            myCustomDevice.DeviceType = deviceTypeAndroid;
            client.UpdateDevice(myCustomDevice);

            //get device and assert
            myCustomDevice = client.GetDevice<MyCustomUserGridDevice>(deviceName);
            Assert.That(myCustomDevice.Name, Is.EqualTo(deviceName));
            Assert.That(myCustomDevice.DeviceType, Is.EqualTo(deviceTypeAndroid));
        }

        public class MyCustomUserGridDevice : UsergridDevice
        {
            public string DeviceType { get; set; }
        }
    }
}