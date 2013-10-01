namespace Usergrid.Sdk.Payload
{
    internal class AndroidNotifierPayload
    {
        public string Name { get; set; }

        public string Provider
        {
            get { return "google"; }
        }

        public string ApiKey { get; set; }
    }
}