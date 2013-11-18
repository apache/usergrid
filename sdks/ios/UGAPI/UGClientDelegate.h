#import <Foundation/Foundation.h>
#import "UGClientResponse.h"

/******************************A NOTE ON THIS DELEGATE********************************
Objects conform to this protocol to take advantage of the Asynchronus SDK functionality.
The setDelegate method needs to be called on the current UGClient for this function to
be called on an implemented delegate.
 
If you do not set a delegate, all functions will run synchronously, blocking
until a response has been received or an error detected.
*************************************************************************************/
@protocol UGClientDelegate <NSObject>

//This method is called after every request to the UserGrid API.
//It passes in the response to the API request, and returns nothing.
-(void)ugClientResponse:(UGClientResponse *)response;

@end
