using Microsoft.AspNetCore.Mvc;
using TradingEngine.SDK.Models;
using TradingEngine.SDK.Services;

[ApiController]
[Route("api/[controller]")]
public class RecommendationController : ControllerBase
{
    private readonly TradeClient _tradeClient;

    public RecommendationController(TradeClient tradeClient)
    {
        _tradeClient = tradeClient;
    }

    [HttpGet("{symbol}")]
    public async Task<IActionResult> GetRecommendation(string symbol)
    {
        var result = await _tradeClient.GetRecommendationAsync(symbol);
        return result.Success ? Ok(result.Data) : BadRequest(result.Error);
    }
}
