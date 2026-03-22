using mauiApp.Services.Interfaces;
using UIKit;

namespace mauiApp.Services;

[assembly: Dependency(typeof(DeviceInfoService_Mac))]
public class DeviceInfoService_Mac : IDeviceService
{
    public string GetPlatformName() => "MacCatalyst";
    public string GetDeviceModel() => UIDevice.CurrentDevice.Model;
}