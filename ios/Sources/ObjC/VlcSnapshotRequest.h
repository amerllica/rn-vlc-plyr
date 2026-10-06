#import <Foundation/Foundation.h>

@class VLCMediaPlayer;

NS_ASSUME_NONNULL_BEGIN

@interface VlcSnapshotRequest : NSObject

+ (BOOL)saveSnapshotOfPlayer:(VLCMediaPlayer *)player toPath:(NSString *)path;

@end

NS_ASSUME_NONNULL_END
