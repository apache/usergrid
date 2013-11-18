namespace Usergrid.Sdk.Model
{
    public abstract class Notification
    {
        protected Notification(string notifierIdentifier, string message)
        {
            NotifierIdentifier = notifierIdentifier;
            Message = message;
        }

        public string NotifierIdentifier { get; set; }
        public string Message { get; set; }

        internal abstract object GetPayload();
    }
}