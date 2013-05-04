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

- (id)initWithStyle:(UITableViewStyle)style
{
    self = [super initWithStyle:style];
    if (self) {
        // Custom initialization
    }
    return self;
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (void) viewWillAppear:(BOOL)animated
{
    self.navigationItem.title = [self.content objectForKey:@"title"];
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
    return 2;
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
                annotationPoint.title = [self.content objectForKey:@"name"];
                annotationPoint.subtitle = [self.content objectForKey:@"venue"];
                [mapView addAnnotation:annotationPoint];
                mapView.scrollEnabled = NO;
                mapView.zoomEnabled = NO;
            }
            return self.mapCell;
        }
        default: {
            UITableViewCell *cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleDefault reuseIdentifier:@"Cell"];
            cell.selectionStyle = UITableViewCellSelectionStyleNone;
            return cell;
        }
    }
}

- (CGFloat) tableView:(UITableView *)tableView heightForRowAtIndexPath:(NSIndexPath *)indexPath
{
    switch ([indexPath row]) {
        case 0:
            return 200;
        default:
            return 600;
    }
}

#pragma mark - Table view delegate

- (void)tableView:(UITableView *)tableView willDisplayCell:(UITableViewCell *)cell forRowAtIndexPath:(NSIndexPath *)indexPath
{
    switch ([indexPath row]) {
        case 0: {

        }
        default: {
            cell.textLabel.text = [self.content description];
            cell.textLabel.font = [UIFont systemFontOfSize:12];
            cell.textLabel.numberOfLines = 0;            
        }
    }
}

@end
