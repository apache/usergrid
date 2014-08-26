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
//  UGTextViewController.m
//  Browser
//


#import "UGTextViewController.h"

@interface UGTextViewController ()
@property (nonatomic, strong) UITextView *textView;
@end

@implementation UGTextViewController

- (void) loadView
{
    [super loadView];
    self.textView = [[UITextView alloc] initWithFrame:self.view.bounds];
    self.textView.autoresizingMask = UIViewAutoresizingFlexibleWidth+UIViewAutoresizingFlexibleHeight;
    self.textView.font = [UIFont systemFontOfSize:20];
    [self.view addSubview:self.textView];
    self.textView.text = [self.binding[self.key] description];
    self.navigationItem.title = self.key;
}

@end
