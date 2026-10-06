require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "RnVlcPlyr"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.homepage     = package["homepage"]
  s.license      = package["license"]
  s.authors      = package["author"]

  s.platforms    = { :ios => "16.0" }
  s.source       = { :git => "https://github.com/amerllica/rn-vlc-plyr.git", :tag => "#{s.version}" }

  s.source_files = [
    "ios/Sources/**/*.{swift}",
    "ios/Sources/**/*.{m,mm}",
    "cpp/**/*.{hpp,cpp}",
    "ios/Sources/**/*.h",
  ]
  s.public_header_files = ["ios/Sources/ObjC/*.h"]

  s.dependency 'React-jsi'
  s.dependency 'React-callinvoker'
  s.dependency "MobileVLCKit", "~> 3.7.4"

  s.test_spec "Tests" do |t|
    t.source_files = "ios/Tests/**/*.swift"
    t.resources = "ios/Tests/Resources/*"
    t.frameworks = "XCTest"
    t.pod_target_xcconfig = {
      "CLANG_CXX_LANGUAGE_STANDARD" => "c++20",
      "SWIFT_OBJC_INTEROP_MODE" => "objcxx",
    }
  end

  load 'nitrogen/generated/ios/RnVlcPlyr+autolinking.rb'
  add_nitrogen_files(s)

  install_modules_dependencies(s)
end
