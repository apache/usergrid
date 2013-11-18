using NSubstitute;
using NUnit.Framework;
using Usergrid.Sdk.Manager;
using Usergrid.Sdk.Model;

namespace Usergrid.Sdk.Tests.ClientTests
{
    [TestFixture]
    public class DeviceTests
    {
        #region Setup/Teardown

        [SetUp]
        public void Setup()
        {
            _entityManager = Substitute.For<IEntityManager>();
            _client = new Client(null, null) {EntityManager = _entityManager};
        }

        #endregion

        private IEntityManager _entityManager;
        private IClient _client;

        [Test]
        public void CreateDeviceShouldDelegateToEntityManagerWithCorrectCollectionNameAndDevice()
        {
            var device = new UsergridDevice();

            _client.CreateDevice(device);

            _entityManager.Received(1).CreateEntity("devices", device);
        }

        [Test]
        [ExpectedException(ExpectedException = typeof (UsergridException), ExpectedMessage = "Exception message")]
        public void CreateDeviceShouldPassOnTheException()
        {
            _entityManager
                .When(m => m.CreateEntity("devices", Arg.Any<UsergridDevice>()))
                .Do(m => { throw new UsergridException(new UsergridError {Description = "Exception message"}); });

            _client.CreateDevice<UsergridDevice>(null);
        }

        [Test]
        public void DeleteDeviceShouldDelegateToEntityManagerrWithCorrectCollectionNameAndIdentfier()
        {
            _client.DeleteDevice("deviceName");

            _entityManager.Received(1).DeleteEntity("devices", "deviceName");
        }

        [Test]
        [ExpectedException(ExpectedException = typeof (UsergridException), ExpectedMessage = "Exception message")]
        public void DeleteDeviceShouldPassOnTheException()
        {
            _entityManager
                .When(m => m.DeleteEntity("devices", Arg.Any<string>()))
                .Do(m => { throw new UsergridException(new UsergridError {Description = "Exception message"}); });

            _client.DeleteDevice(null);
        }

        [Test]
        public void GetDeviceShouldDelegateToEntityManagerWithCorrectCollectionNameAndIdentifier()
        {
            _client.GetDevice<UsergridDevice>("identifier");

            _entityManager.Received(1).GetEntity<UsergridDevice>("devices", "identifier");
        }

        [Test]
        [ExpectedException(ExpectedException = typeof (UsergridException), ExpectedMessage = "Exception message")]
        public void GetDeviceShouldPassOnTheException()
        {
            _entityManager
                .When(m => m.GetEntity<UsergridDevice>("devices", Arg.Any<string>()))
                .Do(m => { throw new UsergridException(new UsergridError {Description = "Exception message"}); });

            _client.GetDevice<UsergridDevice>(null);
        }

        [Test]
        public void GetDeviceShouldReturnNullForUnexistingDevice()
        {
            _entityManager.GetEntity<UsergridDevice>("devices", "identifier").Returns(x => null);

            var usergridDevice = _client.GetDevice<UsergridDevice>("identifier");

            Assert.IsNull(usergridDevice);
        }

        [Test]
        public void GetDeviceShouldReturnUsergridDevice()
        {
            var usergridDevice = new UsergridDevice();
            _entityManager.GetEntity<UsergridDevice>("devices", "identifier").Returns(x => usergridDevice);

            var returnedDevice = _client.GetDevice<UsergridDevice>("identifier");

            Assert.AreEqual(usergridDevice, returnedDevice);
        }

        [Test]
        public void UpdateDeviceShouldDelegateToEntityManagerrWithCorrectCollectionNameAndDeviceNameAsTheIdentifier()
        {
            var device = new UsergridDevice {Name = "deviceName"};

            _client.UpdateDevice(device);

            _entityManager.Received(1).UpdateEntity("devices", device.Name, device);
        }

        [Test]
        [ExpectedException(ExpectedException = typeof (UsergridException), ExpectedMessage = "Exception message")]
        public void UpdateDeviceShouldPassOnTheException()
        {
            var device = new UsergridDevice {Name = "deviceName"};

            _entityManager
                .When(m => m.UpdateEntity("devices", device.Name, device))
                .Do(m => { throw new UsergridException(new UsergridError {Description = "Exception message"}); });

            _client.UpdateDevice(device);
        }
    }
}