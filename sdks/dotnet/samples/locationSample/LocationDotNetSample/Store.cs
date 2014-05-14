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
