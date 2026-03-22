using System.Text.Encodings.Web;
using System.Threading.Tasks;
using TradingEngine.SDK.Models;

namespace TradingEngine.SDK.Services;

public class TradeRecommendationService : ITradeRecommendationService
{
    private readonly RequestManager _requestManager = RequestManager.Instance;
    private readonly UrlEncoder _urlEncoder = UrlEncoder.Default;
    
    private static string _deviceServiceUrl { get; set; }

    public TradeRecommendationService()
    {
        _requestManager = RequestManager.Instance;
        _deviceServiceUrl = "http://localhost:8080";
    }

    public async Task<ApiResult<TradeRecommendation>> GetRecommendationAsync(string symbol, string token = null)
    {
        var url = _deviceServiceUrl + _urlEncoder.Encode("/tradeRecommendations?symbol=" + symbol).ToString();
        
        var result =  await _requestManager.GetApiAsync<TradeRecommendation>(url, token);
        
        return result;
    }
}