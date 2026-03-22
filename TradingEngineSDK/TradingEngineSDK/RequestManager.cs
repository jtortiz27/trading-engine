using System;
using System.IO;
using System.Net;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;
using System.Threading.Tasks;
using TradingEngine.SDK.Models;

namespace TradingEngine.SDK;

internal sealed class RequestManager
{
    private static readonly Lazy<RequestManager> LazyRequestManager = new();
    private readonly JsonSerializerOptions _jsonOptions;
    private HttpClient _client;

    public RequestManager()
    {
        //Determine which HttpClient to Initialize
        ConfigureHttpClient();

        //Configure Json.Net
        _jsonOptions = new JsonSerializerOptions
        {
            DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull, // NullValueHandling.Ignore
            PropertyNamingPolicy = JsonNamingPolicy.CamelCase, // ContractResolver = CamelCase
            WriteIndented = false, // optional, for pretty-printing
        };
    }

    public static RequestManager Instance => LazyRequestManager.Value;

    public void ConfigureHttpClient()
    {
        //Cleanup any potentially old reference to HTTPClient
        _client?.Dispose();

        //Create new HttpClient with explicit constructor, enable automatic decompression for non-xamarin platforms
        _client = new HttpClient(new HttpClientHandler
            {
                AutomaticDecompression = DecompressionMethods.GZip
            })
            { Timeout = TimeSpan.FromMilliseconds(20000) };
    }

    public async Task<ApiResult<T>> GetApiAsync<T>(string url, string token = null)
    {
        var apiResult = new ApiResult<T>();

        try
        {
            var request = new HttpRequestMessage
            {
                RequestUri = new Uri(url),
                Method = HttpMethod.Get
            };
            request.Headers.AcceptEncoding.Add(new StringWithQualityHeaderValue("gzip"));

            if (!string.IsNullOrWhiteSpace(token)) request.Headers.Add("Authorization", token);

            using (var response = await _client.SendAsync(request))
            {
                if (response.IsSuccessStatusCode)
                {
                    apiResult.IsSuccess = true;

                    using (var s = await response.Content.ReadAsStreamAsync())
                    {
                        if (s.Length > 0)
                            apiResult.SuccessResult = JsonSerializer.Deserialize<T>(s, _jsonOptions);
                    }
                }
                else if (response.StatusCode == HttpStatusCode.NotFound)
                {
                    apiResult.IsSuccess = true;
                }
                else
                {
                    apiResult.IsSuccess = false;

                    using (var s = await response.Content.ReadAsStreamAsync())
                    {
                        if (s.Length > 0)
                        {
                            apiResult.ErrorResult =
                                await JsonSerializer.DeserializeAsync<ErrorResponse>(s, _jsonOptions);
                        }
                    }

                    //If Status Code is missing, apply it
                    if (apiResult.ErrorResult == null) apiResult.ErrorResult = new ErrorResponse();

                    apiResult.ErrorResult.Code = response.StatusCode.ToString();
                }
            }
        }
        catch (TaskCanceledException ex)
        {
            apiResult.IsSuccess = false;
            apiResult.ErrorResult = new ErrorResponse { Code = "REQUEST_CANCELED", Message = ex.Message };
        }
        catch (HttpRequestException ex)
        {
            apiResult.IsSuccess = false;
            apiResult.ErrorResult = new ErrorResponse { Code = "HTTP_ERROR", Message = ex.Message };
        }
        catch (Exception ex)
        {
            apiResult.IsSuccess = false;
            apiResult.ErrorResult = new ErrorResponse { Code = "EXCEPTION", Message = ex.Message };
        }

        return apiResult;
    }

    public async Task<ApiResult<T>> PostApiAsync<T>(string url, object data, string token = null)
    {
        var apiResult = new ApiResult<T>();

        var inputJson = SerializeJson(data);

        if (string.IsNullOrWhiteSpace(inputJson)) apiResult.IsSuccess = false;

        try
        {
            var request = new HttpRequestMessage
            {
                RequestUri = new Uri(url),
                Method = new HttpMethod("POST"),
                Content = new StringContent(inputJson, Encoding.UTF8, "application/json")
            };

            if (!string.IsNullOrWhiteSpace(token)) request.Headers.Add("Authorization", token);
            using (var response = await _client.SendAsync(request))
            {
                if (response.IsSuccessStatusCode)
                {
                    apiResult.IsSuccess = true;

                    //string someText = await response.Content.ReadAsStringAsync();

                    using (var s = await response.Content.ReadAsStreamAsync())
                    {
                        if (s.Length > 0)
                        {
                            apiResult.SuccessResult = JsonSerializer.Deserialize<T>(s, _jsonOptions);
                        }

                    }
                }
                else if (response.StatusCode == HttpStatusCode.NotFound)
                {
                    apiResult.IsSuccess = true;
                }
                else
                {
                    apiResult.IsSuccess = false;
                    
                    using (var s = await response.Content.ReadAsStreamAsync())
                    {
                        if (s.Length > 0)
                            apiResult.ErrorResult = JsonSerializer.Deserialize<ErrorResponse>(s, _jsonOptions);
                    }

                    //If Status Code is missing, apply it
                    if (apiResult.ErrorResult == null) apiResult.ErrorResult = new ErrorResponse();
                    apiResult.ErrorResult.Code = response.StatusCode.ToString();
                }
            }
        }
        catch (TaskCanceledException ex)
        {
            apiResult.IsSuccess = false;
            apiResult.ErrorResult = new ErrorResponse { Code = "REQUEST_CANCELED", Message = ex.Message };
        }
        catch (HttpRequestException ex)
        {
            apiResult.IsSuccess = false;
            apiResult.ErrorResult = new ErrorResponse { Code = "HTTP_ERROR", Message = ex.Message };
        }
        catch (Exception ex)
        {
            apiResult.IsSuccess = false;
            apiResult.ErrorResult = new ErrorResponse { Code = "EXCEPTION", Message = ex.Message };
        }

        return apiResult;
    }

    public async Task<ApiResult<T>> PatchApiAsync<T>(string url, T data, string token = null)
    {
        var apiResult = new ApiResult<T>();

        var inputJson = SerializeJson(data);
        if (string.IsNullOrWhiteSpace(inputJson)) apiResult.IsSuccess = false;

        try
        {
            var request = new HttpRequestMessage
            {
                RequestUri = new Uri(url),
                Method = new HttpMethod("PATCH"),
                Content = new StringContent(inputJson, Encoding.Unicode, "application/json")
            };

            if (!string.IsNullOrWhiteSpace(token)) request.Headers.Add("Authorization", token);
            using (var response = await _client.SendAsync(request))
            {
                if (response.IsSuccessStatusCode)
                {
                    apiResult.IsSuccess = true;

                    using (var s = await response.Content.ReadAsStreamAsync())
                    {
                        if (s.Length > 0)
                        {
                            apiResult.SuccessResult = JsonSerializer.Deserialize<T>(s, _jsonOptions);
                        }
                    }
                }
                else if (response.StatusCode == HttpStatusCode.NotFound)
                {
                    apiResult.IsSuccess = true;
                }
                else
                {
                    apiResult.IsSuccess = false;

                    using (var s = await response.Content.ReadAsStreamAsync())
                    {
                        if (s.Length > 0)
                        {
                            apiResult.ErrorResult = JsonSerializer.Deserialize<ErrorResponse>(s, _jsonOptions);
                        }
                    }

                    //If Status Code is missing, apply it
                    if (apiResult.ErrorResult == null) apiResult.ErrorResult = new ErrorResponse();
                    apiResult.ErrorResult.Code = response.StatusCode.ToString();
                }
            }
        }
        catch (TaskCanceledException ex)
        {
            apiResult.IsSuccess = false;
            apiResult.ErrorResult = new ErrorResponse { Code = "REQUEST_CANCELED", Message = ex.Message };
        }
        catch (HttpRequestException ex)
        {
            apiResult.IsSuccess = false;
            apiResult.ErrorResult = new ErrorResponse { Code = "HTTP_ERROR", Message = ex.Message };
        }
        catch (Exception ex)
        {
            apiResult.IsSuccess = false;
            apiResult.ErrorResult = new ErrorResponse { Code = "EXCEPTION", Message = ex.Message };
        }

        return apiResult;
    }

    public async Task<ApiResult<T>> DelApiAsync<T>(string url, string token = null)
    {
        var apiResult = new ApiResult<T>();
        try
        {
            var request = new HttpRequestMessage
            {
                RequestUri = new Uri(url),
                Method = HttpMethod.Delete
            };

            if (!string.IsNullOrWhiteSpace(token)) request.Headers.Add("Authorization", token);

            using (var response = await _client.SendAsync(request))
            {
                if (response.IsSuccessStatusCode)
                {
                    apiResult.IsSuccess = true;
                }
                else
                {
                    apiResult.IsSuccess = false;

                    using (var s = await response.Content.ReadAsStreamAsync())
                    {
                        if (s.Length > 0)
                        {
                            apiResult.ErrorResult = JsonSerializer.Deserialize<ErrorResponse>(s, _jsonOptions);
                        }
                    }

                    //If Status Code is missing, apply it
                    if (apiResult.ErrorResult == null) apiResult.ErrorResult = new ErrorResponse();
                    apiResult.ErrorResult.Code = response.StatusCode.ToString();
                }
            }
        }
        catch (TaskCanceledException ex)
        {
            apiResult.IsSuccess = false;
            apiResult.ErrorResult = new ErrorResponse { Code = "REQUEST_CANCELED", Message = ex.Message };
        }
        catch (HttpRequestException ex)
        {
            apiResult.IsSuccess = false;
            apiResult.ErrorResult = new ErrorResponse { Code = "HTTP_ERROR", Message = ex.Message };
        }
        catch (Exception ex)
        {
            apiResult.IsSuccess = false;
            apiResult.ErrorResult = new ErrorResponse { Code = "EXCEPTION", Message = ex.Message };
        }

        return apiResult;
    }

    #region JsonSerialization

    private string SerializeJson<T>(T data)
    {
        try
        {
            var json = JsonSerializer.Serialize(data);
            //string json = JsonConvert.SerializeObject(data, typeof(T), _jsonOptions);

            return json;
        }
        catch
        {
            return null;
        }
    }

    #endregion
}