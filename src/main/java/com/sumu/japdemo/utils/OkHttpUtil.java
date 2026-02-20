package com.sumu.japdemo.utils;

import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * OkHttp 工具类 - 增强版（含完整鉴权功能）
 * 支持多种认证方式：Basic Auth、Bearer Token、API Key、自定义签名
 */
@Component
public class OkHttpUtil {

    private static final Logger logger = LoggerFactory.getLogger(OkHttpUtil.class);

    // 支持的认证类型
    public enum AuthType {
        NONE,          // 无认证
        BASIC,         // Basic Auth
        BEARER_TOKEN,  // Bearer Token
        API_KEY,       // API Key
        SIGNATURE      // 自定义签名
    }

    // 单例客户端
    private final OkHttpClient client;

    // 当前认证配置
    private AuthConfig authConfig = new AuthConfig();

    // 签名算法常量
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String MD5 = "MD5";

    /**
     * 构造函数 - 初始化OkHttpClient
     */
    public OkHttpUtil() {
        this.client = createHttpClient();
    }

    /**
     * 创建配置好的 OkHttpClient
     */
    private OkHttpClient createHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(50, 5, TimeUnit.MINUTES))
                .retryOnConnectionFailure(true)
                .addInterceptor(this::logRequestResponse) // 添加日志拦截器
                .build();
    }

    /**
     * 请求/响应日志拦截器
     */
    private Response logRequestResponse(Interceptor.Chain chain) throws IOException {
        Request request = chain.request();

        // 记录请求信息
        logger.debug("Sending request to {} [{}] with headers: {}",
                request.url(), request.method(), request.headers());

        long startTime = System.nanoTime();
        Response response = chain.proceed(request);
        long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

        // 记录响应信息
        logger.debug("Received response from {} in {}ms [HTTP {}] with headers: {}",
                response.request().url(), duration, response.code(), response.headers());

        return response;
    }

    // ==================== 认证配置方法 ====================

    /**
     * 设置认证配置
     */
    public void setAuthConfig(AuthConfig config) {
        // 若 config 为 null，则 new AuthConfig()；否则保留 config
        this.authConfig = config != null ? config : new AuthConfig();
    }

    /**
     * 获取当前认证配置
     */
    public AuthConfig getAuthConfig() {
        return authConfig;
    }

    /**
     * 更新 Token
     */
    public void updateToken(String token) {
        authConfig.setToken(token);
        logger.info("Authentication token updated");
    }

    /**
     * 清除 Token
     */
    public void clearToken() {
        authConfig.setToken(null);
        logger.info("Authentication token cleared");
    }

    // ==================== 签名和加密方法 ====================

    /**
     * 生成签名
     */
    public String generateSignature(String secret, String method, String path,
                                    Map<String, String> params, String body)
            throws NoSuchAlgorithmException, InvalidKeyException {

        // 1. 规范化参数
        String queryString = normalizeParams(params);

        // 2. 创建待签名字符串
        String signString = method.toUpperCase() + "\n" +
                path + "\n" +
                queryString + "\n" +
                (body != null ? body : "") + "\n" +
                authConfig.getNonce() + "\n" +
                Instant.now().getEpochSecond();

        // 3. 计算签名
        return calculateHmac(secret, signString);
    }

    /**
     * 规范化参数
     */
    private String normalizeParams(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }

        return params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> encodeParam(entry.getKey()) + "=" + encodeParam(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    /**
     * URL 编码参数
     */
    private String encodeParam(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name())
                    .replace("+", "%20")
                    .replace("*", "%2A")
                    .replace("%7E", "~");
        } catch (Exception e) {
            return value;
        }
    }

    /**
     * 计算 HMAC-SHA256
     */
    private String calculateHmac(String secret, String data)
            throws NoSuchAlgorithmException, InvalidKeyException {

        Mac mac = Mac.getInstance(HMAC_SHA256);
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
        mac.init(secretKey);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

        // 转换为十六进制字符串
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * 计算 MD5
     */
    public String calculateMd5(String input) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(MD5);
        byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));

        StringBuilder hexString = new StringBuilder();
        for (byte b : digest) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    // ==================== HTTP请求方法 ====================

    /**
     * GET 请求
     */
    public String get(String url) throws IOException {
        return execute(buildRequest(url, "GET", null, null, null));
    }

    /**
     * GET 请求（带查询参数）
     */
    public String get(String url, Map<String, String> queryParams) throws IOException {
        return execute(buildRequest(url, "GET", queryParams, null, null));
    }

    /**
     * POST JSON 请求
     */
    public String postJson(String url, String jsonBody) throws IOException {
        return execute(buildRequest(url, "POST", null, jsonBody, "application/json"));
    }

    /**
     * POST 表单请求
     */
    public String postForm(String url, Map<String, String> formData) throws IOException {
        FormBody.Builder formBuilder = new FormBody.Builder();
        formData.forEach(formBuilder::add);
        RequestBody body = formBuilder.build();

        return execute(buildRequest(url, "POST", null, body, null));
    }

    /**
     * PUT JSON 请求
     */
    public String putJson(String url, String jsonBody) throws IOException {
        return execute(buildRequest(url, "PUT", null, jsonBody, "application/json"));
    }

    /**
     * DELETE 请求
     */
    public String delete(String url) throws IOException {
        return execute(buildRequest(url, "DELETE", null, null, null));
    }

    /**
     * PATCH JSON 请求
     */
    public String patchJson(String url, String jsonBody) throws IOException {
        return execute(buildRequest(url, "PATCH", null, jsonBody, "application/json"));
    }

    /**
     * 通用请求方法
     */
    public String executeRequest(String method, String url,
                                 Map<String, String> queryParams,
                                 Object body,
                                 String contentType) throws IOException {
        return execute(buildRequest(url, method, queryParams, body, contentType));
    }

    // ==================== 核心构建和执行方法 ====================

    /**
     * 构建请求对象
     */
    private Request buildRequest(String url, String method,
                                 Map<String, String> queryParams,
                                 Object body,
                                 String contentType) {

        // 1. 处理查询参数
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
        if (queryParams != null && !queryParams.isEmpty()) {
            queryParams.forEach(urlBuilder::addQueryParameter);
        }

        // 2. 创建请求构建器
        Request.Builder requestBuilder = new Request.Builder()
                .url(urlBuilder.build())
                .method(method.toUpperCase(), createRequestBody(body, contentType));

        // 3. 添加认证头
        addAuthHeaders(requestBuilder);

        // 4. 添加自定义头
        if (authConfig.getCustomHeaders() != null) {
            authConfig.getCustomHeaders().forEach(requestBuilder::addHeader);
        }

        return requestBuilder.build();
    }

    /**
     * 创建请求体
     */
    private RequestBody createRequestBody(Object body, String contentType) {
        if (body == null) {
            return null;
        }

        if (body instanceof String) {
            MediaType mediaType = contentType != null ?
                    MediaType.parse(contentType) :
                    MediaType.parse("text/plain; charset=utf-8");
            return RequestBody.create((String) body, mediaType);
        } else if (body instanceof Map) {
            // 表单数据
            FormBody.Builder formBuilder = new FormBody.Builder();
            ((Map<?, ?>) body).forEach((k, v) ->
                    formBuilder.add(k.toString(), v != null ? v.toString() : ""));
            return formBuilder.build();
        } else if (body instanceof RequestBody) {
            return (RequestBody) body;
        }

        throw new IllegalArgumentException("Unsupported body type: " + body.getClass());
    }

    /**
     * 添加认证头
     */
    private void addAuthHeaders(Request.Builder requestBuilder) {
        switch (authConfig.getAuthType()) {
            case BASIC:
                addBasicAuth(requestBuilder);
                break;
            case BEARER_TOKEN:
                addBearerToken(requestBuilder);
                break;
            case API_KEY:
                addApiKey(requestBuilder);
                break;
            case SIGNATURE:
                addSignature(requestBuilder);
                break;
            default:
                // 无认证
        }
    }

    /**
     * 添加 Basic Auth
     */
    private void addBasicAuth(Request.Builder builder) {
        if (StringUtils.hasText(authConfig.getUsername()) &&
                StringUtils.hasText(authConfig.getPassword())) {

            String credentials = authConfig.getUsername() + ":" + authConfig.getPassword();
            String encoded = Base64.getEncoder().encodeToString(credentials.getBytes());
            builder.header("Authorization", "Basic " + encoded);
        }
    }

    /**
     * 添加 Bearer Token
     */
    private void addBearerToken(Request.Builder builder) {
        if (StringUtils.hasText(authConfig.getToken())) {
            builder.header("Authorization", "Bearer " + authConfig.getToken());
        }
    }

    /**
     * 添加 API Key
     */
    private void addApiKey(Request.Builder builder) {
        if (StringUtils.hasText(authConfig.getApiKey())) {
            if ("header".equalsIgnoreCase(authConfig.getApiKeyLocation())) {
                builder.header(authConfig.getApiKeyName(), authConfig.getApiKey());
            } else if ("query".equalsIgnoreCase(authConfig.getApiKeyLocation())) {
                // API Key 作为查询参数需要在URL中添加
                // 这里简化处理，实际应该在URL构建时添加
                logger.warn("API Key location 'query' not fully implemented in this version");
            }
        }
    }

    /**
     * 添加签名认证
     */
    private void addSignature(Request.Builder builder) {
        try {
            if (StringUtils.hasText(authConfig.getSecretKey())) {
                // 生成签名所需参数
                String method = builder.build().method();
                String path = builder.build().url().encodedPath();
                Map<String, String> queryParams = new HashMap<>();

                // 从URL提取查询参数
                for (String name : builder.build().url().queryParameterNames()) {
                    queryParams.put(name, builder.build().url().queryParameter(name));
                }

                // 生成签名
                String signature = generateSignature(
                        authConfig.getSecretKey(),
                        method,
                        path,
                        queryParams,
                        null // 请求体签名需要额外处理
                );

                // 添加签名头
                builder.header("X-Signature", signature)
                        .header("X-Nonce", authConfig.getNonce())
                        .header("X-Timestamp", String.valueOf(Instant.now().getEpochSecond()));
            }
        } catch (Exception e) {
            logger.error("Failed to generate signature", e);
        }
    }

    /**
     * 执行请求并处理响应
     */
    private String execute(Request request) throws IOException {
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                logger.error("Request failed: {} - {}\nResponse: {}",
                        response.code(), response.message(), errorBody);
                throw new IOException("Request failed: " + response.code() + " - " + response.message());
            }

            ResponseBody body = response.body();
            return body != null ? body.string() : "";
        }
    }

    // ==================== 内部配置类 ====================

    /**
     * 认证配置类
     */
    public static class AuthConfig {
        private AuthType authType = AuthType.NONE;
        private String username;
        private String password;
        private String token;
        private String apiKey;
        private String apiKeyName = "X-API-KEY";
        private String apiKeyLocation = "header"; // header or query
        private String secretKey;
        private String nonce = UUID.randomUUID().toString();
        private Map<String, String> customHeaders;

        // Getters and Setters
        public AuthType getAuthType() { return authType; }
        public void setAuthType(AuthType authType) { this.authType = authType; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }

        public String getApiKeyName() { return apiKeyName; }
        public void setApiKeyName(String apiKeyName) { this.apiKeyName = apiKeyName; }

        public String getApiKeyLocation() { return apiKeyLocation; }
        public void setApiKeyLocation(String apiKeyLocation) { this.apiKeyLocation = apiKeyLocation; }

        public String getSecretKey() { return secretKey; }
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

        public String getNonce() { return nonce; }
        public void setNonce(String nonce) { this.nonce = nonce; }

        public Map<String, String> getCustomHeaders() { return customHeaders; }
        public void setCustomHeaders(Map<String, String> customHeaders) {
            this.customHeaders = customHeaders;
        }

        public void addCustomHeader(String key, String value) {
            if (customHeaders == null) {
                customHeaders = new HashMap<>();
            }
            customHeaders.put(key, value);
        }
    }
}