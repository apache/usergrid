using Usergrid.Sdk.Model;

namespace Usergrid.Sdk.Manager
{
    internal interface IAuthenticationManager
    {
        void ChangePassword(string userName, string oldPassword, string newPassword);
        void Login(string loginId, string secret, AuthType authType);
    }
}