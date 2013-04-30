//
//  UGEntityViewController.m
//  Browser
//

#import "UGEntityViewController.h"
#import "UGTextViewController.h"

@interface UGEntityViewController ()

@end

@implementation UGEntityViewController

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
    self.navigationItem.title = self.entity[@"name"];
}

#pragma mark - Table view data source

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView
{
    return 1;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
    return [self.entity count];
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    UITableViewCell *cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleSubtitle reuseIdentifier:@"Cell"];
    return cell;
}

#pragma mark - Table view delegate

- (void) tableView:(UITableView *)tableView willDisplayCell:(UITableViewCell *)cell forRowAtIndexPath:(NSIndexPath *)indexPath
{
    NSArray *keys = [[self.entity allKeys] sortedArrayUsingSelector:@selector(compare:)];
    NSString *key = keys[[indexPath row]];
    cell.textLabel.text = key;
    id entity = self.entity[key];
    cell.detailTextLabel.text = [entity description];
}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath
{
    NSArray *keys = [[self.entity allKeys] sortedArrayUsingSelector:@selector(compare:)];
    NSString *key = keys[[indexPath row]];
    UGTextViewController *textViewController = [[UGTextViewController alloc] init];
    textViewController.key = key;
    textViewController.binding = self.entity;
    [self.navigationController pushViewController:textViewController animated:YES];
}

@end
