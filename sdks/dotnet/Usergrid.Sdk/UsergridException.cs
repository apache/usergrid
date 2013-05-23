using System;
using Usergrid.Sdk.Model;

namespace Usergrid.Sdk
{
    public class UsergridException : Exception
    {
        public UsergridException(UsergridError error) : base(error.Description)
        {
            ErrorCode = error.Error;
        }

        public string ErrorCode { get; set; }
    }
}