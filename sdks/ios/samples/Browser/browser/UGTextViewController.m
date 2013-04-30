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
