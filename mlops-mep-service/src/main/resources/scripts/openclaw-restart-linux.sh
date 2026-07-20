#!/bin/bash
set -e

# ============================================================
# OpenClaw 轻量重启脚本（仅重新配置并启动Gateway）
# 跳过环境安装、源码克隆、依赖构建等耗时步骤
# 由 MEP 服务自动生成，变量通过 Java 后端替换
# ============================================================

GATEWAY_PORT="${GATEWAY_PORT}"
CONFIG_PATH="${CONFIG_PATH}"
LOGS_PATH="${LOGS_PATH}"
SOURCE_PATH="${SOURCE_PATH}"
API_KEY_ENV="${API_KEY_ENV}"
TOTAL_STEPS=4
CURRENT_STEP=0

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_step() {
    CURRENT_STEP=$((CURRENT_STEP + 1))
    echo -e "${BLUE}[Step ${CURRENT_STEP}/${TOTAL_STEPS}]${NC} $1"
}

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

handle_error() {
    log_error "重启失败，错误发生在第 $1 行"
    exit 1
}

trap 'handle_error $LINENO' ERR

# ============================================================
# Step 1: 终止已有Gateway进程
# ============================================================
log_step "终止已有Gateway进程"

# 优先使用PID文件终止进程
if [ -f "$LOGS_PATH/gateway.pid" ]; then
    OLD_PID=$(cat "$LOGS_PATH/gateway.pid" 2>/dev/null)
    if [ -n "$OLD_PID" ] && kill -0 "$OLD_PID" 2>/dev/null; then
        log_info "通过PID文件终止进程 $OLD_PID"
        kill "$OLD_PID" 2>/dev/null || true
        sleep 2
        kill -9 "$OLD_PID" 2>/dev/null || true
    fi
    rm -f "$LOGS_PATH/gateway.pid"
fi

# 再检查端口，确保彻底清理
if ss -tlnp 2>/dev/null | grep -q ":${GATEWAY_PORT} " || netstat -tlnp 2>/dev/null | grep -q ":${GATEWAY_PORT} "; then
    EXISTING_PID=$(ss -tlnp 2>/dev/null | grep ":${GATEWAY_PORT} " | grep -oP 'pid=\K[0-9]+' || true)
    if [ -n "$EXISTING_PID" ]; then
        kill "$EXISTING_PID" 2>/dev/null || true
        sleep 2
        kill -9 "$EXISTING_PID" 2>/dev/null || true
        log_info "已终止端口占用进程 $EXISTING_PID"
    fi
else
    log_info "端口 ${GATEWAY_PORT} 未被占用"
fi

# ============================================================
# Step 2: 准备配置与日志目录
# ============================================================
log_step "准备配置与日志目录"

mkdir -p "$CONFIG_PATH"
mkdir -p "$LOGS_PATH"
mkdir -p "$CONFIG_PATH/agents/main/sessions"
mkdir -p "$CONFIG_PATH/credentials"

chmod 700 "$CONFIG_PATH"

# 验证配置文件存在（由后端 generateAndDeployConfig 上传）
if [ ! -f "$CONFIG_PATH/openclaw.json" ]; then
    log_error "配置文件 $CONFIG_PATH/openclaw.json 不存在！请确认后端已正确上传配置"
    exit 1
fi
chmod 600 "$CONFIG_PATH/openclaw.json"

# 创建 devices 目录并写入自动批准配置（避免 device identity required 错误）
mkdir -p "$CONFIG_PATH/devices"
cat > "$CONFIG_PATH/devices/pending.json" << 'DEVEOF'
{
  "silent": true,
  "autoApprove": ["browser", "cli"],
  "logLevel": "warn"
}
DEVEOF

log_info "配置目录: $CONFIG_PATH"
log_info "日志目录: $LOGS_PATH"
log_info "配置文件内容:"
cat "$CONFIG_PATH/openclaw.json" 2>/dev/null | head -30

# ============================================================
# Step 3: 启动 Gateway
# ============================================================
log_step "启动 OpenClaw Gateway"

# 验证源码目录存在（首次部署可能未完成）
if [ ! -d "$SOURCE_PATH" ]; then
    log_error "源码目录 $SOURCE_PATH 不存在！请先执行完整部署（而非重启）"
    exit 1
fi

cd "$SOURCE_PATH"

# 确保 PATH 包含 Node.js
export PATH="/usr/local/bin:$PATH"

# 设置环境变量
export OPENCLAW_STATE_DIR="$CONFIG_PATH"
${API_KEY_ENV}

# 自动修复目录结构（不修改配置文件内容）
log_info "检查目录结构..."
pnpm openclaw doctor 2>&1 || true

# 用 config set 确保关键配置项存在
log_info "设置关键配置项..."
pnpm openclaw config set gateway.mode local 2>&1
pnpm openclaw config set gateway.http.endpoints.chatCompletions.enabled true 2>&1
pnpm openclaw config set gateway.controlUi.allowInsecureAuth true 2>&1
# 注意：设备自动批准通过 devices/pending.json 文件实现

# 验证配置文件包含 chatCompletions 启用
log_info "最终配置文件内容:"
cat "$CONFIG_PATH/openclaw.json" 2>/dev/null
if ! grep -q "chatCompletions" "$CONFIG_PATH/openclaw.json" 2>/dev/null; then
    log_warn "配置文件中未检测到 chatCompletions 设置，尝试直接写入..."
    node -e "
const fs = require('fs');
const f = '$CONFIG_PATH/openclaw.json';
const c = JSON.parse(fs.readFileSync(f, 'utf8'));
if (!c.gateway) c.gateway = {};
if (!c.gateway.http) c.gateway.http = {};
if (!c.gateway.http.endpoints) c.gateway.http.endpoints = {};
if (!c.gateway.http.endpoints.chatCompletions) c.gateway.http.endpoints.chatCompletions = {};
c.gateway.http.endpoints.chatCompletions.enabled = true;
c.gateway.mode = 'local';
if (!c.gateway.controlUi) c.gateway.controlUi = {};
c.gateway.controlUi.allowInsecureAuth = true;
fs.writeFileSync(f, JSON.stringify(c, null, 2));
console.log('Config patched successfully');
" 2>&1
fi

# 从源码启动 Gateway（后台运行）
nohup pnpm openclaw gateway --port "$GATEWAY_PORT" > "$LOGS_PATH/gateway.log" 2>&1 &
GATEWAY_PID=$!

# 存储PID文件，便于后续管理
echo "$GATEWAY_PID" > "$LOGS_PATH/gateway.pid"
log_info "PID 文件已写入: $LOGS_PATH/gateway.pid"

sleep 3

# 检查进程是否仍在运行
if kill -0 "$GATEWAY_PID" 2>/dev/null; then
    log_info "Gateway 进程已启动, PID: $GATEWAY_PID"
else
    log_error "Gateway 进程启动后立即退出"
    cat "$LOGS_PATH/gateway.log" 2>/dev/null | tail -20
    exit 1
fi

# ============================================================
# Step 4: 健康检查
# ============================================================
log_step "健康检查"

MAX_WAIT=60
WAITED=0
HEALTHY=false

while [ $WAITED -lt $MAX_WAIT ]; do
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:${GATEWAY_PORT}/health" 2>/dev/null || echo "000")
    if echo "$HTTP_CODE" | grep -qE "^(200|403)$"; then
        HEALTHY=true
        break
    fi
    sleep 3
    WAITED=$((WAITED + 3))
    log_info "等待 Gateway 就绪... (${WAITED}s/${MAX_WAIT}s)"
done

if [ "$HEALTHY" = true ]; then
    # 额外验证 HTTP API 端点是否可用
    API_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "http://localhost:${GATEWAY_PORT}/v1/chat/completions" \
        -H "Content-Type: application/json" -d '{}' 2>/dev/null || echo "000")
    if echo "$API_CODE" | grep -qE "^(405)$"; then
        log_warn "HTTP API 端点返回 405，chatCompletions 可能未启用"
    else
        log_info "HTTP API 端点可用 (状态码: $API_CODE)"
    fi

    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN} OpenClaw Gateway 重启成功${NC}"
    echo -e "${GREEN} 地址: http://0.0.0.0:${GATEWAY_PORT}${NC}"
    echo -e "${GREEN} PID:  ${GATEWAY_PID}${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo "OPENCLAW_PID=${GATEWAY_PID}"
else
    log_warn "健康检查超时，Gateway 可能仍在初始化中"
    log_info "进程 PID: $GATEWAY_PID 仍在运行"
    echo "OPENCLAW_PID=${GATEWAY_PID}"
fi
