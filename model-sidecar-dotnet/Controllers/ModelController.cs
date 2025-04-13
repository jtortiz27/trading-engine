using Microsoft.AspNetCore.Mvc;
using Microsoft.ML.OnnxRuntime;
using Microsoft.ML.OnnxRuntime.Tensors;
using TradingEngine.SDK.Models;

[ApiController]
[Route("[controller]")]
public class ModelController : ControllerBase
{
    private static InferenceSession _session = new InferenceSession("shared-models/onnx/trade-recommender.onnx");

    [HttpPost("infer")]
    public IActionResult Infer([FromBody] StockFeatures features)
    {
        var inputs = new List<NamedOnnxValue>
        {
            NamedOnnxValue.CreateFromTensor("input", features.ToTensor())
        };
        using var results = _session.Run(inputs);
        var prediction = TradeRecommendation.FromResults(results);
        return Ok(prediction);
    }

    [HttpPost("reload")]
    public IActionResult Reload()
    {
        _session.Dispose();
        _session = new InferenceSession("shared-models/onnx/trade-recommender.onnx");
        return Ok("Model reloaded");
    }
}
