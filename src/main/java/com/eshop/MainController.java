package com.eshop;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * MainController — 所有 API 路由
 *
 * 类比 Python backend/main.py
 * Python main.py 中有：
 *   class RegisterRequest / LoginRequest / ProductRequest / CartAddRequest / CartUpdateRequest
 *   以及所有 @app.get/post/put/delete 路由
 */
@RestController
@RequestMapping("/api")
public class MainController {

    private final DatabaseService db;
    private final AuthUtil auth;

    // 有效分类 — 类比 Python: VALID_CATEGORIES
    private static final Set<String> VALID_CATEGORIES = Set.of(
            "electronic", "clothes", "living", "beauty",
            "sports", "baby", "food", "book", "others"
    );

    public MainController(DatabaseService db, AuthUtil auth) {
        this.db = db;
        this.auth = auth;
    }

    /* ==========================================================
     * 认证相关 — 类比 Python /api/register, /api/login, /api/user/me
     * ========================================================== */

    /** POST /api/register — 注册 */
    @PostMapping("/register")
    public Map<String, Object> register(@Valid @RequestBody RegisterRequest req) {
        return db.createUser(req.name, req.password, req.roles, req.tel);
    }

    /** POST /api/login — 登录 */
    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody LoginRequest req) {
        Map<String, Object> result = db.login(req.name, req.password);
        if ("success".equals(result.get("status"))) {
            String token = auth.generateToken((Integer) result.get("id"), (String) result.get("roles"));
            result.put("token", token);
        }
        return result;
    }

    /** GET /api/user/me — 获取当前用户信息 */
    @GetMapping("/user/me")
    public Map<String, Object> getMe(@RequestHeader("Authorization") String authHeader) {
        Integer userId = extractUserId(authHeader);
        return db.findUserById(userId);
    }

    /* ==========================================================
     * 商品相关 — 类比 Python /api/merchant/products, /api/products
     * ========================================================== */

    /** GET /api/merchant/products — 商家查看自己商品 */
    @GetMapping("/merchant/products")
    public Map<String, Object> getMerchantProducts(@RequestHeader("Authorization") String authHeader) {
        Integer merchantId = extractUserId(authHeader);
        return db.listGoodsByMerchant(merchantId);
    }

    /** POST /api/merchant/products — 商家添加商品 */
    @PostMapping("/merchant/products")
    public Map<String, Object> addProduct(@Valid @RequestBody ProductRequest req,
                                           @RequestHeader("Authorization") String authHeader) {
        if (req.category != null && !VALID_CATEGORIES.contains(req.category))
            return error("无效的分类");
        Integer merchantId = extractUserId(authHeader);
        return db.addGoods(merchantId, req.name, req.description, req.price, req.category, req.imageUrl);
    }

    /** PUT /api/merchant/products/{id} — 商家修改商品 */
    @PutMapping("/merchant/products/{id}")
    public Map<String, Object> updateProduct(@PathVariable Integer id,
                                              @Valid @RequestBody ProductRequest req,
                                              @RequestHeader("Authorization") String authHeader) {
        if (req.category != null && !VALID_CATEGORIES.contains(req.category))
            return error("无效的分类");
        Integer merchantId = extractUserId(authHeader);
        return db.updateGoods(merchantId, id, req.name, req.description, req.price, req.category, req.imageUrl);
    }

    /** DELETE /api/merchant/products/{id} — 商家删除商品 */
    @DeleteMapping("/merchant/products/{id}")
    public Map<String, Object> deleteProduct(@PathVariable Integer id,
                                              @RequestHeader("Authorization") String authHeader) {
        Integer merchantId = extractUserId(authHeader);
        return db.deleteGoods(merchantId, id);
    }

    /** GET /api/products/random — 随机推荐 */
    @GetMapping("/products/random")
    public Map<String, Object> getRandomProducts(@RequestParam(required = false) String category) {
        return db.randomGoods(category);
    }

    /** GET /api/products — 分页浏览 */
    @GetMapping("/products")
    public Map<String, Object> getProducts(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int limit,
                                            @RequestParam(required = false) String category) {
        return db.listGoods(page, limit, category);
    }

    /** GET /api/products/{id} — 商品详情 */
    @GetMapping("/products/{id}")
    public Map<String, Object> getProductDetail(@PathVariable Integer id) {
        return db.getProductDetail(id);
    }

    /* ==========================================================
     * 购物车相关 — 类比 Python /api/cart
     * ========================================================== */

    /** GET /api/cart — 查看购物车 */
    @GetMapping("/cart")
    public Map<String, Object> getCart(@RequestHeader("Authorization") String authHeader) {
        Integer userId = extractUserId(authHeader);
        return db.getCart(userId);
    }

    /** POST /api/cart — 添加购物车 */
    @PostMapping("/cart")
    public Map<String, Object> addToCart(@Valid @RequestBody CartAddRequest req,
                                          @RequestHeader("Authorization") String authHeader) {
        Integer userId = extractUserId(authHeader);
        return db.addToCart(userId, req.goodsId, req.quantity);
    }

    /** DELETE /api/cart/{goodsId} — 删除购物车项 */
    @DeleteMapping("/cart/{goodsId}")
    public Map<String, Object> deleteFromCart(@PathVariable Integer goodsId,
                                               @RequestHeader("Authorization") String authHeader) {
        Integer userId = extractUserId(authHeader);
        return db.deleteFromCart(userId, goodsId);
    }

    /** PUT /api/cart/{goodsId} — 调整数量 */
    @PutMapping("/cart/{goodsId}")
    public Map<String, Object> updateCart(@PathVariable Integer goodsId,
                                           @RequestBody Map<String, Integer> body,
                                           @RequestHeader("Authorization") String authHeader) {
        Integer userId = extractUserId(authHeader);
        Integer quantity = body.getOrDefault("quantity", 1);
        return db.adjustCartQuantity(userId, goodsId, quantity);
    }

    /* ==========================================================
     * 内部方法
     * ========================================================== */

    /** 从 Authorization Header 提取用户ID — 类比 Python 的 auth.decode_token(token) */
    private Integer extractUserId(String authHeader) {
        if (authHeader == null || authHeader.isEmpty())
            throw new RuntimeException("缺少token");
        String token = authHeader;
        if (token.startsWith("Bearer ")) token = token.substring(7);
        return auth.getUserIdFromToken(token);
    }

    private Map<String, Object> error(String msg) {
        Map<String, Object> r = new HashMap<>();
        r.put("status", "error"); r.put("message", msg); return r;
    }

    /* ==========================================================
     * 请求体 DTO — 类比 Python 的 Pydantic models
     * Python: class RegisterRequest(BaseModel) ... 都在 main.py 里
     * 这里也用内部类，放在同一个文件
     * ========================================================== */

    /** 登录请求 — 类比 Python: class LoginRequest(BaseModel) */
    public static class LoginRequest {
        @NotBlank public String name;
        @NotBlank public String password;
    }

    /** 注册请求 — 类比 Python: class RegisterRequest(BaseModel) */
    public static class RegisterRequest {
        @NotBlank public String name;
        @NotBlank public String password;
        @NotBlank public String roles;
        public String tel;
    }

    /** 商品请求 — 类比 Python: class ProductRequest(BaseModel) */
    public static class ProductRequest {
        @NotBlank public String name;
        public String description;
        @NotNull public BigDecimal price;
        @NotBlank public String category;
        public String imageUrl;
    }

    /** 添加购物车请求 — 类比 Python: class CartAddRequest(BaseModel) */
    public static class CartAddRequest {
        @NotNull public Integer goodsId;
        public Integer quantity = 1;
    }

    /* ==========================================================
     * 全局异常处理 — 类比 Python 的 HTTPException
     * ========================================================== */

    /** 参数校验失败 — 捕获 @Valid 抛出的异常 */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, Object> handleValidation(MethodArgumentNotValidException ex) {
        FieldError fe = ex.getBindingResult().getFieldError();
        return error(fe != null ? fe.getDefaultMessage() : "参数校验失败");
    }

    /** 通用异常 — 捕获其他运行时错误 */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public Map<String, Object> handleOther(Exception ex) {
        return error("服务器内部错误: " + ex.getMessage());
    }
}
