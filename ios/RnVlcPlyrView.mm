#import "RnVlcPlyrView.h"

#import <react/renderer/components/RnVlcPlyrViewSpec/ComponentDescriptors.h>
#import <react/renderer/components/RnVlcPlyrViewSpec/EventEmitters.h>
#import <react/renderer/components/RnVlcPlyrViewSpec/Props.h>
#import <react/renderer/components/RnVlcPlyrViewSpec/RCTComponentViewHelpers.h>

#import "RCTFabricComponentsPlugins.h"
#import "RnVlcPlyr-Swift.h"

@protocol VLCViewProtocol <NSObject>

- (void)setUrl:(NSString *)url;

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

    if (oldViewProps.url != newViewProps.url) {
        NSString *urlToConvert = [[NSString alloc] initWithUTF8String: newViewProps.url.c_str()];
        
        [self.vlcPlayerView performSelector:@selector(setUrl:) withObject:urlToConvert];
    }

    [super updateProps:props oldProps:oldProps];
}

Class<RCTComponentViewProtocol> RnVlcPlyrViewCls(void)
{
    return RnVlcPlyrView.class;
}

@end
