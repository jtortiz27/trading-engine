using System.Threading.Tasks;
using TradingEngine.SDK.Models;

namespace TradingEngine.SDK.Services;

public interface ITradeRecommendationService
{
    Task<ApiResult<TradeRecommendation>> GetRecommendationAsync(string symbol, string token);
}