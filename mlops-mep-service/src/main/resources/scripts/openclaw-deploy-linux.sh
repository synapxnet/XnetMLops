#!/bin/bash
set -e

# ============================================================
# OpenClaw 远程部署脚本（从源码构建）
# 由 MEP 服务自动生成，变量通过 Java 后端替换
# ============================================================

GATEWAY_PORT="${GATEWAY_PORT}"
CONFIG_PATH="${CONFIG_PATH}"
LOGS_PATH="${LOGS_PATH}"
SOURCE_PATH="${SOURCE_PATH}"
API_KEY_ENV="${API_KEY_ENV}"
NODE_MAJOR=22
TOTAL_STEPS=9
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
    log_error "部署失败，错误发生在第 $1 行"
    exit 1
}

trap 'handle_error $LINENO' ERR

# ============================================================
# Step 1: 系统检测
# ============================================================
log_step "系统环境检测"

OS_TYPE=$(uname -s)
if [ "$OS_TYPE" != "Linux" ]; then
    log_error "仅支持 Linux 系统，当前系统: $OS_TYPE"
    exit 1
fi

ARCH=$(uname -m)
case "$ARCH" in
    x86_64)  NODE_ARCH="x64" ;;
    aarch64) NODE_ARCH="arm64" ;;
    armv7l)  NODE_ARCH="armv7l" ;;
    *)       log_error "不支持的CPU架构: $ARCH"; exit 1 ;;
esac

TOTAL_MEM=$(free -m | awk '/^Mem:/{print $2}')
AVAILABLE_DISK=$(df -m "$HOME" | awk 'NR==2{print $4}')
log_info "操作系统: $(cat /etc/os-release 2>/dev/null | grep PRETTY_NAME | cut -d= -f2 | tr -d '\"' || uname -a)"
log_info "CPU架构: $ARCH | 内存: ${TOTAL_MEM}MB | 可用磁盘: ${AVAILABLE_DISK}MB"

if [ "$AVAILABLE_DISK" -lt 2048 ]; then
    log_error "磁盘空间不足 2GB，源码构建需要更多空间"
    exit 1
fi

# 检查端口是否被占用
if ss -tlnp 2>/dev/null | grep -q ":${GATEWAY_PORT} " || netstat -tlnp 2>/dev/null | grep -q ":${GATEWAY_PORT} "; then
    log_warn "端口 ${GATEWAY_PORT} 已被占用，尝试终止已有进程"
    EXISTING_PID=$(ss -tlnp 2>/dev/null | grep ":${GATEWAY_PORT} " | grep -oP 'pid=\K[0-9]+' || true)
    if [ -n "$EXISTING_PID" ]; then
        kill "$EXISTING_PID" 2>/dev/null || true
        sleep 2
        log_info "已终止进程 $EXISTING_PID"
    fi
fi

# ============================================================
# Step 2: 安装基础依赖
# ============================================================
log_step "安装基础依赖"

install_packages() {
    if command -v apt-get &>/dev/null; then
        apt-get update -qq 2>/dev/null
        apt-get install -y -qq curl wget ca-certificates git tar xz-utils 2>/dev/null
    elif command -v dnf &>/dev/null; then
        dnf install -y -q curl wget ca-certificates git tar xz 2>/dev/null
    elif command -v yum &>/dev/null; then
        yum install -y -q curl wget ca-certificates git tar xz 2>/dev/null
    fi
}

NEED_PACKAGES=false
for cmd in curl git; do
    if ! command -v "$cmd" &>/dev/null; then
        NEED_PACKAGES=true
        break
    fi
done

if [ "$NEED_PACKAGES" = true ]; then
    log_info "安装缺失依赖..."
    install_packages
fi

log_info "基础依赖检查完成 (curl: $(curl --version 2>/dev/null | head -1 | cut -d' ' -f1-2), git: $(git --version 2>/dev/null | cut -d' ' -f3))"

# ============================================================
# Step 3: 安装 Node.js >= 22 (官方二进制包方式，兼容所有发行版)
# ============================================================
log_step "检查/安装 Node.js"

NEED_NODE_INSTALL=false
NODE_INSTALL_DIR="/usr/local"

if command -v node &>/dev/null; then
    NODE_VERSION=$(node --version | sed 's/v//' | cut -d. -f1)
    if [ "$NODE_VERSION" -ge "$NODE_MAJOR" ]; then
        log_info "Node.js $(node --version) 已安装，满足要求"
    else
        log_warn "Node.js 版本 $(node --version) 过低，需要 >= ${NODE_MAJOR}"
        NEED_NODE_INSTALL=true
    fi
else
    log_info "Node.js 未安装"
    NEED_NODE_INSTALL=true
fi

if [ "$NEED_NODE_INSTALL" = true ]; then
    log_info "正在通过官方二进制包安装 Node.js ${NODE_MAJOR}..."

    # 获取最新 LTS 版本号
    NODE_FULL_VERSION=$(curl -fsSL "https://nodejs.org/dist/latest-v${NODE_MAJOR}.x/SHASUMS256.txt" 2>/dev/null | head -1 | grep -oP 'node-v\K[0-9.]+' || echo "")

    if [ -z "$NODE_FULL_VERSION" ]; then
        NODE_FULL_VERSION="${NODE_MAJOR}.12.0"
        log_warn "无法获取最新版本号，使用 ${NODE_FULL_VERSION}"
    fi

    NODE_FILENAME="node-v${NODE_FULL_VERSION}-linux-${NODE_ARCH}"
    NODE_URL="https://nodejs.org/dist/v${NODE_FULL_VERSION}/${NODE_FILENAME}.tar.xz"

    log_info "下载 ${NODE_URL} ..."
    cd /tmp
    curl -fsSL -o "${NODE_FILENAME}.tar.xz" "$NODE_URL"

    log_info "解压并安装到 ${NODE_INSTALL_DIR} ..."
    tar -xJf "${NODE_FILENAME}.tar.xz"
    cp -rf "${NODE_FILENAME}/bin/"* "${NODE_INSTALL_DIR}/bin/"
    cp -rf "${NODE_FILENAME}/lib/"* "${NODE_INSTALL_DIR}/lib/"
    cp -rf "${NODE_FILENAME}/include/"* "${NODE_INSTALL_DIR}/include/" 2>/dev/null || true
    cp -rf "${NODE_FILENAME}/share/"* "${NODE_INSTALL_DIR}/share/" 2>/dev/null || true

    # 清理
    rm -rf "/tmp/${NODE_FILENAME}" "/tmp/${NODE_FILENAME}.tar.xz"

    # 确保在 PATH 中
    export PATH="${NODE_INSTALL_DIR}/bin:$PATH"
    hash -r 2>/dev/null || true

    if ! command -v node &>/dev/null; then
        log_error "Node.js 安装失败"
        exit 1
    fi

    log_info "Node.js $(node --version) 安装成功"
fi

# ============================================================
# Step 4: 启用 pnpm
# ============================================================
log_step "检查/安装 pnpm"

export PATH="${NODE_INSTALL_DIR}/bin:$PATH"

if command -v pnpm &>/dev/null; then
    log_info "pnpm $(pnpm --version) 已安装"
else
    log_info "通过 corepack 启用 pnpm..."
    corepack enable 2>/dev/null || true

    if ! command -v pnpm &>/dev/null; then
        log_info "corepack 未生效，通过 npm 安装 pnpm..."
        npm install -g pnpm 2>&1
    fi

    if ! command -v pnpm &>/dev/null; then
        log_error "pnpm 安装失败"
        exit 1
    fi

    log_info "pnpm $(pnpm --version) 安装成功"
fi

# ============================================================
# Step 5: 克隆/更新 OpenClaw 源码
# ============================================================
log_step "获取 OpenClaw 源码"

# GitHub 直连 + 镜像源（国内服务器 GitHub 访问不稳定）
REPO_URLS=(
    "https://github.com/openclaw/openclaw.git"
    "https://ghproxy.net/https://github.com/openclaw/openclaw.git"
    "https://mirror.ghproxy.com/https://github.com/openclaw/openclaw.git"
    "https://gitclone.com/github.com/openclaw/openclaw.git"
)

git_clone_with_retry() {
    local target_dir="$1"
    for url in "${REPO_URLS[@]}"; do
        log_info "尝试克隆: $url"
        if git clone --depth 1 "$url" "$target_dir" 2>&1; then
            log_info "克隆成功: $url"
            return 0
        fi
        log_warn "克隆失败: $url，尝试下一个源..."
        rm -rf "$target_dir"
    done
    return 1
}

git_pull_with_retry() {
    for url in "${REPO_URLS[@]}"; do
        log_info "尝试拉取: $url"
        if git remote set-url origin "$url" && git fetch --depth 1 origin 2>&1; then
            git reset --hard origin/main 2>&1 || git reset --hard origin/master 2>&1
            log_info "更新成功: $url"
            return 0
        fi
        log_warn "拉取失败: $url，尝试下一个源..."
    done
    return 1
}

if [ -d "$SOURCE_PATH/.git" ]; then
    log_info "源码目录已存在，拉取最新代码..."
    cd "$SOURCE_PATH"
    if ! git_pull_with_retry; then
        log_warn "所有源均拉取失败，删除旧目录重新克隆..."
        cd /tmp
        rm -rf "$SOURCE_PATH"
        if ! git_clone_with_retry "$SOURCE_PATH"; then
            log_error "所有 Git 源均不可用，无法获取 OpenClaw 源码"
            exit 1
        fi
    fi
else
    log_info "克隆 OpenClaw 源码到 $SOURCE_PATH ..."
    rm -rf "$SOURCE_PATH"
    if ! git_clone_with_retry "$SOURCE_PATH"; then
        log_error "所有 Git 源均不可用，无法获取 OpenClaw 源码"
        exit 1
    fi
fi

cd "$SOURCE_PATH"
log_info "当前版本: $(git log --oneline -1 2>/dev/null || echo 'unknown')"

# ============================================================
# Step 6: 安装依赖并构建
# ============================================================
log_step "安装依赖并构建项目"

cd "$SOURCE_PATH"

log_info "pnpm install ..."
pnpm install 2>&1

log_info "pnpm ui:build ..."
pnpm ui:build 2>&1

log_info "pnpm build ..."
pnpm build 2>&1

# 验证构建产物
if [ ! -d "$SOURCE_PATH/dist" ]; then
    log_error "构建失败: dist 目录不存在"
    exit 1
fi

log_info "构建完成，dist 目录大小: $(du -sh "$SOURCE_PATH/dist" | cut -f1)"

# ============================================================
# Step 7: 准备配置与日志目录
# ============================================================
log_step "准备配置与日志目录"

mkdir -p "$CONFIG_PATH"
mkdir -p "$LOGS_PATH"
# doctor 要求的子目录
mkdir -p "$CONFIG_PATH/agents/main/sessions"
mkdir -p "$CONFIG_PATH/credentials"

# 修复权限（doctor 建议）
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
# Step 8: 启动 Gateway
# ============================================================
log_step "启动 OpenClaw Gateway"

cd "$SOURCE_PATH"

# 设置环境变量
export OPENCLAW_STATE_DIR="$CONFIG_PATH"
${API_KEY_ENV}

# 自动修复目录结构（不修改配置文件内容）
log_info "检查目录结构..."
pnpm openclaw doctor 2>&1 || true

# 用 config set 确保关键配置项存在（覆盖式写入，不依赖 doctor --fix）
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
    # 使用 node 直接修改 JSON（最可靠方式）
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
# Step 9: 健康检查
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
    echo -e "${GREEN} OpenClaw Gateway 启动成功${NC}"
    echo -e "${GREEN} 地址: http://0.0.0.0:${GATEWAY_PORT}${NC}"
    echo -e "${GREEN} PID:  ${GATEWAY_PID}${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo "OPENCLAW_PID=${GATEWAY_PID}"
else
    log_warn "健康检查超时，Gateway 可能仍在初始化中"
    log_info "进程 PID: $GATEWAY_PID 仍在运行"
    echo "OPENCLAW_PID=${GATEWAY_PID}"
fi
