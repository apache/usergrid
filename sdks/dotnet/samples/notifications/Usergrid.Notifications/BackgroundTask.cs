using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Windows.ApplicationModel.Background;

namespace Usergrid.Notifications
{
    public sealed class ExampleBackgroundTask : IBackgroundTask
    {
        private static BackgroundTaskRegistration task;
        public void Run(IBackgroundTaskInstance taskInstance)
        {
            var test = "";

        }
        public static void Register()
        {
             
            foreach (var iter in BackgroundTaskRegistration.AllTasks)
            {
                IBackgroundTaskRegistration mytask = iter.Value;
                if (mytask.Name == "ExampleBackgroundTask")
                {

                    mytask.Unregister(true);
                    break;
                }
            }

           
            var builder = new BackgroundTaskBuilder();
            PushNotificationTrigger trigger = new PushNotificationTrigger();
            builder.SetTrigger(trigger);
            builder.Name = "ExampleBackgroundTask";
            builder.TaskEntryPoint = "Usergrid.Notifications.ExampleBackgroundTask";

            ExampleBackgroundTask.task = builder.Register();
            task.Progress += task_Progress;

            task.Completed += task_Completed;

        }

        static void task_Progress(BackgroundTaskRegistration sender, BackgroundTaskProgressEventArgs args)
        {

            var test = "done";
        }

        static void task_Completed(BackgroundTaskRegistration sender, BackgroundTaskCompletedEventArgs args)
        {
            var test = "done";
        }
    }
}
