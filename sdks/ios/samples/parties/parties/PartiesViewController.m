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
@property (nonatomic, strong) NSMutableDictionary *images;
@end

@implementation PartiesViewController

- (id)initWithStyle:(UITableViewStyle)style
{
    self = [super initWithStyle:style];
    if (self) {
        self.images = [NSMutableDictionary dictionary];
    }
    return self;
}

- (void) fetchAssetForParty:(NSString *) partyName atIndexPath:(NSIndexPath *) indexPath
{
    UGConnection *connection = [UGConnection sharedConnection];
    if ([connection isAuthenticated]) {
        NSDictionary *query = [connection queryWithString:
                               [NSString stringWithFormat:@"select * where path='/assets/%@'", partyName]
                                                    limit:1
                                                startUUID:nil
                                                   cursor:nil
                                                 reversed:NO];
        UGHTTPClient *client = [[UGHTTPClient alloc] initWithRequest:
                                [connection getEntitiesInCollection:@"assets"
                                                         usingQuery:query]];
        [client connectWithCompletionHandler:^(UGHTTPResult *result) {
            id entities = [result.object objectForKey:@"entities"];
            if ([entities count]) {
                id entity = [entities objectAtIndex:0];
                NSString *uuid = [entity objectForKey:@"uuid"];
                UGHTTPClient *assetclient = [[UGHTTPClient alloc] initWithRequest:
                                             [connection getDataForAsset:uuid]];
                [assetclient connectWithCompletionHandler:^(UGHTTPResult *result) {
                    UIImage *image = [UIImage imageWithData:result.data];
                    [self.images setObject:image forKey:partyName];
                    [self.tableView reloadRowsAtIndexPaths:[NSArray arrayWithObject:indexPath]
                                          withRowAnimation:UITableViewRowAnimationNone];
                }];
            }
        }];
    }
}

- (void) reload {
    UGConnection *connection = [UGConnection sharedConnection];
    if ([connection isAuthenticated]) {
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
}

- (void) authenticate {
    UGConnection *connection = [UGConnection sharedConnection];
    if (![connection isAuthenticated]) {
        [[[UGHTTPClient alloc] initWithRequest:
          [connection getAccessTokenForApplicationWithUsername:@"radtastical"
                                                      password:@"partyrock2013"]]
         connectWithCompletionHandler:^(UGHTTPResult *result) {
             [connection authenticateWithResult:result];
             if ([connection isAuthenticated]) {
                 if (self.content) {
                     // trigger photo loads
                     [self.tableView reloadData];
                 } else {
                     // load all party information
                     [self reload];
                 }
             }
         }];
    }
}

- (void)loadView
{
    [super loadView];
    self.navigationItem.title = @"Parties";
    self.navigationItem.leftBarButtonItem = [[UIBarButtonItem alloc]
                                             initWithBarButtonSystemItem:UIBarButtonSystemItemRefresh
                                             target:self
                                             action:@selector(reload)];
    self.tableView.rowHeight = 100;
    
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    NSDictionary *parties = [defaults objectForKey:@"parties"];
    if (parties) {
        self.content = parties;
    }
    UGConnection *connection = [UGConnection sharedConnection];
    connection.server = @"https://api.usergrid.com";
    connection.organization = @"macmoe";
    connection.application = @"partyapi";
    [self authenticate];
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
        
        NSString *partyName = [entity objectForKey:@"name"];
        UIImage *image = [self.images objectForKey:partyName];
        if (image) {
            cell.imageView.image = image;
        } else {
            cell.imageView.image = nil;
            [self fetchAssetForParty:[entity objectForKey:@"name"] atIndexPath:indexPath];
        }
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
