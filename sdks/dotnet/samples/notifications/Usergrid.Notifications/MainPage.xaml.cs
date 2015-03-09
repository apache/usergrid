/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  *  contributor license agreements.  The ASF licenses this file to You
 *  * under the Apache License, Version 2.0 (the "License"); you may not
 *  * use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.  For additional information regarding
 *  * copyright in this work, please see the NOTICE file in the top level
 *  * directory of this distribution.
 *
 */
using System;
using System.Threading.Tasks;
using Usergrid.Notifications.Client;
using Windows.UI.Xaml;
using Windows.UI.Xaml.Controls;
using Windows.UI.Xaml.Navigation;

// The Blank Page item template is documented at http://go.microsoft.com/fwlink/?LinkId=391641

namespace Usergrid.Notifications
{
    /// <summary>
    /// An empty page that can be used on its own or navigated to within a Frame.
    /// </summary>
    public sealed partial class MainPage : Page
    {
        private IUsergridClient usergrid;
        private string serverUrl;
        private string org;
        private string app;
        private string notifier;
        private string user;
        private string password;
        public MainPage()
        {
            this.InitializeComponent();
            //TODO: change me to your server
            serverUrl = "http://server/";
            //TODO: change me to your org
            org = "test-organization";
            //TODO: change me to your app
            app = "test-app";
            //TODO: change me to your notifier name
            notifier = "windows";
            //TODO: change me to your user
            user = "test";
            //TODO: change me to your password
            password = "test";
            this.NavigationCacheMode = NavigationCacheMode.Required;
            usergrid = new Client.Usergrid(serverUrl, org, app, user, password, notifier);

        }

        private async Task setup()
        {
        }

        /// <summary>
        /// Invoked when this page is about to be displayed in a Frame.
        /// </summary>
        /// <param name="e">Event data that describes how this page was reached.
        /// This parameter is typically used to configure the page.</param>
        protected override void OnNavigatedTo(NavigationEventArgs e)
        {
            // TODO: Prepare page for display here.

            // TODO: If your application contains multiple pages, ensure that you are
            // handling the hardware Back button by registering for the
            // Windows.Phone.UI.Input.HardwareButtons.BackPressed event.
            // If you are using the NavigationHelper provided by some templates,
            // this event is handled for you.
        }

        private void pushText_TextChanged(object sender, TextChangedEventArgs e)
        {

        }

        private async void Button_Click(object sender, RoutedEventArgs e)
        {
            Result.Text = "Sending....";

            var message = this.pushText.Text;
            if (await usergrid.Push.SendToast(message))
            {
                Result.Text = "It did! :)";
            }
            else
            {
                Result.Text = "It did not! :(";
            }
        }

        private async void Badge_Click(object sender, RoutedEventArgs e)
        {
            Result.Text = "Sending....";

            if (await usergrid.Push.SendBadge<int>(new Random().Next(1,100)))
            {
                Result.Text = "It did! :)";
            }
            else
            {
                Result.Text = "It did not! :(";
            }
        }

        private void Result_SelectionChanged(object sender, RoutedEventArgs e)
        {

        }

        public AggregateException LastException { get; set; }

        private async void Button_Click_1(object sender, RoutedEventArgs e)
        {
            Result.Text = "Sending....";

            var message = this.pushText.Text;
            if (await usergrid.Push.SendRaw(message))
            {
                Result.Text = "It did! :)";
            }
            else
            {
                Result.Text = "It did not! :(";
            }
        }
    }
}
