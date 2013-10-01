using System;
using NUnit.Framework;
using Usergrid.Sdk.Model;

namespace Usergrid.Sdk.IntegrationTests
{
	public class Page
	{
		public int PageNumber {get;set;}
		public string Name {get;set;}
	}

	[TestFixture]
	public class EntityPagingTests : BaseTest
	{
		[Test]
		public void ShouldDoPaging()
		{
			var client = new Client(Organization, Application);
			client.Login(ClientId, ClientSecret, AuthType.Organization);

			for (var i=0; i<20; i++) 
			{
				try{
					client.DeleteEntity ("pages", "page-" + i);
					Console.WriteLine ("Deleted " + i);
				} catch (UsergridException) {
				}
			}

			for (var i=0; i<20; i++) 
			{
				var page = new Page { PageNumber = i, Name = "page-" + i };
				client.CreateEntity ("pages", page);
				Console.WriteLine ("Created " + i);
			}
      

			var collection = client.GetEntities<Page> ("pages", 3);
			Assert.AreEqual (3, collection.Count);
			Assert.IsTrue (collection.HasNext);
			Assert.IsFalse (collection.HasPrevious);
			Assert.AreEqual ("page-1", collection [1].Name);

			collection = client.GetNextEntities<Page> ("pages");
			Assert.IsTrue (collection.HasNext);
			Assert.IsTrue (collection.HasPrevious);
			Assert.AreEqual ("page-4", collection [1].Name);

			collection = client.GetNextEntities<Page> ("pages");
			Assert.IsTrue (collection.HasNext);
			Assert.IsTrue (collection.HasPrevious);
			Assert.AreEqual ("page-7", collection [1].Name);

			collection = client.GetPreviousEntities<Page> ("pages");
			Assert.IsTrue (collection.HasNext);
			Assert.IsTrue (collection.HasPrevious);
			Assert.AreEqual ("page-4", collection [1].Name);

			for (var i=0; i<20; i++) 
			{
				client.DeleteEntity ("pages", "page-" + i);
				Console.WriteLine ("Deleted " + i);
			}
		}
	}
}

