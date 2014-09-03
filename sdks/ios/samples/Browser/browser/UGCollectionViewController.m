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
