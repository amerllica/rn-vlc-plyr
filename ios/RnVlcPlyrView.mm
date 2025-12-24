#import "RnVlcPlyrView.h"

#import <react/renderer/components/RnVlcPlyrViewSpec/ComponentDescriptors.h>
#import <react/renderer/components/RnVlcPlyrViewSpec/EventEmitters.h>
#import <react/renderer/components/RnVlcPlyrViewSpec/Props.h>
#import <react/renderer/components/RnVlcPlyrViewSpec/RCTComponentViewHelpers.h>

#import "RCTFabricComponentsPlugins.h"
#import "RnVlcPlyr-Swift.h"

@protocol VLCViewProtocol <NSObject>

// props setters
- (void)setUrl:(NSString *)url;
- (void)setAutoPlay:(BOOL)autoPlay;
- (void)setLoop:(BOOL)loop;
- (void)setMuted:(BOOL)muted;

// commands
- (void)play;
- (void)pause;
- (void)stop;
- (void)seek:(double)time;

@end

using namespace facebook::react;

@interface RnVlcPlyrView () <RCTRnVlcPlyrViewViewProtocol>

@property (nonatomic, strong) __kindof UIView<VLCViewProtocol> *vlcPlayerView;

@end

@implementation RnVlcPlyrView {
  // remove: UIView *_view;
}

+ (ComponentDescriptorProvider)componentDescriptorProvider
{
  return concreteComponentDescriptorProvider<RnVlcPlyrViewComponentDescriptor>();
}

- (instancetype)initWithFrame:(CGRect)frame
{
  if (self = [super initWithFrame:frame]) {
    static const auto defaultProps = std::make_shared<const RnVlcPlyrViewProps>();
    _props = defaultProps;
    
    Class vlcViewClass = NSClassFromString(@"RnVlcPlyr.VlcPlyrView");
    
    if (vlcViewClass) {
      id <VLCViewProtocol> playerViewInstance = [[vlcViewClass alloc] initWithFrame:self.bounds];
      
      self.vlcPlayerView = (__kindof UIView<VLCViewProtocol> *)playerViewInstance;
      
      self.vlcPlayerView.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
      self.contentView = self.vlcPlayerView;
      
    } else {
      UIView *fallbackView = [[UIView alloc] init];
      fallbackView.backgroundColor = [UIColor redColor];
      fallbackView.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
      
      self.contentView = fallbackView;
      
      NSLog(@"[RnVlcPlyr] ERROR: Could not find Swift class RnVlcPlyr.VlcPlyrView. Using Red Fallback.");
    }
  }
  
  return self;
}

- (void)updateProps:(Props::Shared const &)props oldProps:(Props::Shared const &)oldProps
{
  const auto &oldViewProps = *std::static_pointer_cast<RnVlcPlyrViewProps const>(_props);
  const auto &newViewProps = *std::static_pointer_cast<RnVlcPlyrViewProps const>(props);
  
  if (oldProps == nullptr || oldViewProps.autoPlay != newViewProps.autoPlay) {
    [self.vlcPlayerView setAutoPlay:newViewProps.autoPlay];
  }
  
  if (oldViewProps.muted != newViewProps.muted) {
    [self.vlcPlayerView setMuted:newViewProps.muted];
  }
  
  if (oldViewProps.loop != newViewProps.loop) {
    [self.vlcPlayerView setLoop:newViewProps.loop];
  }
  
  if (oldViewProps.url != newViewProps.url) {
    NSString *urlToConvert = [[NSString alloc] initWithUTF8String: newViewProps.url.c_str()];
    
    [self.vlcPlayerView setUrl:urlToConvert];
  }
  
  [super updateProps:props oldProps:oldProps];
}

- (void)handleCommand:(NSString *)commandName args:(NSArray<id<NSObject>> *)args
{
  if (!self.vlcPlayerView) {
    return;
  }
  
  if ([commandName isEqualToString:@"play"]) {
    [self.vlcPlayerView play];
  } else if ([commandName isEqualToString:@"pause"]) {
    [self.vlcPlayerView pause];
  } else if ([commandName isEqualToString:@"stop"]) {
    [self.vlcPlayerView stop];
  } else if ([commandName isEqualToString:@"seek"]) {
    if (args.count > 0 && [args[0] isKindOfClass:[NSNumber class]]) {
      double time = [(NSNumber *)args[0] doubleValue];
      [self.vlcPlayerView seek:time];
    }
  }
}

Class<RCTComponentViewProtocol> RnVlcPlyrViewCls(void)
{
  return RnVlcPlyrView.class;
}

@end
