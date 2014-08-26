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
//  AddBookViewController.m
//  books
//

#import "AddBookViewController.h"
#import "UGConnection.h"
#import "UGHTTPClient.h"
#import "UGHTTPResult.h"

@interface FormTableViewCell : UITableViewCell <UITextFieldDelegate>
@property (nonatomic, strong) UILabel *label;
@property (nonatomic, strong) UITextField *textField;
@property (nonatomic, strong) NSString *key;
@property (nonatomic, weak) id binding;
@end

@implementation FormTableViewCell

- (id) initWithTitle:(NSString *)title key:(NSString *) key binding:(id) binding
{
    if (self = [super initWithStyle:UITableViewCellStyleDefault reuseIdentifier:@"Cell"]) {
        self.label = [[UILabel alloc] initWithFrame:CGRectMake(10,0,90,14)];
        self.label.backgroundColor = [UIColor clearColor];
        self.label.font = [UIFont systemFontOfSize:10];
        self.label.textColor = [UIColor grayColor];
        self.label.textAlignment = NSTextAlignmentLeft;
        [self.contentView addSubview:self.label];
        self.textField = [[UITextField alloc] initWithFrame:CGRectMake(10, 14, 100, 30)];
        self.textField.contentVerticalAlignment = UIControlContentVerticalAlignmentCenter;
        self.textField.font = [UIFont systemFontOfSize:18];
        self.textField.autocorrectionType = UITextAutocorrectionTypeNo;
        self.textField.autocapitalizationType = UITextAutocapitalizationTypeNone;
        self.textField.delegate = self;
        if ([key isEqualToString:@"password"]) {
            self.textField.secureTextEntry = YES;
        }
        [self.contentView addSubview:self.textField];
        self.selectionStyle = UITableViewCellSelectionStyleNone;
        // individual cell properties
        self.label.text = title;
        self.key = key;
        self.binding = binding;
    }
    return self;
}

- (void) layoutSubviews
{
    [super layoutSubviews];
    CGRect textFieldFrame = self.textField.frame;
    textFieldFrame.size.width = self.textField.superview.bounds.size.width - textFieldFrame.origin.x - 5;
    self.textField.frame = textFieldFrame;
}

- (void) textFieldDidEndEditing:(UITextField *)textField
{
    [self.binding setObject:textField.text forKey:self.key];
}

@end

@interface AddBookViewController ()
@property (nonatomic, strong) NSMutableDictionary *values;
@property (nonatomic, strong) NSArray *cells;
@end

@implementation AddBookViewController

- (id)init {
    if (self = [super initWithStyle:UITableViewStyleGrouped]) {
        self.values = [[[NSUserDefaults standardUserDefaults] objectForKey:@"usergrid"] mutableCopy];
        if (!self.values) {
            self.values = [NSMutableDictionary dictionary];
        }
        self.cells =
        @[[[FormTableViewCell alloc] initWithTitle:@"Title" key:@"title" binding:self.values],
          [[FormTableViewCell alloc] initWithTitle:@"Author" key:@"author" binding:self.values]];
    }
    return self;
}

- (void) loadView
{
    [super loadView];
    self.title = @"Connection";
    self.tableView.backgroundView = nil;
    self.tableView.backgroundColor = [UIColor darkGrayColor];
    self.navigationItem.leftBarButtonItem = [[UIBarButtonItem alloc]
                                             initWithTitle:@"Cancel"
                                             style:UIBarButtonItemStyleBordered
                                             target:self
                                             action:@selector(cancel:)];
    self.navigationItem.rightBarButtonItem = [[UIBarButtonItem alloc]
                                              initWithTitle:@"Save"
                                              style:UIBarButtonItemStyleBordered
                                              target:self
                                              action:@selector(save:)];
}

#pragma mark - Table view data source

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView
{
    return 1;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
    return [self.cells count];
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    return [self.cells objectAtIndex:[indexPath row]];
}

#pragma mark - Table view delegate

- (void)tableView:(UITableView *)tableView willDisplayCell:(UITableViewCell *)cell forRowAtIndexPath:(NSIndexPath *)indexPath
{
    FormTableViewCell *formCell = (FormTableViewCell *) cell;
    formCell.textField.text = [formCell.binding objectForKey:formCell.key];
}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath
{
    FormTableViewCell *formCell = (FormTableViewCell *)[tableView cellForRowAtIndexPath:indexPath];
    [formCell.textField becomeFirstResponder];
}

#pragma mark - Sign In

- (void) cancel:(id) sender
{
    [self dismissViewControllerAnimated:YES completion:nil];
}

- (void) save:(id) sender
{
    for (FormTableViewCell *cell in self.cells) {
        [cell.textField resignFirstResponder];
    }    
    UGConnection *connection = [UGConnection sharedConnection];
    [[[UGHTTPClient alloc] initWithRequest:
      [connection createEntityInCollection:@"books" withValues:self.values]]
     connectWithCompletionHandler:^(UGHTTPResult *result) {
         [self dismissViewControllerAnimated:YES completion:nil];
     }];
}

@end
