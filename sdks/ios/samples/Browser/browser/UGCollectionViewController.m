//
//  UGCollectionViewController.m
//  Browser
//

#import "UGHTTPClient.h"
#import "UGHTTPResult.h"
#import "UGConnection.h"
#import "UGCollectionViewController.h"
#import "UGEntityViewController.h"

@interface UGCollectionViewController ()

@end

@implementation UGCollectionViewController

- (id)initWithStyle:(UITableViewStyle)style
{
    self = [super initWithStyle:style];
    if (self) {
        // Custom initialization
    }
    return self;
}

- (void) loadView
{
    [super loadView];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (void) getCollection
{
    [[[UGHTTPClient alloc] initWithRequest:[[UGConnection sharedConnection]
                                            getEntitiesInCollection:self.collection[@"name"]
                                            limit:200]]
     connectWithCompletionHandler:^(UGHTTPResult *result) {
         self.collectionDetail = result.object;
         [self.tableView reloadData];
         self.navigationItem.title = result.object[@"path"];
     }];
}

- (void) viewWillAppear:(BOOL)animated
{
    [self getCollection];
}

#pragma mark - Table view data source

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView
{
    return 1;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
    if (!self.collectionDetail) {
        return 0;
    } else {
        return [self.collectionDetail[@"count"] intValue];
    }
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    UITableViewCell *cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleSubtitle reuseIdentifier:@"Cell"];
    cell.textLabel.font = [UIFont boldSystemFontOfSize:14];
    return cell;
}

- (void) tableView:(UITableView *)tableView willDisplayCell:(UITableViewCell *)cell forRowAtIndexPath:(NSIndexPath *)indexPath
{
    int row = [indexPath row];
    NSDictionary *entity = self.collectionDetail[@"entities"][row];
    cell.textLabel.text = entity[@"name"];
    cell.detailTextLabel.text = entity[@"uuid"];
}



#pragma mark - Table view delegate

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath
{
    UGEntityViewController *entityViewController = [[UGEntityViewController alloc] init];
    int row = [indexPath row];
    entityViewController.entity = self.collectionDetail[@"entities"][row];
    [self.navigationController pushViewController:entityViewController animated:YES];
}

@end
