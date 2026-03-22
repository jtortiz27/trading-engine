using TradingEngine.SDK.Models;
using TradingEngine.SDK.Services;

namespace mauiApp.Services;

public class TradeRecommendationAppService : ITradeRecommendationAppService
{
    private readonly ITradeRecommendationService _tradeRecommendationService;

    public TradeRecommendationAppService(ITradeRecommendationService tradeRecommendationService)
    {
        _tradeRecommendationService = tradeRecommendationService;
    }


    public async Task<TradeRecommendation> GetRecommendationAsync(string ticker, string token = null)
    {
        ApiResult<TradeRecommendation> apiResult = await _tradeRecommendationService.GetRecommendationAsync(ticker, token);

        return apiResult.IsSuccess ? apiResult.SuccessResult : null;
    }
    
}