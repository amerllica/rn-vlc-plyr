#import "VlcBufferingObserver.h"
#import <MobileVLCKit/MobileVLCKit.h>
#import <MobileVLCKit/vlc/libvlc.h>
#import <MobileVLCKit/vlc/libvlc_picture.h>
#import <MobileVLCKit/vlc/libvlc_renderer_discoverer.h>
#import <MobileVLCKit/vlc/libvlc_media.h>
#import <MobileVLCKit/vlc/libvlc_media_player.h>
#import <MobileVLCKit/vlc/libvlc_events.h>
#import <objc/message.h>

static NSString *const VlcNativePlayerSelectorName = @"libVLCMediaPlayer";

static void VlcBufferingObserverHandleEvent(const libvlc_event_t *event, void *context);

@interface VlcBufferingObserver ()
@property (nonatomic, readonly) libvlc_event_manager_t *eventManager;
@property (nonatomic, readonly, copy) void (^onBuffering)(float percent);
@property (atomic) BOOL isAttached;
@end

@implementation VlcBufferingObserver

+ (instancetype)observePlayer:(VLCMediaPlayer *)player onBuffering:(void (^)(float percent))onBuffering {
  libvlc_media_player_t *nativePlayer = [self nativePlayerOf:player];
  if (nativePlayer == NULL) {
    return nil;
  }
  libvlc_event_manager_t *eventManager = libvlc_media_player_event_manager(nativePlayer);
  if (eventManager == NULL) {
    return nil;
  }
  VlcBufferingObserver *observer = [[self alloc] initWithEventManager:eventManager onBuffering:onBuffering];
  return [observer attach] ? observer : nil;
}

+ (libvlc_media_player_t *)nativePlayerOf:(VLCMediaPlayer *)player {
  SEL selector = NSSelectorFromString(VlcNativePlayerSelectorName);
  if (![player respondsToSelector:selector]) {
    return NULL;
  }
  void *(*readNativePlayer)(id, SEL) = (void *(*)(id, SEL))objc_msgSend;
  return (libvlc_media_player_t *)readNativePlayer(player, selector);
}

- (instancetype)initWithEventManager:(libvlc_event_manager_t *)eventManager onBuffering:(void (^)(float percent))onBuffering {
  self = [super init];
  if (self) {
    _eventManager = eventManager;
    _onBuffering = [onBuffering copy];
  }
  return self;
}

- (BOOL)attach {
  int failed = libvlc_event_attach(self.eventManager, libvlc_MediaPlayerBuffering, VlcBufferingObserverHandleEvent, (__bridge void *)self);
  self.isAttached = failed == 0;
  return self.isAttached;
}

- (void)stop {
  if (!self.isAttached) {
    return;
  }
  self.isAttached = NO;
  libvlc_event_detach(self.eventManager, libvlc_MediaPlayerBuffering, VlcBufferingObserverHandleEvent, (__bridge void *)self);
}

- (void)deliver:(float)percent {
  void (^onBuffering)(float) = self.onBuffering;
  dispatch_async(dispatch_get_main_queue(), ^{
    onBuffering(percent);
  });
}

@end

static void VlcBufferingObserverHandleEvent(const libvlc_event_t *event, void *context) {
  VlcBufferingObserver *observer = (__bridge VlcBufferingObserver *)context;
  if (!observer.isAttached) {
    return;
  }
  [observer deliver:event->u.media_player_buffering.new_cache];
}
