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
			var client = new Client(Organization, Application, ApiUri);
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

