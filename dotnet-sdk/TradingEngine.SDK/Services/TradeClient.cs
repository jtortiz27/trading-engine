using System.Net.Http.Json;
using TradingEngine.SDK.Models;

namespace TradingEngine.SDK.Services
{
    public class TradeClient
    {
        private readonly HttpClient _httpClient;

        public TradeClient(HttpClient httpClient)
        {
            _httpClient = httpClient;
        }

        public async Task<ApiResult<TradeRecommendation>> GetRecommendationAsync(string symbol)
        {
            try
            {
                var response = await _httpClient.GetAsync($"/recommendation/{symbol}");
                if (!response.IsSuccessStatusCode)
                {
                    var error = await response.Content.ReadFromJsonAsync<ErrorResponse>();
                    return new ApiResult<TradeRecommendation> { Success = false, Error = error?.Message ?? "Unknown error" };
                }

                var recommendation = await response.Content.ReadFromJsonAsync<TradeRecommendation>();
                return new ApiResult<TradeRecommendation> { Success = true, Data = recommendation };
            }
            catch (Exception ex)
            {
                return new ApiResult<TradeRecommendation> { Success = false, Error = ex.Message };
            }
        }
    }
}
