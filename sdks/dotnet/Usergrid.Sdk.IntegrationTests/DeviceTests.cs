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
