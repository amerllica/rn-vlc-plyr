#import <Foundation/Foundation.h>

@class VLCMediaPlayer;

NS_ASSUME_NONNULL_BEGIN

@interface VlcBufferingObserver : NSObject

+ (nullable instancetype)observePlayer:(VLCMediaPlayer *)player onBuffering:(void (^)(float percent))onBuffering NS_SWIFT_NAME(observe(_:onBuffering:));
- (instancetype)init NS_UNAVAILABLE;
- (void)stop;

@end

NS_ASSUME_NONNULL_END
