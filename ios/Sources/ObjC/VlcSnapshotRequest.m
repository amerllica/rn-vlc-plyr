#import "VlcSnapshotRequest.h"
#import <MobileVLCKit/MobileVLCKit.h>

static const int VlcOriginalSnapshotDimension = 0;

@implementation VlcSnapshotRequest

+ (BOOL)saveSnapshotOfPlayer:(VLCMediaPlayer *)player toPath:(NSString *)path {
  @try {
    [player saveVideoSnapshotAt:path withWidth:VlcOriginalSnapshotDimension andHeight:VlcOriginalSnapshotDimension];
    return YES;
  } @catch (NSException *exception) {
    return NO;
  }
}

@end
