//
//  UGAppViewController.m
//  Browser
//

#import "UGHTTPClient.h"
#import "UGHTTPResult.h"
#import "UGConnection.h"
#import "UGAppViewController.h"
#import "UGSignInViewController.h"
#import "UGCollectionViewController.h"

@interface UGAppViewController ()

@end

@implementation UGAppViewController

- (void) loadView
{
    [super loadView];
    self.navigationItem.title = @"Usergrid";
    self.navigationItem.leftBarButtonItem = [[UIBarButtonItem alloc]
                                             initWithTitle:@"Connection"
                                             style:UIBarButtonItemStyleBordered
                                             target:self
                                             action:@selector(connect:)];
}

- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation
{
    if ([[UIDevice currentDevice] userInterfaceIdiom] == UIUserInterfaceIdiomPhone) {
        return (interfaceOrientation != UIInterfaceOrientationPortraitUpsideDown);
    } else {
        return YES;
    }
}

- (void) connect:(id) sender
{
    UGSignInViewController *signinViewController = [[UGSignInViewController alloc] init];
    signinViewController.appViewController = self;
    UINavigationController *signinNavigationController =
    [[UINavigationController alloc] initWithRootViewController:signinViewController];
    signinNavigationController.modalPresentationStyle = UIModalPresentationFormSheet;
    signinNavigationController.navigationBar.tintColor = self.navigationController.navigationBar.tintColor;
    [self presentModalViewController:signinNavigationController animated:YES];
}

- (void) downloadApplicationDescription
{
    [[[UGHTTPClient alloc]
      initWithRequest:[[UGConnection sharedConnection] getApplication] ]
     connectWithCompletionHandler:^(UGHTTPResult *result) {
         self.application = result.object[@"entities"][0];
         [self.tableView reloadData];
     }];
}

- (NSInteger) numberOfSectionsInTableView:(UITableView *)tableView
{
    return self.application ? 2 : 0;
}

- (NSInteger) tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
    if (section == 0) {
        return self.application ? 1 : 0;
    } else if (section == 1) {
        return self.application ? [self.application[@"metadata"][@"collections"] count] : 0;
    } else {
        return 0;
    }
}

- (UITableViewCell *) tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    UITableViewCell *cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleSubtitle reuseIdentifier:@"Cell"];
    cell.backgroundColor = [UIColor whiteColor];
    return cell;
}

- (void) tableView:(UITableView *)tableView willDisplayCell:(UITableViewCell *)cell forRowAtIndexPath:(NSIndexPath *)indexPath
{
    if ([indexPath section] == 0) {
        cell.textLabel.text = self.application[@"name"];
        cell.accessoryType = UITableViewCellAccessoryNone;
    } else {
        NSDictionary *collections = self.application[@"metadata"][@"collections"];
        NSString *key =  [[[collections allKeys] sortedArrayUsingSelector:@selector(compare:)] objectAtIndex:[indexPath row]];
        NSDictionary *object = collections[key];
        cell.textLabel.text = [NSString stringWithFormat:@"%@",
                               object[@"title"]];
        cell.detailTextLabel.text = [NSString stringWithFormat:@"name:%@ type:%@",
                                     object[@"name"],
                                     object[@"type"]];
        cell.accessoryType = UITableViewCellAccessoryDisclosureIndicator;
    }
    cell.backgroundColor = [UIColor whiteColor];
    cell.selectionStyle = UITableViewCellSelectionStyleGray;
}

- (NSString *) tableView:(UITableView *)tableView titleForHeaderInSection:(NSInteger)section
{
    if (section == 0) {
        return @"Application";
    } else {
        return @"Collections";
    }
}

- (void) tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath
{
    [tableView deselectRowAtIndexPath:indexPath animated:YES];
    if ([indexPath section] == 1) {
        NSDictionary *collections = self.application[@"metadata"][@"collections"];
        NSString *key =  [[[collections allKeys] sortedArrayUsingSelector:@selector(compare:)] objectAtIndex:[indexPath row]];
        NSDictionary *collection = collections[key];
        UGCollectionViewController *collectionViewController = [[UGCollectionViewController alloc] init];
        collectionViewController.collection = collection;
        [self.navigationController pushViewController:collectionViewController animated:YES];
    }
}

@end
