using TradingEngine.SDK.Services;
using TradingEngine.SDK.Models;

public partial class MainPage : ContentPage
{
    private readonly TradeClient _tradeClient = new(new HttpClient { BaseAddress = new Uri("https://your-api-url.com") });

    public MainPage()
    {
        InitializeComponent();
    }

    private async void OnGetRecommendationClicked(object sender, EventArgs e)
    {
        string symbol = SymbolEntry.Text;
        var result = await _tradeClient.GetRecommendationAsync(symbol);

        ResultLabel.Text = result.Success && result.Data is not null
            ? $"{result.Data.Action} {result.Data.Symbol} (Confidence: {result.Data.Confidence:P2})"
            : $"Error: {result.Error}";
    }
}
