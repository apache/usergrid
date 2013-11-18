using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace LocationDotNetSample
{
    class Store
    {
        public string uuid { get; set; }
        public string type { get; set; }
        public string name { get; set; }
        public string[] hours { get; set; }
        public location location { get; set; }
        public string pharmacy { get; set; }
        public string[] services { get; set; }
        public string storeNumber { get; set; }
        public string supertarget { get; set; }

    }

    class location
    {
        public string stateCode { get; set; }
        public string latitude { get; set; }
        public string longitude { get; set; }
        public string displayAddress { get; set; }
    }
}
