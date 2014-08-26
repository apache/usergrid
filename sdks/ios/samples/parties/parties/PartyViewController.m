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
//  PartyViewController.m
//  parties
//
//  Copyright (c) 2013 Apigee. All rights reserved.
//

#import "PartyViewController.h"

@interface PartyViewController ()

@property (nonatomic, strong) UITableViewCell *mapCell;

@end

@implementation PartyViewController

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
}

- (void) viewWillAppear:(BOOL)animated
{
    self.navigationItem.title = [self.content objectForKey:@"title"];
}

UIFont *font_with_size(CGFloat size) {
    return [UIFont fontWithName:@"Helvetica-Bold" size:size];
}

NSAttributedString *carriage_return(CGFloat size) {
    return [[NSAttributedString alloc] initWithString:[NSString stringWithFormat:@"\n"]
                                           attributes:@{NSFontAttributeName:font_with_size(size)}];
}

NSMutableAttributedString *attributed_string(NSString *string, CGFloat size) {
    return [[NSMutableAttributedString alloc] initWithString:string
                                                  attributes:@{NSFontAttributeName:font_with_size(size)}];
}

- (NSMutableAttributedString *) attributedStringForIndexPath:(NSIndexPath *) indexPath
{
    if (([indexPath section] == 0) && ([indexPath row] == 1)) {
        NSMutableAttributedString *attributedString = attributed_string([self.content objectForKey:@"title"], 30);
        [attributedString appendAttributedString:carriage_return(18)];
        [attributedString appendAttributedString:attributed_string([self.content objectForKey:@"venue"], 24)];
        [attributedString appendAttributedString:carriage_return(24)];
        
        [attributedString appendAttributedString:attributed_string([self.content objectForKey:@"startDate"], 18)];
        [attributedString appendAttributedString:carriage_return(18)];
        [attributedString appendAttributedString:carriage_return(18)];
        
        id address = [self.content objectForKey:@"address"];
        [attributedString appendAttributedString:attributed_string([address objectForKey:@"street"], 18)];
        [attributedString appendAttributedString:carriage_return(18)];
        [attributedString appendAttributedString:attributed_string([address objectForKey:@"city"], 18)];
        [attributedString appendAttributedString:attributed_string(@", ", 18)];
        [attributedString appendAttributedString:attributed_string([address objectForKey:@"state"], 18)];
        [attributedString appendAttributedString:carriage_return(18)];
        [attributedString appendAttributedString:carriage_return(18)];
        
        [attributedString appendAttributedString:attributed_string([self.content objectForKey:@"description"], 18)];
        [attributedString appendAttributedString:carriage_return(18)];
        [attributedString appendAttributedString:carriage_return(18)];
        
        return attributedString;
    } else if (([indexPath section] == 1) && ([indexPath row] == 0)) {
        NSMutableAttributedString *attributedString = attributed_string([self.content description], 12);
        [attributedString appendAttributedString:carriage_return(12)];
        return attributedString;
    } else {
        return nil;
    }
}

#pragma mark - Table view data source

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView
{
    return 2;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
    return (section == 0) ? 2 : 1;
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    if (([indexPath section] == 0) && ([indexPath row] == 0)) {
        if (!self.mapCell) {
            self.mapCell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleDefault reuseIdentifier:@"Map"];
            MKMapView *mapView = [[MKMapView alloc] initWithFrame:self.mapCell.bounds];
            mapView.autoresizingMask = UIViewAutoresizingFlexibleWidth + UIViewAutoresizingFlexibleHeight;
            [self.mapCell addSubview:mapView];
            id location = [self.content objectForKey:@"location"];
            double lat = [[location objectForKey:@"latitude"] floatValue];
            double lng = [[location objectForKey:@"longitude"] floatValue];
            [mapView setRegion:MKCoordinateRegionMake(CLLocationCoordinate2DMake(lat,lng),
                                                      MKCoordinateSpanMake(0.002,0.002))];
            CLLocationCoordinate2D annotationCoord;
            annotationCoord.latitude = lat;
            annotationCoord.longitude = lng;
            MKPointAnnotation *annotationPoint = [[MKPointAnnotation alloc] init];
            annotationPoint.coordinate = annotationCoord;
            annotationPoint.title = [self.content objectForKey:@"title"];
            annotationPoint.subtitle = [self.content objectForKey:@"venue"];
            [mapView addAnnotation:annotationPoint];
            mapView.scrollEnabled = NO;
            mapView.zoomEnabled = NO;
        }
        return self.mapCell;
    } else {
        UITableViewCell *cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleDefault
                                                       reuseIdentifier:@"Cell"];
        cell.selectionStyle = UITableViewCellSelectionStyleNone;
        cell.textLabel.attributedText = [self attributedStringForIndexPath:indexPath];
        cell.textLabel.numberOfLines = 0;
        return cell;
    }
}

- (CGFloat) tableView:(UITableView *)tableView heightForRowAtIndexPath:(NSIndexPath *)indexPath
{
    if (([indexPath section] == 0) && ([indexPath row] == 0)) {
        return 200;
    } else {
        CGFloat w = tableView.bounds.size.width - 20; // use approximate margins
        CGFloat h = 0;
        NSMutableAttributedString *attributedString = [self attributedStringForIndexPath:indexPath];
        CGRect bounds = [attributedString boundingRectWithSize:CGSizeMake(w,h)
                                                       options:NSStringDrawingUsesFontLeading+NSStringDrawingUsesLineFragmentOrigin
                                                       context:nil];
        return bounds.size.height;
    }
}

- (NSString *) tableView:(UITableView *)tableView titleForHeaderInSection:(NSInteger)section
{
    if (section == 0) {
        return nil;
    } else if (section == 1) {
        return @"Party Entity Details";
    }
    return nil;
}

@end
