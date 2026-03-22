using Android.Hardware.Display;
using Android.OS;
using mauiApp.Services;
using mauiApp.Services.Interfaces;

[assembly: Dependency(typeof(DeviceInfoAppServiceAndroid))]
namespace mauiApp.Services;

public class DeviceInfoAppServiceAndroid : IDeviceAppService
{
    private DevicePlatform _devicePlatform = DevicePlatform.Android;
    private IDeviceInfo _deviceInfo;
    private IDeviceAppService _deviceAppService;
    
    // public string GetNotificationsForDevice() => _deviceAppService.GetNotificationsForDevice();
    public string GetPlatformName() => _devicePlatform.ToString();
    public string GetDeviceModel() => Build.Model;
}