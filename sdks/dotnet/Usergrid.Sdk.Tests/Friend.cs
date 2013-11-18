using System.Collections.Generic;
using System.Net;
using NSubstitute;
using NUnit.Framework;
using RestSharp;
using Usergrid.Sdk.Model;
using Usergrid.Sdk.Payload;

namespace Usergrid.Sdk.Tests
{

    public class Friend
    {
        public string Name { get; set; }
        public int Age { get; set; }
    }

   
}