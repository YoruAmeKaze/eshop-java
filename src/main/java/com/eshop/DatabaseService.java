package com.eshop;

import jakarta.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * DatabaseService — 数据库操作服务
 *
 * 类比 Python backend/database.py
 * Python 中分 3 个类：user_database / shop / cart
 * 这里用 3 个区域隔开，结构完全一致
 */
@Service
public class DatabaseService {

    // EntityManager = Python 的 pymysql 连接 + cursor
    @PersistenceContext
    private EntityManager em;

    /* ==========================================================
     * 用户相关 — 类比 Python class user_database
     * ========================================================== */

    /** 注册 — 类比 Python: create_user(info) */
    @Transactional
    public Map<String, Object> createUser(String name, String password, String roles, String tel) {
        // 检查空值 — 类比 check_empty
        if (isEmpty(name))  return error("用户名不能为空");
        if (isEmpty(password)) return error("密码不能为空");
        if (!roles.equals("consumer") && !roles.equals("merchant"))
            return error("角色必须是consumer或merchant");

        // 检查重名 — 类比 check_exist
        if (!isNameAvailable(name)) return error("用户名已存在");

        // 插入 — 类比 insert into users ...
        UserEntity u = new UserEntity();
        u.name = name;
        u.password = AuthUtil.hashPassword(password);
        u.roles = roles;
        u.tel = tel;
        em.persist(u);

        return success("用户创建成功");
    }

    /** 登录 — 类比 Python: login(info) */
    public Map<String, Object> login(String name, String password) {
        if (isEmpty(name))  return error("用户名不能为空");
        if (isEmpty(password)) return error("密码不能为空");

        try {
            UserEntity u = findUserByName(name);
            if (u == null) return error("用户不存在");
            if (!AuthUtil.verifyPassword(password, u.password))
                return error("密码错误");

            Map<String, Object> data = new HashMap<>();
            data.put("status", "success");
            data.put("message", "登录成功");
            data.put("roles", u.roles);
            data.put("id", u.id);
            return data;
        } catch (NoResultException e) {
            return error("用户不存在");
        }
    }

    /** 查用户 — 类比 Python: find_users(id) */
    public Map<String, Object> findUserById(Integer id) {
        UserEntity u = em.find(UserEntity.class, id);
        if (u == null) return error("用户不存在");

        Map<String, Object> data = new HashMap<>();
        data.put("id", u.id);
        data.put("name", u.name);
        data.put("tel", u.tel);
        data.put("roles", u.roles);

        Map<String, Object> result = success("用户查询成功");
        result.put("data", data);
        return result;
    }

    /* ==========================================================
     * 商品相关 — 类比 Python class shop
     * ========================================================== */

    /** 商家查看自己商品 — 类比: list_goods_merchant(id) */
    public Map<String, Object> listGoodsByMerchant(Integer merchantId) {
        List<GoodsEntity> list = em.createQuery(
                "SELECT g FROM GoodsEntity g WHERE g.merchantId = :mid", GoodsEntity.class)
                .setParameter("mid", merchantId)
                .getResultList();

        return successData(list.stream().map(this::goodsToMap).toList(), "查询成功");
    }

    /** 商家添加商品 — 类比: add_goods(merchant_id, name, desc, price, category, image_url) */
    @Transactional
    public Map<String, Object> addGoods(Integer merchantId, String name, String desc,
                                         BigDecimal price, String category, String imageUrl) {
        GoodsEntity g = new GoodsEntity();
        g.merchantId = merchantId;
        g.name = name;
        g.description = desc;
        g.price = price;
        g.category = category;
        g.imageUrl = imageUrl;
        em.persist(g);
        em.flush(); // 获取自增ID

        return successData(goodsToMap(g), "添加成功");
    }

    /** 商家修改商品 — 类比: update_goods(merchant_id, id, name, ...) */
    @Transactional
    public Map<String, Object> updateGoods(Integer merchantId, Integer id, String name, String desc,
                                            BigDecimal price, String category, String imageUrl) {
        GoodsEntity g = em.find(GoodsEntity.class, id);
        if (g == null || !g.merchantId.equals(merchantId))
            return error("商品不存在");

        g.name = name;
        g.description = desc;
        g.price = price;
        g.category = category;
        g.imageUrl = imageUrl;

        return successData(goodsToMap(g), "修改成功");
    }

    /** 商家删除商品 — 类比: delete_goods(merchant_id, id) */
    @Transactional
    public Map<String, Object> deleteGoods(Integer merchantId, Integer id) {
        GoodsEntity g = em.find(GoodsEntity.class, id);
        if (g == null || !g.merchantId.equals(merchantId))
            return error("商品不存在");
        em.remove(g);
        return success("删除成功");
    }

    /** 随机推荐 — 类比: random_goods(category) */
    public Map<String, Object> randomGoods(String category) {
        List<GoodsEntity> list;
        if (category != null && !category.isEmpty()) {
            list = em.createNativeQuery(
                    "SELECT * FROM goods WHERE category = ? ORDER BY RAND() LIMIT 10", GoodsEntity.class)
                    .setParameter(1, category)
                    .getResultList();
        } else {
            list = em.createNativeQuery(
                    "SELECT * FROM goods ORDER BY RAND() LIMIT 10", GoodsEntity.class)
                    .getResultList();
        }
        return successData(list.stream().map(this::goodsToMap).toList(), "查询成功");
    }

    /** 分页浏览 — 类比: list_goods_consumer(page, limit, category) */
    public Map<String, Object> listGoods(int page, int limit, String category) {
        if (category == null || category.isEmpty() || "null".equals(category) || "undefined".equals(category))
            category = null;

        String jpql = category == null
                ? "SELECT g FROM GoodsEntity g ORDER BY g.id"
                : "SELECT g FROM GoodsEntity g WHERE g.category = :cat ORDER BY g.id";

        TypedQuery<GoodsEntity> query = category == null
                ? em.createQuery(jpql, GoodsEntity.class)
                : em.createQuery(jpql, GoodsEntity.class).setParameter("cat", category);

        // 分页
        int total = query.getResultList().size();
        List<GoodsEntity> list = query
                .setFirstResult((page - 1) * limit)
                .setMaxResults(limit)
                .getResultList();

        Map<String, Object> result = successData(
                list.stream().map(this::goodsToMap).toList(), "查询成功");
        result.put("total_pages", (int) Math.ceil((double) total / limit));
        return result;
    }

    /** 商品详情 — 类比: get_product_detail(id) */
    public Map<String, Object> getProductDetail(Integer id) {
        GoodsEntity g = em.find(GoodsEntity.class, id);
        if (g == null) return error("商品不存在");
        return successData(goodsToMap(g), "查询成功");
    }

    /* ==========================================================
     * 购物车相关 — 类比 Python class cart
     * ========================================================== */

    /** 添加购物车 — 类比: add_to_cart(user_id, goods_id, quantity) */
    @Transactional
    public Map<String, Object> addToCart(Integer userId, Integer goodsId, Integer quantity) {
        // 查找是否已有 — 类比 ON DUPLICATE KEY UPDATE
        CartEntity existing = findCartItem(userId, goodsId);
        if (existing != null) {
            existing.quantity += quantity;
        } else {
            CartEntity c = new CartEntity();
            c.userId = userId;
            c.goodsId = goodsId;
            c.quantity = quantity;
            em.persist(c);
        }
        return success("添加到购物车成功");
    }

    /** 删除购物车项 — 类比: delete_from_cart(user_id, goods_id) */
    @Transactional
    public Map<String, Object> deleteFromCart(Integer userId, Integer goodsId) {
        em.createQuery("DELETE FROM CartEntity c WHERE c.userId = :uid AND c.goodsId = :gid")
                .setParameter("uid", userId)
                .setParameter("gid", goodsId)
                .executeUpdate();
        return success("从购物车删除成功");
    }

    /** 调整数量 — 类比: adjust_number(user_id, goods_id, quantity) */
    @Transactional
    public Map<String, Object> adjustCartQuantity(Integer userId, Integer goodsId, Integer quantity) {
        em.createQuery("UPDATE CartEntity c SET c.quantity = :q WHERE c.userId = :uid AND c.goodsId = :gid")
                .setParameter("q", quantity)
                .setParameter("uid", userId)
                .setParameter("gid", goodsId)
                .executeUpdate();
        return success("调整购物车数量成功");
    }

    /** 查看购物车 — 类比: get_cart(user_id)  JOIN goods */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getCart(Integer userId) {
        List<Object[]> rows = em.createNativeQuery(
                "SELECT c.id, c.goods_id, c.quantity, g.name, g.price, g.image_url " +
                "FROM cart c JOIN goods g ON c.goods_id = g.id WHERE c.user_id = ?")
                .setParameter(1, userId)
                .getResultList();

        List<Map<String, Object>> data = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("cart_id",   row[0]);
            item.put("goods_id",  row[1]);
            item.put("quantity",  row[2]);
            item.put("name",      row[3]);
            item.put("price",     row[4]);
            item.put("image_url", row[5]);
            data.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("data", data);
        return result;
    }

    /* ==========================================================
     * 内部实体类 — 类比 MySQL 表结构
     * ========================================================== */

    // ── 用户表 ── 类比 CREATE TABLE users
    @Entity
    @Table(name = "users")
    public static class UserEntity {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
        public Integer id;
        @Column(nullable = false, length = 16)  public String name;
        @Column(nullable = false, length = 64)  public String password;
        @Column(length = 20)  public String tel;
        @Column(length = 16)  public String roles = "consumer";
    }

    // ── 商品表 ── 类比 CREATE TABLE goods
    @Entity
    @Table(name = "goods")
    public static class GoodsEntity {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
        public Integer id;
        @Column(name = "merchant_id", nullable = false)  public Integer merchantId;
        @Column(nullable = false, length = 100)  public String name;
        @Column(columnDefinition = "TEXT")  public String description;
        @Column(nullable = false, precision = 10, scale = 2)  public BigDecimal price;
        @Column(length = 50)  public String category;
        @Column(name = "image_url", length = 255)  public String imageUrl;
        @Column(name = "created_at", updatable = false)  public LocalDateTime createdAt;
        @PrePersist  void onC() { createdAt = LocalDateTime.now(); }
    }

    // ── 购物车表 ── 类比 CREATE TABLE cart
    @Entity
    @Table(name = "cart", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "goods_id"}))
    public static class CartEntity {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
        public Integer id;
        @Column(name = "user_id", nullable = false)  public Integer userId;
        @Column(name = "goods_id", nullable = false)  public Integer goodsId;
        @Column(nullable = false)  public Integer quantity = 1;
        @Column(name = "created_at", updatable = false)  public LocalDateTime createdAt;
        @PrePersist  void onC() { createdAt = LocalDateTime.now(); }
    }

    /* ==========================================================
     * 内部辅助方法
     * ========================================================== */

    private boolean isEmpty(String s) { return s == null || s.trim().isEmpty(); }

    private boolean isNameAvailable(String name) {
        Long count = em.createQuery(
                "SELECT COUNT(u) FROM UserEntity u WHERE u.name = :n", Long.class)
                .setParameter("n", name)
                .getSingleResult();
        return count == 0;
    }

    private UserEntity findUserByName(String name) {
        try {
            return em.createQuery(
                    "SELECT u FROM UserEntity u WHERE u.name = :n", UserEntity.class)
                    .setParameter("n", name)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    private CartEntity findCartItem(Integer userId, Integer goodsId) {
        try {
            return em.createQuery(
                    "SELECT c FROM CartEntity c WHERE c.userId = :uid AND c.goodsId = :gid", CartEntity.class)
                    .setParameter("uid", userId)
                    .setParameter("gid", goodsId)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    private Map<String, Object> goodsToMap(GoodsEntity g) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", g.id);
        m.put("merchant_id", g.merchantId);
        m.put("name", g.name);
        m.put("description", g.description);
        m.put("price", g.price);
        m.put("category", g.category);
        m.put("image_url", g.imageUrl);
        return m;
    }

    private Map<String, Object> success(String msg) {
        Map<String, Object> r = new HashMap<>();
        r.put("status", "success"); r.put("message", msg); return r;
    }

    private Map<String, Object> successData(Object data, String msg) {
        Map<String, Object> r = success(msg);
        r.put("data", data); return r;
    }

    private Map<String, Object> error(String msg) {
        Map<String, Object> r = new HashMap<>();
        r.put("status", "error"); r.put("message", msg); return r;
    }
}
