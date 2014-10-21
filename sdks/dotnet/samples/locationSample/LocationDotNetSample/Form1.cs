
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
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;
using Geocoder;
// https://github.com/bmontgomery/Geocoder

namespace LocationDotNetSample
{
    public partial class Form1 : Form
    {
        private Geocoder.Location mapCenter{get; set;}
        private GMap.NET.WindowsForms.Markers.GMapMarkerGoogleGreen currentPosition { get; set; }
        private GMap.NET.WindowsForms.GMapOverlay markerOverlay { get; set; }
        private Usergrid.Sdk.Client client { get; set; }
        private Usergrid.Sdk.UsergridCollection<Store> stores;
        private string orgName = "mmalloy@apigee.com";
        private string appName = "testapp";
        private string collection = "targets";

        public Form1()
        {
            InitializeComponent();
            currentPosition = null;
            client = new Usergrid.Sdk.Client("mmalloy@apigee.com", "testapp");
        }

       
        private void btnSubmit_Click(object sender, EventArgs e)
        {

            string address = "";
            if (txtStreetAddress.Text != "" && txtCity.Text != "" && txtState.Text != "") { address = txtStreetAddress.Text + "," + txtCity.Text + "," + txtState.Text; }
            else if (txtCity.Text != "" && txtState.Text != ""){ address = txtCity.Text + "," + txtState.Text;}
            else if (txtZip.Text != "") { address = txtZip.Text; }

            if (address == "") { MessageBox.Show("Enter a valid address, city, or zip code");}
            else {
                var geocoder = new Geocoder.GeocodeService();
                var location = geocoder.GeocodeLocation(address);
                storeList.Items.Clear();
                txtStoreDetails.Text = "";
                markerOverlay.Markers.Clear(); 
                centerMap(location); 
                markCurrentLocation(location);
                stores = findStores();
                for (int i = 0; i < stores.Count; i++) { markStoreLocation(stores[i]); }
                addStoresToList();

            }
            
        }

        private void centerMap(Geocoder.Location location)
        {
            mapCenter = location;
            var lat = mapCenter.Latitude;
            var lon = mapCenter.Longitude;
            gMap.Position = new GMap.NET.PointLatLng(lat, lon);

        }
        private void markCurrentLocation(Geocoder.Location location)
        {
            var lat = location.Latitude;
            var lon = location.Longitude;
            GMap.NET.WindowsForms.Markers.GMapMarkerGoogleGreen m = new GMap.NET.WindowsForms.Markers.GMapMarkerGoogleGreen(new GMap.NET.PointLatLng(lat, lon));
            currentPosition = m;
            markerOverlay.Markers.Add(m);
            m.ToolTipText = "Current Position";
        }

        private void markStoreLocation(Store store)
        {
            var lat = Convert.ToDouble(store.location.latitude);
            var lon = Convert.ToDouble(store.location.longitude);
            GMap.NET.WindowsForms.Markers.GMapMarkerGoogleRed m = new GMap.NET.WindowsForms.Markers.GMapMarkerGoogleRed(new GMap.NET.PointLatLng(lat, lon));
            string tooltiptext = store.name + "\n" + store.location.displayAddress;
            m.ToolTipText = tooltiptext;
            markerOverlay.Markers.Add(m);
        }

        private void addStoresToList()
        {
            for (int i = 0; i < stores.Count; i++)
            {
                storeList.Items.Add(stores[i].name);
            }
        }

        private void Form1_Load(object sender, EventArgs e)
        {
            this.Text = "Store Finder";
            gMap.MapProvider = GMap.NET.MapProviders.GoogleMapProvider.Instance;
            GMap.NET.GMaps.Instance.Mode = GMap.NET.AccessMode.ServerOnly;
            var geocoder = new Geocoder.GeocodeService();
            var location = geocoder.GeocodeLocation("Palo Alto, CA");
            centerMap(location);
            markerOverlay = new GMap.NET.WindowsForms.GMapOverlay(gMap, "markers");
            gMap.Overlays.Add(markerOverlay);
        }

        private Usergrid.Sdk.UsergridCollection<Store> findStores()
        {
            string latlon = mapCenter.Latitude + "," + mapCenter.Longitude;
            return client.GetEntities<Store>(collection, 20, "location within 15000 of " + latlon);
        }

        private void storeList_SelectedIndexChanged(object sender, EventArgs e)
        {
            Store selectedStore = stores[storeList.SelectedIndex];
            txtStoreDetails.Text = "Store Name: " + selectedStore.name + "\n" + "Hours:";
            for (int j = 0; j < selectedStore.hours.Count(); j++)
            {
                txtStoreDetails.Text += "\n" + selectedStore.hours[j];
            }
            txtStoreDetails.Text += "\n\nServices: ";
            for (int k = 0; k < selectedStore.services.Count(); k++)
            {          
                txtStoreDetails.Text += selectedStore.services[k];
                if (k != selectedStore.services.Count()-1) { txtStoreDetails.Text += ","; }
                if (k != 0 && k % 4 == 0) { txtStoreDetails.Text += "\n"; }
            }
               
        }

    }
}
