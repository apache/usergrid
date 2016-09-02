//
// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements.  See the NOTICE file distributed with
// this work for additional information regarding copyright ownership.
// The ASF licenses this file to You under the Apache License, Version 2.0
// (the "License"); you may not use this file except in compliance with
// the License.  You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

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
@property (nonatomic, strong) NSArray *partiesByStartDate;
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

- (void) setContent:(NSDictionary *)content
{
    _content = content;
    
    NSMutableDictionary *partiesByStartDateDictionary = [NSMutableDictionary dictionary];
    for (NSDictionary *party in self.content[@"entities"]) {
        NSString *startDate = [party objectForKey:@"startDate"];
        NSArray *parts = [startDate componentsSeparatedByString:@" "];
        NSString *datePart = [parts objectAtIndex:0];
        NSMutableArray *arrayOfParties = [partiesByStartDateDictionary objectForKey:datePart];
        if (!arrayOfParties) {
            arrayOfParties = [NSMutableArray array];
            [partiesByStartDateDictionary setObject:arrayOfParties forKey:datePart];
        }
        [arrayOfParties addObject:party];
    }
    NSArray *keys = [[partiesByStartDateDictionary allKeys] sortedArrayUsingComparator:^NSComparisonResult(id obj1, id obj2) {
        return [obj1 compare:obj2];
    }];
    NSMutableArray *partiesByStartDate = [NSMutableArray array];
    for (NSString *key in keys) {
        [partiesByStartDate addObject:
         @{@"date":key,
         @"parties":[partiesByStartDateDictionary objectForKey:key]}];
    }
    self.partiesByStartDate = partiesByStartDate;
    // NSLog(@"%@", self.partiesByStartDate);
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
    return [self.partiesByStartDate count];
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
    if (self.content) {
        NSDictionary *sectionInfo = [self.partiesByStartDate objectAtIndex:section];
        NSArray *parties = [sectionInfo objectForKey:@"parties"];
        return [parties count];
    } else {
        return 0;
    }
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    return [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleSubtitle reuseIdentifier:@"Cell"];
}

#pragma mark - Table view delegate

- (void)tableView:(UITableView *)tableView willDisplayCell:(UITableViewCell *)cell forRowAtIndexPath:(NSIndexPath *)indexPath
{
    if (self.content) {
        
        NSDictionary *sectionInfo = [self.partiesByStartDate objectAtIndex:[indexPath section]];
        NSArray *parties = [sectionInfo objectForKey:@"parties"];
        
        id entity = parties[[indexPath row]];
        cell.textLabel.text = entity[@"title"];
        cell.textLabel.numberOfLines = 0;
        cell.detailTextLabel.text = [NSString stringWithFormat:@"%@ %@", entity[@"venue"], entity[@"partytime"]];
        cell.detailTextLabel.numberOfLines = 0;
        
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
        NSDictionary *sectionInfo = [self.partiesByStartDate objectAtIndex:[indexPath section]];
        NSArray *parties = [sectionInfo objectForKey:@"parties"];
        id entity = [parties objectAtIndex:[indexPath row]];
        PartyViewController *partyViewController = [[PartyViewController alloc] init];
        partyViewController.content = entity;
        [self.navigationController pushViewController:partyViewController animated:YES];
    }
}

- (NSString *) tableView:(UITableView *)tableView titleForHeaderInSection:(NSInteger)section
{
    NSDictionary *sectionInfo = [self.partiesByStartDate objectAtIndex:section];
    return [sectionInfo objectForKey:@"date"];
}

@end
