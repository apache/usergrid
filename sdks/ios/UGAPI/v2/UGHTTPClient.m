#import "UGHTTPClient.h"
#import "UGHTTPResult.h"

@interface UGHTTPClient ()
@property (nonatomic, strong) NSMutableURLRequest *request;
@property (nonatomic, strong) NSMutableData *data;
@property (nonatomic, strong) NSHTTPURLResponse *response;
@property (nonatomic, strong) NSURLConnection *connection;
@end

@implementation UGHTTPClient

static int activityCount = 0;

+ (void) retainNetworkActivityIndicator {
    activityCount++;
#if TARGET_OS_IPHONE
    [[UIApplication sharedApplication] setNetworkActivityIndicatorVisible:YES];
#endif
}

+ (void) releaseNetworkActivityIndicator {
    activityCount--;
#if TARGET_OS_IPHONE
    if (activityCount == 0) {
        [[UIApplication sharedApplication] setNetworkActivityIndicatorVisible:NO];
    }
#endif
}

- (id) initWithRequest:(NSMutableURLRequest *)request
{
    if (self = [super init]) {
        self.request = request;
    }
    return self;
}

- (UGHTTPResult *) connect {
    NSHTTPURLResponse *response;
    NSError *error;
    UGHTTPResult *result = [[UGHTTPResult alloc] init];
    result.data = [NSURLConnection sendSynchronousRequest:self.request returningResponse:&response error:&error];
    result.response = response;
    result.error = error;
    if (self.completionHandler) {
        self.completionHandler(result);
    }
    return result;
}

- (void) connectWithCompletionHandler:(UGHTTPCompletionHandler)completionHandler
                      progressHandler:(UGHTTPProgressHandler)progressHandler
{
   	[self.request setCachePolicy:NSURLRequestReloadIgnoringLocalCacheData];
    self.data = nil;
    self.response = nil;
	self.completionHandler = completionHandler;
    self.progressHandler = progressHandler;
	self.connection = [[NSURLConnection alloc] initWithRequest:self.request delegate:self];
    [isa retainNetworkActivityIndicator];
}

- (void) connectWithCompletionHandler:(UGHTTPCompletionHandler) completionHandler
{
    [self connectWithCompletionHandler:completionHandler progressHandler:nil];
}

- (void)connection:(NSURLConnection *)connection didReceiveResponse:(NSHTTPURLResponse *)response
{
	self.response = response;
    if (self.progressHandler) {
        self.progressHandler(0.0);
    }
}

- (void)connection:(NSURLConnection *)connection didReceiveData:(NSData *)newData
{
    if (!self.data) {
        self.data = [NSMutableData dataWithData:newData];
    } else {
        [self.data appendData:newData];
    }
    if (self.progressHandler) {
        long long expectedLength = [self.response expectedContentLength];
        if (expectedLength > 0) {
            CGFloat progress = ((CGFloat) [self.data length]) / expectedLength;
            self.progressHandler(progress);
        }
    }
}

- (void)connection:(NSURLConnection *)connection didFailWithError:(NSError *)error
{
    if (self.completionHandler) {
        UGHTTPResult *result = [[UGHTTPResult alloc] init];
        result.response = self.response;
        result.data = self.data;
        result.error = error;
        self.completionHandler(result);
    }
	self.connection = nil;
    self.data = nil;
    [isa releaseNetworkActivityIndicator];
}

- (void)connectionDidFinishLoading:(NSURLConnection *)connection
{
    if (self.progressHandler) {
        self.progressHandler(1.0);
    }
    if (self.completionHandler) {
        UGHTTPResult *result = [[UGHTTPResult alloc] init];
        result.response = self.response;
        result.data = self.data;
        result.error = nil;
        //[self.data writeToFile:@"/tmp/data" atomically:NO];
        self.completionHandler(result);
    }
    self.connection = nil;
    self.data = nil;
    [isa releaseNetworkActivityIndicator];
}

- (void) cancel {
    if (self.connection) {
        [self.connection cancel];
    }
    self.connection = nil;
}

- (BOOL) isRunning {
    return (self.connection != nil);
}

- (CGFloat) progress {
    long long expectedLength = [self.response expectedContentLength];
    if (expectedLength > 0) {
        return ((CGFloat) [self.data length]) / expectedLength;
    } else {
        return 0.0;
    }
}

@end
