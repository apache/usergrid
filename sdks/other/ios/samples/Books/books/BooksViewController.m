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
//  BooksViewController.m
//  books
//

#import "BooksViewController.h"
#import "UGSignInViewController.h"
#import "UGConnection.h"
#import "UGHTTPClient.h"
#import "UGHTTPResult.h"
#import "AddBookViewController.h"

@interface BooksViewController ()
@property (nonatomic, strong) NSDictionary *content;
@end

@implementation BooksViewController

- (id)initWithStyle:(UITableViewStyle)style
{
    self = [super initWithStyle:style];
    if (self) {
        // Custom initialization
    }
    return self;
}

- (void)loadView
{
    [super loadView];
    self.navigationItem.title = @"My Books";
    self.navigationItem.rightBarButtonItem = [[UIBarButtonItem alloc] initWithTitle:@"Add Book"
                                                                              style:UIBarButtonItemStyleBordered target:self action:@selector(addbook:)];
    self.navigationItem.leftBarButtonItem = [[UIBarButtonItem alloc]
                                             initWithTitle:@"Connection"
                                             style:UIBarButtonItemStyleBordered
                                             target:self
                                             action:@selector(connect:)];                                              
}

- (void) connect:(id) sender
{
    UGSignInViewController *signinViewController = [[UGSignInViewController alloc] init];
    UINavigationController *signinNavigationController =
    [[UINavigationController alloc] initWithRootViewController:signinViewController];
    signinNavigationController.modalPresentationStyle = UIModalPresentationFormSheet;
    signinNavigationController.navigationBar.tintColor = self.navigationController.navigationBar.tintColor;
    [self presentViewController:signinNavigationController animated:YES completion:nil];
}

- (void) addbook:(id) sender
{
    AddBookViewController *addBookViewController = [[AddBookViewController alloc] init];
    UINavigationController *navigationController =
    [[UINavigationController alloc] initWithRootViewController:addBookViewController];
    navigationController.modalPresentationStyle = UIModalPresentationFormSheet;
    navigationController.navigationBar.tintColor = self.navigationController.navigationBar.tintColor;
    [self presentViewController:navigationController animated:YES completion:nil];
}

- (void) viewWillAppear:(BOOL)animated
{
    [self reload];
}

- (void) reload {
    UGConnection *usergrid = [UGConnection sharedConnection];
    if ([usergrid isAuthenticated]) {
        NSLog(@"loading...");
        UGHTTPClient *client = [[UGHTTPClient alloc] initWithRequest:
                                [usergrid getEntitiesInCollection:@"books" limit:100]];
        [client connectWithCompletionHandler:^(UGHTTPResult *result) {
            NSLog(@"%@", result.object);
            self.content = result.object;
            [self.tableView reloadData];
        }];
    }
}

#pragma mark - Table view data source

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView
{
    // Return the number of sections.
    return 1;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
    // Return the number of rows in the section.
    return self.content ? [self.content[@"entities"] count] : 1;
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    return [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleSubtitle reuseIdentifier:@"Cell"];
}

#pragma mark - Table view delegate

- (void)tableView:(UITableView *)tableView willDisplayCell:(UITableViewCell *)cell forRowAtIndexPath:(NSIndexPath *)indexPath
{
    if (!self.content) {
        cell.textLabel.text = @"Please sign in.";
    } else {
        id entity = self.content[@"entities"][[indexPath row]];
        cell.textLabel.text = entity[@"title"];
        cell.detailTextLabel.text = entity[@"author"];
        UIButton *deleteButton = [UIButton buttonWithType:UIButtonTypeRoundedRect];
        cell.accessoryView = deleteButton;
        [deleteButton setTitle:@"X" forState:UIControlStateNormal];
        deleteButton.tag = [indexPath row];
        [deleteButton addTarget:self action:@selector(deleteItem:) forControlEvents:UIControlEventTouchUpInside];
        [deleteButton sizeToFit];
    }
    cell.selectionStyle = UITableViewCellSelectionStyleNone;
}

- (void) deleteItem:(UIButton *) sender {
    int row = [sender tag];
    id entity = self.content[@"entities"][row];
    NSString *uuid = [entity objectForKey:@"uuid"];
    UGHTTPClient *client = [[UGHTTPClient alloc] initWithRequest:
                            [[UGConnection sharedConnection] deleteEntity:uuid inCollection:@"books"]];
    [client connectWithCompletionHandler:^(UGHTTPResult *result) {
        [self reload];
    }];
}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath
{
    if (!self.content) {
        [self connect:nil];
    }
}

@end
