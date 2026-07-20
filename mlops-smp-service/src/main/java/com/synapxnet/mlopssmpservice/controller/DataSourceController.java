package com.synapxnet.mlopssmpservice.controller;

import com.synapxnet.mlopssmpservice.entity.DataSource;
import com.synapxnet.mlopssmpservice.mapper.DataSourceMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.DatabaseMetaData;
import java.sql.Statement;
import java.util.*;

@RestController
@RequestMapping("/api/smp/datasource")
public class DataSourceController {

    private static final Logger logger = LoggerFactory.getLogger(DataSourceController.class);

    private final DataSourceMapper dataSourceMapper;

    @Autowired
    public DataSourceController(DataSourceMapper dataSourceMapper) {
        this.dataSourceMapper = dataSourceMapper;
    }

    // 获取所有数据源
    @GetMapping
    public ResponseEntity<Map<String, Object>> listAll() {
        try {
            List<DataSource> list = dataSourceMapper.findAll();
            // 隐藏密码
            list.forEach(ds -> ds.setPassword("******"));
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "ok",
                    "data", list,
                    "error", "null"
            ));
        } catch (Exception e) {
            logger.error("获取数据源列表失败", e);
            return ResponseEntity.ok(Map.of(
                    "code", 500,
                    "message", "获取数据源列表失败",
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }

    // 获取启用的数据源列表
    @GetMapping("/enabled")
    public ResponseEntity<Map<String, Object>> listEnabled() {
        try {
            List<DataSource> list = dataSourceMapper.findAllEnabled();
            list.forEach(ds -> ds.setPassword("******"));
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "ok",
                    "data", list,
                    "error", "null"
            ));
        } catch (Exception e) {
            logger.error("获取启用数据源列表失败", e);
            return ResponseEntity.ok(Map.of(
                    "code", 500,
                    "message", "获取启用数据源列表失败",
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }

    // 根据ID获取数据源
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        try {
            Optional<DataSource> ds = dataSourceMapper.findById(id);
            if (ds.isPresent()) {
                DataSource dataSource = ds.get();
                dataSource.setPassword("******");
                return ResponseEntity.ok(Map.of(
                        "code", 0,
                        "message", "ok",
                        "data", dataSource,
                        "error", "null"
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "code", 404,
                        "message", "数据源不存在",
                        "data", "null",
                        "error", "DataSource not found"
                ));
            }
        } catch (Exception e) {
            logger.error("获取数据源失败", e);
            return ResponseEntity.ok(Map.of(
                    "code", 500,
                    "message", "获取数据源失败",
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }

    // 创建数据源
    @PostMapping
    @Transactional
    public ResponseEntity<Map<String, Object>> create(
            @RequestBody DataSource dataSource,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            // 校验用户ID
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "code", 401,
                        "message", "用户未登录或缺少用户信息",
                        "data", "null",
                        "error", "Missing X-User-Id header"
                ));
            }

            // 检查名称是否重复
            if (dataSourceMapper.countByName(dataSource.getName()) > 0) {
                return ResponseEntity.ok(Map.of(
                        "code", 400,
                        "message", "数据源名称已存在",
                        "data", "null",
                        "error", "Duplicate name"
                ));
            }

            // 生成UID
            dataSource.setUid("DS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            if (dataSource.getEnabled() == null) {
                dataSource.setEnabled(true);
            }
            // 设置创建者
            dataSource.setCreatedBy(userId);

            dataSourceMapper.insert(dataSource);

            dataSource.setPassword("******");
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "创建成功",
                    "data", dataSource,
                    "error", "null"
            ));
        } catch (Exception e) {
            logger.error("创建数据源失败", e);
            return ResponseEntity.ok(Map.of(
                    "code", 500,
                    "message", "创建数据源失败: " + e.getMessage(),
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }

    // 更新数据源
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id, @RequestBody DataSource dataSource) {
        try {
            Optional<DataSource> existing = dataSourceMapper.findById(id);
            if (existing.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "code", 404,
                        "message", "数据源不存在",
                        "data", "null",
                        "error", "DataSource not found"
                ));
            }

            // 检查名称是否重复
            if (dataSourceMapper.countByNameExcludeId(dataSource.getName(), id) > 0) {
                return ResponseEntity.ok(Map.of(
                        "code", 400,
                        "message", "数据源名称已存在",
                        "data", "null",
                        "error", "Duplicate name"
                ));
            }

            dataSource.setId(id);
            // 如果密码是******，使用原密码
            if ("******".equals(dataSource.getPassword())) {
                dataSource.setPassword(existing.get().getPassword());
            }

            dataSourceMapper.update(dataSource);

            dataSource.setPassword("******");
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "更新成功",
                    "data", dataSource,
                    "error", "null"
            ));
        } catch (Exception e) {
            logger.error("更新数据源失败", e);
            return ResponseEntity.ok(Map.of(
                    "code", 500,
                    "message", "更新数据源失败: " + e.getMessage(),
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }

    // 删除数据源
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        try {
            if (dataSourceMapper.findById(id).isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "code", 404,
                        "message", "数据源不存在",
                        "data", "null",
                        "error", "DataSource not found"
                ));
            }

            dataSourceMapper.deleteById(id);
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "删除成功",
                    "data", "null",
                    "error", "null"
            ));
        } catch (Exception e) {
            logger.error("删除数据源失败", e);
            return ResponseEntity.ok(Map.of(
                    "code", 500,
                    "message", "删除数据源失败: " + e.getMessage(),
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }

    // 测试数据源连接
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testConnection(@RequestBody DataSource dataSource) {
        Connection conn = null;
        try {
            String url = buildJdbcUrl(dataSource);
            String driver = getDriverClass(dataSource.getType());

            Class.forName(driver);
            conn = DriverManager.getConnection(url, dataSource.getUsername(), dataSource.getPassword());

            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "连接成功",
                    "data", Map.of("success", true),
                    "error", "null"
            ));
        } catch (Exception e) {
            logger.error("测试数据源连接失败", e);
            return ResponseEntity.ok(Map.of(
                    "code", 500,
                    "message", "连接失败: " + e.getMessage(),
                    "data", Map.of("success", false),
                    "error", e.getMessage()
            ));
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (Exception ignored) {}
            }
        }
    }

    // 获取数据库列表（仅返回配置的允许数据库列表，未配置则返回空列表）
    @GetMapping("/{id}/databases")
    public ResponseEntity<Map<String, Object>> getDatabases(@PathVariable Long id) {
        try {
            Optional<DataSource> dsOpt = dataSourceMapper.findById(id);
            if (dsOpt.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "code", 404,
                        "message", "数据源不存在",
                        "data", "null",
                        "error", "DataSource not found"
                ));
            }

            DataSource ds = dsOpt.get();
            List<String> databases = new ArrayList<>();

            // 仅返回配置的允许数据库列表，未配置则返回空列表
            if (ds.getAllowedDatabases() != null && !ds.getAllowedDatabases().trim().isEmpty()) {
                String[] dbArray = ds.getAllowedDatabases().split(",");
                for (String db : dbArray) {
                    String trimmedDb = db.trim();
                    if (!trimmedDb.isEmpty()) {
                        databases.add(trimmedDb);
                    }
                }
            }
            // 未配置allowed_databases则返回空列表，不允许访问任何数据库

            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "ok",
                    "data", databases,
                    "error", "null"
            ));
        } catch (Exception e) {
            logger.error("获取数据库列表失败", e);
            return ResponseEntity.ok(Map.of(
                    "code", 500,
                    "message", "获取数据库列表失败: " + e.getMessage(),
                    "data", "null",
                    "error", e.getMessage()
            ));
        }
    }

    // 获取所有数据库列表（从数据源实际获取，用于配置时选择）
    @GetMapping("/{id}/all-databases")
    public ResponseEntity<Map<String, Object>> getAllDatabases(@PathVariable Long id) {
        Connection conn = null;
        try {
            Optional<DataSource> dsOpt = dataSourceMapper.findById(id);
            if (dsOpt.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "code", 404,
                        "message", "数据源不存在",
                        "data", "null",
                        "error", "DataSource not found"
                ));
            }

            DataSource ds = dsOpt.get();
            String url = buildJdbcUrl(ds);
            Class.forName(getDriverClass(ds.getType()));
            conn = DriverManager.getConnection(url, ds.getUsername(), ds.getPassword());

            List<String> databases = new ArrayList<>();
            ResultSet rs = conn.getMetaData().getCatalogs();
            while (rs.next()) {
                databases.add(rs.getString("TABLE_CAT"));
            }
            rs.close();

            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "ok",
                    "data", databases,
                    "error", "null"
            ));
        } catch (Exception e) {
            logger.error("获取所有数据库列表失败", e);
            return ResponseEntity.ok(Map.of(
                    "code", 500,
                    "message", "获取数据库列表失败: " + e.getMessage(),
                    "data", "null",
                    "error", e.getMessage()
            ));
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (Exception ignored) {}
            }
        }
    }

    // 获取数据表列表
    @GetMapping("/{id}/tables")
    public ResponseEntity<Map<String, Object>> getTables(
            @PathVariable Long id,
            @RequestParam String database) {
        Connection conn = null;
        try {
            Optional<DataSource> dsOpt = dataSourceMapper.findById(id);
            if (dsOpt.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "code", 404,
                        "message", "数据源不存在",
                        "data", "null",
                        "error", "DataSource not found"
                ));
            }

            DataSource ds = dsOpt.get();
            ds.setDefaultDatabase(database);
            String url = buildJdbcUrl(ds);
            Class.forName(getDriverClass(ds.getType()));
            conn = DriverManager.getConnection(url, ds.getUsername(), ds.getPassword());

            List<Map<String, String>> tables = new ArrayList<>();
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getTables(database, null, "%", new String[]{"TABLE", "VIEW"});
            while (rs.next()) {
                Map<String, String> table = new HashMap<>();
                table.put("name", rs.getString("TABLE_NAME"));
                table.put("type", rs.getString("TABLE_TYPE"));
                table.put("remarks", rs.getString("REMARKS"));
                tables.add(table);
            }
            rs.close();

            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "ok",
                    "data", tables,
                    "error", "null"
            ));
        } catch (Exception e) {
            logger.error("获取数据表列表失败", e);
            return ResponseEntity.ok(Map.of(
                    "code", 500,
                    "message", "获取数据表列表失败: " + e.getMessage(),
                    "data", "null",
                    "error", e.getMessage()
            ));
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (Exception ignored) {}
            }
        }
    }

    // 获取表字段列表
    @GetMapping("/{id}/columns")
    public ResponseEntity<Map<String, Object>> getColumns(
            @PathVariable Long id,
            @RequestParam String database,
            @RequestParam String table) {
        Connection conn = null;
        try {
            Optional<DataSource> dsOpt = dataSourceMapper.findById(id);
            if (dsOpt.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "code", 404,
                        "message", "数据源不存在",
                        "data", "null",
                        "error", "DataSource not found"
                ));
            }

            DataSource ds = dsOpt.get();
            ds.setDefaultDatabase(database);
            String url = buildJdbcUrl(ds);
            Class.forName(getDriverClass(ds.getType()));
            conn = DriverManager.getConnection(url, ds.getUsername(), ds.getPassword());

            List<Map<String, Object>> columns = new ArrayList<>();
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getColumns(database, null, table, "%");
            while (rs.next()) {
                Map<String, Object> column = new HashMap<>();
                column.put("name", rs.getString("COLUMN_NAME"));
                column.put("type", rs.getString("TYPE_NAME"));
                column.put("size", rs.getInt("COLUMN_SIZE"));
                column.put("nullable", rs.getInt("NULLABLE") == 1);
                column.put("remarks", rs.getString("REMARKS"));
                column.put("defaultValue", rs.getString("COLUMN_DEF"));
                columns.add(column);
            }
            rs.close();

            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "ok",
                    "data", columns,
                    "error", "null"
            ));
        } catch (Exception e) {
            logger.error("获取表字段列表失败", e);
            return ResponseEntity.ok(Map.of(
                    "code", 500,
                    "message", "获取表字段列表失败: " + e.getMessage(),
                    "data", "null",
                    "error", e.getMessage()
            ));
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (Exception ignored) {}
            }
        }
    }

    // 预览表数据
    @GetMapping("/{id}/preview")
    public ResponseEntity<Map<String, Object>> previewData(
            @PathVariable Long id,
            @RequestParam String database,
            @RequestParam String table,
            @RequestParam(defaultValue = "100") int limit) {
        Connection conn = null;
        Statement stmt = null;
        try {
            Optional<DataSource> dsOpt = dataSourceMapper.findById(id);
            if (dsOpt.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "code", 404,
                        "message", "数据源不存在",
                        "data", "null",
                        "error", "DataSource not found"
                ));
            }

            DataSource ds = dsOpt.get();
            ds.setDefaultDatabase(database);
            String url = buildJdbcUrl(ds);
            Class.forName(getDriverClass(ds.getType()));
            conn = DriverManager.getConnection(url, ds.getUsername(), ds.getPassword());

            stmt = conn.createStatement();
            String sql = String.format("SELECT * FROM %s LIMIT %d", table, Math.min(limit, 1000));
            ResultSet rs = stmt.executeQuery(sql);

            // 获取列信息
            int columnCount = rs.getMetaData().getColumnCount();
            List<String> columnNames = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                columnNames.add(rs.getMetaData().getColumnName(i));
            }

            // 获取数据
            List<Map<String, Object>> rows = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(columnNames.get(i - 1), rs.getObject(i));
                }
                rows.add(row);
            }
            rs.close();

            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "ok",
                    "data", Map.of(
                            "columns", columnNames,
                            "rows", rows,
                            "total", rows.size()
                    ),
                    "error", "null"
            ));
        } catch (Exception e) {
            logger.error("预览表数据失败", e);
            return ResponseEntity.ok(Map.of(
                    "code", 500,
                    "message", "预览表数据失败: " + e.getMessage(),
                    "data", "null",
                    "error", e.getMessage()
            ));
        } finally {
            if (stmt != null) { try { stmt.close(); } catch (Exception ignored) {} }
            if (conn != null) { try { conn.close(); } catch (Exception ignored) {} }
        }
    }

    // 构建JDBC URL
    private String buildJdbcUrl(DataSource ds) {
        String type = ds.getType().toLowerCase();
        String host = ds.getHost();
        int port = ds.getPort();
        String database = ds.getDefaultDatabase() != null ? ds.getDefaultDatabase() : "";

        switch (type) {
            case "mysql":
                return String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
                        host, port, database);
            case "postgresql":
                return String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
            case "oracle":
                return String.format("jdbc:oracle:thin:@%s:%d:%s", host, port, database);
            case "sqlserver":
                return String.format("jdbc:sqlserver://%s:%d;databaseName=%s", host, port, database);
            case "clickhouse":
                return String.format("jdbc:clickhouse://%s:%d/%s", host, port, database);
            case "hive":
                return String.format("jdbc:hive2://%s:%d/%s", host, port, database);
            default:
                throw new IllegalArgumentException("不支持的数据库类型: " + type);
        }
    }

    // 获取驱动类
    private String getDriverClass(String type) {
        switch (type.toLowerCase()) {
            case "mysql":
                return "com.mysql.cj.jdbc.Driver";
            case "postgresql":
                return "org.postgresql.Driver";
            case "oracle":
                return "oracle.jdbc.OracleDriver";
            case "sqlserver":
                return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
            case "clickhouse":
                return "com.clickhouse.jdbc.ClickHouseDriver";
            case "hive":
                return "org.apache.hive.jdbc.HiveDriver";
            default:
                throw new IllegalArgumentException("不支持的数据库类型: " + type);
        }
    }
}
