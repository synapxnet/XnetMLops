package com.synapxnet.mlopslogin.controller;

import com.synapxnet.mlopslogin.entity.User;
import com.synapxnet.mlopslogin.mapper.UserMapper;
import com.synapxnet.mlopslogin.security.jwt.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

class UserControllerService {

}

@RestController
@RequestMapping("/api")
public class UserController {
    private static final Set<String> DEMO_PHONES = Set.of("17870171303", "15870171303");
    private static final String DEMO_VERIFICATION_CODE = "000000";

    @Resource
    private UserMapper userMapper;

    @Resource
    private JwtUtil jwtUtil;

    @GetMapping("/test")
    public String getUsers() {
        return "user";
    }
    // 修改点1：使用 StringRedisTemplate 替代 RedisTemplate
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            String userPhone = request.getUserPhone();
            String code = request.getCode();

            if (!DEMO_PHONES.contains(userPhone) || !DEMO_VERIFICATION_CODE.equals(code)) {
                return ResponseEntity.status(401).body(Map.of(
                        "code", 401,
                        "message", "手机号或验证码错误"
                ));
            }

            User user = userMapper.findByPhone(userPhone);
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of(
                        "code", 404,
                        "message", "User not found"
                ));
            }

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime failureTime = user.getRoles_failure_time();
            boolean isExpired = failureTime != null && (failureTime.isBefore(now) || failureTime.isEqual(now));

            Map<String, Object> userData = new HashMap<>();
            userData.put("phone", user.getPhone());
            userData.put("realName", user.getRealName());
            userData.put("userId", user.getUserId());
            userData.put("roles", isExpired ? null : user.getRoles());
            String token = jwtUtil.generateToken(user.getPhone());
            userData.put("accessToken", token);
            return ResponseEntity.ok(Map.of(
                    "code", isExpired ? 403 : 0,
                    "message", isExpired ? "no" : "ok",
                    "data", userData,
                    "error",isExpired ? "权限到期":"null"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Server error: " + e.getMessage());
        }
    }
    @PostMapping("/auth/logout")
    public ResponseEntity<?> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "redirect", required = false, defaultValue = "false") boolean redirect
    ) {
        try {
            // 1. 如果存在 Token，将其加入黑名单
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.replace("Bearer ", "").trim();
                Claims claims = jwtUtil.extractAllClaims(token);
                Date expiration = claims.getExpiration();
                long ttl = expiration.getTime() - System.currentTimeMillis();

                if (ttl > 0) {
                    stringRedisTemplate.opsForValue().set(
                            "logout:" + token,
                            "invalid",
                            ttl,
                            TimeUnit.MILLISECONDS
                    );
                }
            }

            // 2. 根据 redirect 参数决定是否重定向（此处强制返回 JSON）
            if (redirect) {
                // 如果需要重定向，可以跳转到指定页面（但根据需求，此处直接返回 JSON）
                return ResponseEntity.status(HttpStatus.OK).body(Map.of(
                        "code", 0,
                        "data", Collections.emptyList(),
                        "error", "null",
                        "message", "ok"
                ));
            } else {
                // 直接返回 JSON
                return ResponseEntity.ok(Map.of(
                        "code", 0,
                        "data", Collections.emptyList(),
                        "error", "null",
                        "message", "ok"
                ));
            }
        } catch (Exception e) {
            // 异常时仍保持响应格式一致
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "data", Collections.emptyList(),
                    "error", "null",
                    "message", "ok"
            ));
        }
    }

    @GetMapping("/user/info")
    public ResponseEntity<?> getUserInfo(@RequestHeader("Authorization") String authHeader) {
        try {
            // 1. 从请求头提取 Token 并解析手机号
            String token = authHeader.replace("Bearer ", "").trim();
            String userPhone = jwtUtil.extractUsername(token);
            if (userPhone == null || userPhone.isEmpty()) {
                return ResponseEntity.status(401).body(Map.of(
                        "code", 401,
                        "message", "Unauthorized",
                        "error", "Token无效或已过期",
                        "data", "null"
                ));
            }

            // 2. 查询用户信息
            User user = userMapper.findByPhone(userPhone);
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of(
                        "code", 404,
                        "message", "Not Found",
                        "error", "用户不存在",
                        "data", "null"
                ));
            }

            // 3. 构建用户信息响应体
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("realName", user.getRealName());
            userData.put("userId", user.getUserId());
            userData.put("roles", user.getRoles());  // 假设 roles 是 List<String>
            userData.put("username", user.getUsername());  // 确保 User 实体包含 username 字段

            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "ok",
                    "data", userData,
                    "error", "null"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of(
                    "code", 401,
                    "message", "Internal Server Error",
                    "error", e.getMessage(),
                    "data", "null"
            ));
        }
    }


    @GetMapping("/auth/codes")
    public ResponseEntity<?> getPermissions(@RequestHeader("Authorization") String authHeader) {
        try {
            // 1. 从请求头提取 Token 并解析手机号
            String token = authHeader.replace("Bearer ", "").trim();
            String userPhone = jwtUtil.extractUsername(token);
            if (userPhone == null || userPhone.isEmpty()) {
                return ResponseEntity.status(401).body(Map.of(
                        "code", 401,
                        "message", "Unauthorized",
                        "error", "Token无效或已过期",
                        "data", "null"
                ));
            }

            // 2. 查询用户权限
            User user = userMapper.findByPhone(userPhone);
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of(
                        "code", 404,
                        "message", "Not Found",
                        "error", "用户不存在",
                        "data", "null"
                ));
            }

            // 3. 构建用户信息响应体

            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "ok",
                    "data", user.getPermission(),
                    "error", "null"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "code", 500,
                    "message", "Internal Server Error",
                    "error", e.getMessage(),
                    "data", "null"
            ));
        }
    }

    @PostMapping("/sms-code")
    public ResponseEntity<?> sendCode(@RequestBody LoginRequest request) {
        String userPhone = request.getUserPhone();
        if (!DEMO_PHONES.contains(userPhone)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "code", 400,
                    "message", "展示版仅支持已配置账号"
            ));
        }

        User user = userMapper.findByPhone(userPhone);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of(
                    "code", 404,
                    "message", "User not found"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "展示验证码已就绪",
                "data", Map.of("demo", true)
        ));
    }


    // 内部类保持不变
    static class LoginRequest {
        private String userPhone;
        private String code;

        public String getUserPhone() { return userPhone; }
        public void setUserPhone(String userPhone) { this.userPhone = userPhone; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
    }
}
