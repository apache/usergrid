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
