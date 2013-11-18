using System;
using System.Collections.Generic;

namespace Usergrid.Sdk
{
	public class UsergridCollection<T> : List<T>
	{
		public UsergridCollection() : base() {}
		public UsergridCollection(IEnumerable<T> items) : base(items) {}

		public bool HasNext {get;set;}
		public bool HasPrevious {get;set;}
	}
}

