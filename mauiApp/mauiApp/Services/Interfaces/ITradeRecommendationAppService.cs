using TradingEngine.SDK.Models;
using TradingEngine.SDK.Services;

namespace mauiApp.Services;

public interface ITradeRecommendationAppService
{

    Task<TradeRecommendation> GetRecommendationAsync(string ticker, string token);
    

}