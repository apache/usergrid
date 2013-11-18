//
//  AppDelegate.m
//  books
//

#import "AppDelegate.h"
#import "BooksViewController.h"

@implementation AppDelegate

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
    self.window = [[UIWindow alloc] initWithFrame:[[UIScreen mainScreen] bounds]];
    UINavigationController *navigationController = [[UINavigationController alloc]
                                                    initWithRootViewController:[[BooksViewController alloc] init]];
    navigationController.navigationBar.tintColor = [UIColor colorWithRed:100.0/255.0
                                                                   green:150.0/255.0
                                                                    blue:200.0/255.0
                                                                   alpha:1];
    self.window.rootViewController = navigationController;
    self.window.backgroundColor = [UIColor whiteColor];
    [self.window makeKeyAndVisible];
    return YES;
}

@end
