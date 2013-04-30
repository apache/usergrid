//
//  UGAppDelegate.m
//  Browser
//

#import "UGAppDelegate.h"
#import "UGAppViewController.h"

@implementation UGAppDelegate

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
    self.window = [[UIWindow alloc] initWithFrame:[[UIScreen mainScreen] bounds]];
    UINavigationController *navigationController = [[UINavigationController alloc]
                                                    initWithRootViewController:[[UGAppViewController alloc] init]];
    navigationController.navigationBar.tintColor = [UIColor darkGrayColor];
    self.window.rootViewController = navigationController;
    [self.window makeKeyAndVisible];
    return YES;
}

@end
