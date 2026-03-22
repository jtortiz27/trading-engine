namespace TradingEngine.SDK.Models;

public class ApiResult<T>
{
    public bool IsSuccess { get; set; }
    public T SuccessResult { get; set; }
    public ErrorResponse ErrorResult { get; set; }
}