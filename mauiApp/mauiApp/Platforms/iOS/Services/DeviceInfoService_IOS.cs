using mauiApp.Services.Interfaces;
using UIKit;

namespace mauiApp.Services;

[assembly: Dependency(typeof(DeviceInfoService_IOS))]
public class DeviceInfoService_IOS : IDeviceService
{
    public string GetPlatformName() => "iOS";
    public string GetDeviceModel() => UIDevice.CurrentDevice.Model;
}
