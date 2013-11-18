#import <Foundation/Foundation.h>

@class UGHTTPResult;

typedef void (^UGHTTPCompletionHandler)(UGHTTPResult *result);
typedef void (^UGHTTPProgressHandler)(CGFloat progress);

@interface UGHTTPClient : NSObject
#if TARGET_OS_IPHONE
<NSURLConnectionDataDelegate>
#endif

@property (nonatomic, copy) UGHTTPCompletionHandler completionHandler;
@property (nonatomic, copy) UGHTTPProgressHandler progressHandler;
@property (readonly) CGFloat progress;
@property (readonly) BOOL isRunning;

- (id) initWithRequest:(NSMutableURLRequest *) request;

- (UGHTTPResult *) connect;

- (void) connectWithCompletionHandler:(UGHTTPCompletionHandler) completionHandler;

- (void) connectWithCompletionHandler:(UGHTTPCompletionHandler) completionHandler
                      progressHandler:(UGHTTPProgressHandler) progressHandler;

- (void) cancel;

@end
