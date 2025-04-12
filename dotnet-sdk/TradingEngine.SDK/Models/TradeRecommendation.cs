namespace TradingEngine.SDK.Models
{
    public class TradeRecommendation
    {
        public string Symbol { get; set; }
        public string Action { get; set; }
        public double Confidence { get; set; }
    }
}
