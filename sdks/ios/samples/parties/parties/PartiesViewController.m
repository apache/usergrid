//
//  PartiesViewController.m
//  parties
//
//  Copyright (c) 2013 Apigee. All rights reserved.
//

#import "PartiesViewController.h"
#import "PartyViewController.h"

#import "UGConnection.h"
#import "UGHTTPClient.h"
#import "UGHTTPResult.h"

@interface PartiesViewController ()
@property (nonatomic, strong) NSDictionary *content;
@end

@implementation PartiesViewController

- (id)initWithStyle:(UITableViewStyle)style
{
    self = [super initWithStyle:style];
    if (self) {
        // Custom initialization
    }
    return self;
}

- (void) fetchParties {
    UGConnection *connection = [UGConnection sharedConnection];
    NSLog(@"loading...");
    NSDictionary *query = [connection queryWithString:@"select * where conference = 'wwdc2013'"
                                                limit:1000
                                            startUUID:nil
                                               cursor:nil
                                             reversed:NO];
    UGHTTPClient *client = [[UGHTTPClient alloc] initWithRequest:
                            [connection getEntitiesInCollection:@"parties"
                                                     usingQuery:query]];
    [client connectWithCompletionHandler:^(UGHTTPResult *result) {
        self.content = result.object;
        NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
        [defaults setObject:result.object forKey:@"parties"];
        [defaults synchronize];
        [self.tableView reloadData];
    }];
}

- (void) reload {
    UGConnection *connection = [UGConnection sharedConnection];
    if (![connection isAuthenticated]) {
        [[[UGHTTPClient alloc] initWithRequest:
          [connection getAccessTokenForApplicationWithUsername:@"radtastical"
                                                      password:@"partyrock2013"]]
         connectWithCompletionHandler:^(UGHTTPResult *result) {
             [connection authenticateWithResult:result];
             if ([connection isAuthenticated]) {
                 [self fetchParties];
             }
         }];
    } else {
        [self fetchParties];
    }
}

- (void)loadView
{
    [super loadView];
    self.navigationItem.title = @"Parties";
    
    UGConnection *connection = [UGConnection sharedConnection];
    connection.server = @"https://api.usergrid.com";
    connection.organization = @"macmoe";
    connection.application = @"partyapi";
    
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    NSDictionary *parties = [defaults objectForKey:@"parties"];
    if (parties) {
        self.content = parties;
    } else {
        [self reload];
    }
    self.navigationItem.leftBarButtonItem = [[UIBarButtonItem alloc]
                                             initWithBarButtonSystemItem:UIBarButtonSystemItemRefresh
                                             target:self
                                             action:@selector(reload)];
}

#pragma mark - Table view data source

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView
{
    return 1;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
    return self.content ? [self.content[@"entities"] count] : 0;
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    return [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleSubtitle reuseIdentifier:@"Cell"];
}

#pragma mark - Table view delegate

- (void)tableView:(UITableView *)tableView willDisplayCell:(UITableViewCell *)cell forRowAtIndexPath:(NSIndexPath *)indexPath
{
    if (self.content) {
        id entity = self.content[@"entities"][[indexPath row]];
        cell.textLabel.text = entity[@"title"];
        cell.detailTextLabel.text = entity[@"venue"];
    }
    cell.selectionStyle = UITableViewCellSelectionStyleNone;
}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath
{
    if (self.content) {
        id entity = self.content[@"entities"][[indexPath row]];
        PartyViewController *partyViewController = [[PartyViewController alloc] init];
        partyViewController.content = entity;
        [self.navigationController pushViewController:partyViewController animated:YES];
    }
}

@end
