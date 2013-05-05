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
    if ([indexPath row] == 1) {
        NSMutableAttributedString *attributedString = attributed_string([self.content objectForKey:@"title"], 30);
        [attributedString appendAttributedString:carriage_return(18)];
        [attributedString appendAttributedString:attributed_string([self.content objectForKey:@"venue"], 24)];
        [attributedString appendAttributedString:carriage_return(24)];
        
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
        
        [attributedString appendAttributedString:attributed_string([self.content objectForKey:@"partytime"], 18)];
        [attributedString appendAttributedString:carriage_return(18)];
        [attributedString appendAttributedString:attributed_string([self.content objectForKey:@"startDate"], 18)];
        [attributedString appendAttributedString:carriage_return(18)];
        [attributedString appendAttributedString:attributed_string([self.content objectForKey:@"endDate"], 18)];
        [attributedString appendAttributedString:carriage_return(18)];
        return attributedString;
    } else if ([indexPath row] == 2) {
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
    return 1;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
    return 3;
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    switch ([indexPath row]) {
        case 0: {
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
        }
        default: {
            UITableViewCell *cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleDefault
                                                           reuseIdentifier:@"Cell"];
            cell.selectionStyle = UITableViewCellSelectionStyleNone;
            cell.textLabel.attributedText = [self attributedStringForIndexPath:indexPath];
            cell.textLabel.numberOfLines = 0;
            return cell;
        }
    }
}

- (CGFloat) tableView:(UITableView *)tableView heightForRowAtIndexPath:(NSIndexPath *)indexPath
{
    switch ([indexPath row]) {
        case 0:
            return 200;
        default: {
            CGFloat w = tableView.bounds.size.width;
            CGFloat h = 0;
            NSMutableAttributedString *attributedString = [self attributedStringForIndexPath:indexPath];
            CGRect bounds = [attributedString boundingRectWithSize:CGSizeMake(w,h)
                                                           options:NSStringDrawingUsesFontLeading+NSStringDrawingUsesLineFragmentOrigin
                                                           context:nil];
            return bounds.size.height;
        }
    }
}

@end
