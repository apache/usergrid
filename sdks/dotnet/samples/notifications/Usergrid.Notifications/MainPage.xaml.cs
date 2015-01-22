using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Runtime.InteropServices.WindowsRuntime;
using Windows.Foundation;
using Windows.Foundation.Collections;
using Windows.Networking.PushNotifications;
using Windows.Storage;
using Windows.UI.Xaml;
using Windows.UI.Xaml.Controls;
using Windows.UI.Xaml.Controls.Primitives;
using Windows.UI.Xaml.Data;
using Windows.UI.Xaml.Input;
using Windows.UI.Xaml.Media;
using Windows.UI.Xaml.Navigation;

// The Blank Page item template is documented at http://go.microsoft.com/fwlink/?LinkId=391641

namespace Usergrid.Notifications
{
    /// <summary>
    /// An empty page that can be used on its own or navigated to within a Frame.
    /// </summary>
    public sealed partial class MainPage : Page
    {
        private Usergrid usergrid;
        private string serverUrl;
        private string org;
        private string app;
        private string notifier;
        private string apiKey;
        private string secret;
        private Guid deviceId;
        public MainPage()
        {
            this.InitializeComponent();
            var myIp = "10.0.1.20";
            serverUrl = "http://" + myIp + ":8080";
            org = "mobile";
            app = "sandbox";
            notifier = "winphone";
            this.NavigationCacheMode = NavigationCacheMode.Required;
            this.setup();
        }

        private async void setup()
        {
            usergrid = new Usergrid(serverUrl, org, app);
            await this.usergrid.Authenticate("superuser", "test", true);
            await usergrid.Push.RegisterDevice(notifier);
           
            
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

        private async void Button_Click(object sender, RoutedEventArgs e)
        {

            var message = this.pushText.Text;
            await usergrid.Push.SendNotification(deviceId,message);
        }

        private void pushText_TextChanged(object sender, TextChangedEventArgs e)
        {

        }

        private async void AddNotifierButton_Click(object sender, RoutedEventArgs e)
        {
            await usergrid.Push.AddNotifier(NotifierName.Text,Sid.Text,Api_Key.Text);
        }
    }
}
