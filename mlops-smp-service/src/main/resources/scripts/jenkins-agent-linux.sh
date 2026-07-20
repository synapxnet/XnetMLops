#!/bin/bash
#===============================================================================
# Jenkins Agent 自动部署脚本 - Linux (生产环境版)
# 版本: 2.0.0
# 支持: Ubuntu/Debian/CentOS/RHEL/Rocky/AlmaLinux/OpenCloudOS/Fedora
# 特性: 多源容错、回滚机制、进度显示、安装验证、资源检查
#===============================================================================

# 严格模式
set -o pipefail

# 配置参数（由Java后端替换）
JENKINS_URL="${JENKINS_URL}"
JENKINS_AGENT_NAME="${JENKINS_AGENT_NAME}"
JENKINS_WORK_DIR="${JENKINS_WORK_DIR}"
JAVA_VERSION="${JAVA_VERSION}"
PYTHON_VERSION="${PYTHON_VERSION}"
AGENT_VERSION="${AGENT_VERSION}"
JENKINS_SECRET="${JENKINS_SECRET}"
LABELS="${LABELS}"

# 额外配置
INSTALL_DOCKER="${INSTALL_DOCKER}"
INSTALL_GIT="${INSTALL_GIT}"
INSTALL_MAVEN="${INSTALL_MAVEN}"
MAVEN_VERSION="${MAVEN_VERSION}"
INSTALL_NODE="${INSTALL_NODE}"
NODE_VERSION="${NODE_VERSION}"

# 镜像源配置
USE_DOMESTIC_MIRROR="${USE_DOMESTIC_MIRROR}"

#===============================================================================
# 全局变量
#===============================================================================
OS=""
VERSION=""
ARCH=""
ARCH_NAME=""
INSTALLED_PACKAGES=()
CREATED_DIRS=()
CREATED_FILES=()
CREATED_SERVICES=()
ROLLBACK_ENABLED=true
SCRIPT_START_TIME=$(date +%s)

# 资源要求（可配置）
MIN_DISK_SPACE_MB=2048      # 最小磁盘空间 2GB
MIN_MEMORY_MB=512           # 最小内存 512MB
RECOMMENDED_MEMORY_MB=1024  # 推荐内存 1GB

# 进度跟踪
TOTAL_STEPS=10
CURRENT_STEP=0

#===============================================================================
# 颜色和格式定义
#===============================================================================
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color
BOLD='\033[1m'

#===============================================================================
# 日志函数
#===============================================================================
log_info() {
    echo -e "${BLUE}[INFO]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1" >&2
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_step() {
    CURRENT_STEP=$((CURRENT_STEP + 1))
    local percentage=$((CURRENT_STEP * 100 / TOTAL_STEPS))
    echo ""
    echo -e "${CYAN}${BOLD}════════════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}${BOLD}  步骤 ${CURRENT_STEP}/${TOTAL_STEPS} (${percentage}%): $1${NC}"
    echo -e "${CYAN}${BOLD}════════════════════════════════════════════════════════════════${NC}"
}

# 进度条显示
show_progress() {
    local current=$1
    local total=$2
    local width=50
    local percentage=$((current * 100 / total))
    local filled=$((current * width / total))
    local empty=$((width - filled))

    printf "\r  进度: ["
    printf "%${filled}s" | tr ' ' '='
    printf "%${empty}s" | tr ' ' ' '
    printf "] %3d%%" $percentage
}

# 带旋转动画的等待
spinner() {
    local pid=$1
    local message=$2
    local spin='-\|/'
    local i=0

    while kill -0 $pid 2>/dev/null; do
        i=$(( (i+1) % 4 ))
        printf "\r  ${message} ${spin:$i:1}"
        sleep 0.2
    done
    printf "\r  ${message} done\n"
}

#===============================================================================
# 系统检查函数
#===============================================================================

# 检测操作系统类型
detect_os() {
    log_info "检测操作系统..."

    if [ -f /etc/os-release ]; then
        . /etc/os-release
        OS=$ID
        VERSION=$VERSION_ID
    elif [ -f /etc/redhat-release ]; then
        OS="centos"
        VERSION=$(cat /etc/redhat-release | grep -oE '[0-9]+' | head -1)
    elif [ -f /etc/debian_version ]; then
        OS="debian"
        VERSION=$(cat /etc/debian_version)
    else
        OS=$(uname -s | tr '[:upper:]' '[:lower:]')
    fi

    # 检测架构
    ARCH=$(uname -m)
    case $ARCH in
        x86_64)
            ARCH_NAME="x64"
            ;;
        aarch64|arm64)
            ARCH_NAME="aarch64"
            ;;
        armv7l)
            ARCH_NAME="arm"
            ;;
        *)
            log_error "不支持的架构: $ARCH"
            exit 1
            ;;
    esac

    log_success "操作系统: $OS $VERSION ($ARCH)"

    # 验证支持的操作系统
    case $OS in
        ubuntu|debian|centos|rhel|fedora|rocky|almalinux|opencloudos)
            log_info "操作系统受支持"
            ;;
        *)
            log_error "不支持的操作系统: $OS"
            log_error "支持的系统: Ubuntu, Debian, CentOS, RHEL, Fedora, Rocky, AlmaLinux, OpenCloudOS"
            exit 1
            ;;
    esac
}

# 检查磁盘空间
check_disk_space() {
    log_info "检查磁盘空间..."

    local target_dir="${JENKINS_WORK_DIR:-/opt/jenkins-agent}"
    local parent_dir=$(dirname "$target_dir")

    # 确保父目录存在
    if [ ! -d "$parent_dir" ]; then
        parent_dir="/"
    fi

    # 获取可用空间 (MB)
    local available_mb=$(df -m "$parent_dir" 2>/dev/null | awk 'NR==2 {print $4}')

    if [ -z "$available_mb" ]; then
        log_warn "无法检测磁盘空间，继续安装..."
        return 0
    fi

    log_info "可用磁盘空间: ${available_mb}MB (需要: ${MIN_DISK_SPACE_MB}MB)"

    if [ "$available_mb" -lt "$MIN_DISK_SPACE_MB" ]; then
        log_error "磁盘空间不足！可用: ${available_mb}MB, 需要: ${MIN_DISK_SPACE_MB}MB"
        log_error "请清理磁盘空间后重试"
        exit 1
    fi

    log_success "磁盘空间检查通过"
}

# 检查内存
check_memory() {
    log_info "检查系统内存..."

    # 获取可用内存 (MB)
    local total_mb=$(free -m 2>/dev/null | awk '/^Mem:/ {print $2}')
    local available_mb=$(free -m 2>/dev/null | awk '/^Mem:/ {print $7}')

    if [ -z "$total_mb" ]; then
        log_warn "无法检测内存信息，继续安装..."
        return 0
    fi

    log_info "总内存: ${total_mb}MB, 可用: ${available_mb}MB (推荐: ${RECOMMENDED_MEMORY_MB}MB)"

    if [ "$total_mb" -lt "$MIN_MEMORY_MB" ]; then
        log_error "内存不足！总内存: ${total_mb}MB, 最低要求: ${MIN_MEMORY_MB}MB"
        exit 1
    fi

    if [ "$total_mb" -lt "$RECOMMENDED_MEMORY_MB" ]; then
        log_warn "内存低于推荐值 (${RECOMMENDED_MEMORY_MB}MB)，Agent 可能运行缓慢"
    fi

    log_success "内存检查通过"
}

# 检查是否已有 Agent 运行
check_existing_agent() {
    log_info "检查是否已有 Jenkins Agent 运行..."

    # 检查 systemd 服务
    if systemctl is-active --quiet jenkins-agent 2>/dev/null; then
        log_warn "检测到已有 Jenkins Agent 服务正在运行"
        log_info "停止现有服务..."
        sudo systemctl stop jenkins-agent 2>/dev/null || true
        sleep 2
    fi

    # 检查是否有 agent.jar 进程
    local agent_pids=$(pgrep -f "agent.jar" 2>/dev/null || true)
    if [ -n "$agent_pids" ]; then
        log_warn "检测到运行中的 agent.jar 进程: $agent_pids"
        log_info "终止现有进程..."
        for pid in $agent_pids; do
            sudo kill -15 $pid 2>/dev/null || true
        done
        sleep 2

        # 强制终止
        agent_pids=$(pgrep -f "agent.jar" 2>/dev/null || true)
        if [ -n "$agent_pids" ]; then
            for pid in $agent_pids; do
                sudo kill -9 $pid 2>/dev/null || true
            done
        fi
    fi

    log_success "无冲突的 Agent 进程"
}

# 检查网络连接
check_network() {
    log_info "检查网络连接..."

    local test_hosts=("mirrors.aliyun.com" "mirrors.tencent.com" "mirrors.huaweicloud.com" "download.docker.com")
    local connected=false

    for host in "${test_hosts[@]}"; do
        if ping -c 1 -W 3 "$host" &>/dev/null || curl -s --connect-timeout 5 "https://$host" &>/dev/null; then
            log_info "网络连接正常 (可访问 $host)"
            connected=true
            break
        fi
    done

    if [ "$connected" = false ]; then
        log_warn "无法连接到常用镜像源，可能影响安装速度"
    fi

    # 检查 Jenkins URL
    if [ -n "$JENKINS_URL" ]; then
        log_info "检查 Jenkins 服务器连接..."
        if curl -s --connect-timeout 10 "$JENKINS_URL" &>/dev/null; then
            log_success "Jenkins 服务器可达"
        else
            log_warn "无法连接到 Jenkins 服务器: $JENKINS_URL"
            log_warn "请确保 Jenkins 服务器运行正常且网络可达"
        fi
    fi
}

#===============================================================================
# 回滚机制
#===============================================================================

# 记录已安装的包
record_package() {
    INSTALLED_PACKAGES+=("$1")
}

# 记录创建的目录
record_directory() {
    CREATED_DIRS+=("$1")
}

# 记录创建的文件
record_file() {
    CREATED_FILES+=("$1")
}

# 记录创建的服务
record_service() {
    CREATED_SERVICES+=("$1")
}

# 执行回滚
rollback() {
    if [ "$ROLLBACK_ENABLED" != "true" ]; then
        return
    fi

    echo ""
    log_error "============================================"
    log_error "  安装失败，开始回滚..."
    log_error "============================================"

    # 停止并删除服务
    for service in "${CREATED_SERVICES[@]}"; do
        log_info "停止并删除服务: $service"
        sudo systemctl stop "$service" 2>/dev/null || true
        sudo systemctl disable "$service" 2>/dev/null || true
        sudo rm -f "/etc/systemd/system/${service}.service" 2>/dev/null || true
    done
    sudo systemctl daemon-reload 2>/dev/null || true

    # 删除创建的文件
    for file in "${CREATED_FILES[@]}"; do
        if [ -f "$file" ]; then
            log_info "删除文件: $file"
            sudo rm -f "$file" 2>/dev/null || true
        fi
    done

    # 删除创建的目录（仅删除空目录或本脚本创建的目录）
    for dir in "${CREATED_DIRS[@]}"; do
        if [ -d "$dir" ]; then
            log_info "删除目录: $dir"
            sudo rm -rf "$dir" 2>/dev/null || true
        fi
    done

    log_info "回滚完成"
    log_info "============================================"
}

# 设置错误处理
trap 'rollback; exit 1' ERR

#===============================================================================
# 多源下载函数
#===============================================================================

# 带多源容错的下载函数
download_with_fallback() {
    local url=$1
    local output=$2
    local description=$3

    log_info "下载 $description..."

    local max_retries=3
    local retry=0

    while [ $retry -lt $max_retries ]; do
        retry=$((retry + 1))
        log_info "下载尝试 $retry/$max_retries: $url"

        if curl -fSL --connect-timeout 30 --max-time 600 --progress-bar -o "$output" "$url"; then
            if [ -f "$output" ] && [ -s "$output" ]; then
                log_success "下载成功: $description"
                return 0
            fi
        fi

        log_warn "下载失败，等待重试..."
        sleep 3
    done

    log_error "下载失败: $description"
    return 1
}

# 多镜像源下载
download_with_mirrors() {
    local urls=("$@")
    local output="${urls[-1]}"
    local description="${urls[-2]}"
    unset 'urls[-1]'
    unset 'urls[-1]'

    for url in "${urls[@]}"; do
        if download_with_fallback "$url" "$output" "$description"; then
            return 0
        fi
        log_warn "尝试下一个镜像源..."
    done

    log_error "所有镜像源都无法下载: $description"
    return 1
}

#===============================================================================
# 包管理函数
#===============================================================================

# 安装包（带多源容错）
install_package() {
    local package=$1
    log_info "安装 $package..."

    local success=false
    local retry=0
    local max_retries=3

    case $OS in
        ubuntu|debian)
            while [ $retry -lt $max_retries ] && [ "$success" = false ]; do
                retry=$((retry + 1))

                # 尝试更新源（使用多个镜像）
                if ! sudo apt-get update -qq 2>/dev/null; then
                    log_warn "apt-get update 失败，尝试切换镜像源..."
                    try_switch_apt_mirror
                fi

                if sudo apt-get install -y -qq $package 2>/dev/null; then
                    success=true
                    record_package "$package"
                else
                    log_warn "安装 $package 失败，重试 $retry/$max_retries"
                    sleep 2
                fi
            done
            ;;
        centos|rhel|fedora|rocky|almalinux|opencloudos)
            while [ $retry -lt $max_retries ] && [ "$success" = false ]; do
                retry=$((retry + 1))

                if command -v dnf &> /dev/null; then
                    if sudo dnf install -y -q $package 2>/dev/null; then
                        success=true
                        record_package "$package"
                    fi
                else
                    if sudo yum install -y -q $package 2>/dev/null; then
                        success=true
                        record_package "$package"
                    fi
                fi

                if [ "$success" = false ]; then
                    log_warn "安装 $package 失败，重试 $retry/$max_retries"
                    sleep 2
                fi
            done
            ;;
        *)
            log_error "不支持的操作系统: $OS"
            return 1
            ;;
    esac

    if [ "$success" = true ]; then
        log_success "$package 安装成功"
        return 0
    else
        log_error "$package 安装失败"
        return 1
    fi
}

# 尝试切换 APT 镜像源
try_switch_apt_mirror() {
    local mirrors=(
        "mirrors.aliyun.com"
        "mirrors.tencent.com"
        "mirrors.huaweicloud.com"
        "mirrors.163.com"
    )

    for mirror in "${mirrors[@]}"; do
        if curl -s --connect-timeout 5 "https://$mirror" &>/dev/null; then
            log_info "尝试使用镜像: $mirror"
            # 备份原源
            sudo cp /etc/apt/sources.list /etc/apt/sources.list.bak 2>/dev/null || true
            # 这里只是检测，不实际修改源
            return 0
        fi
    done
}

#===============================================================================
# Java 安装
#===============================================================================

install_java() {
    log_info "安装 Java $JAVA_VERSION..."

    # 检查是否已安装
    if command -v java &> /dev/null; then
        local current_version=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
        if [ "$current_version" = "$JAVA_VERSION" ] || [ "$current_version" = "1" -a "$JAVA_VERSION" = "8" ]; then
            log_info "Java $JAVA_VERSION 已安装，跳过"
            java -version
            return 0
        fi
    fi

    case $OS in
        ubuntu|debian)
            sudo apt-get update -qq
            case $JAVA_VERSION in
                8)  sudo apt-get install -y -qq openjdk-8-jdk || install_java_from_adoptium ;;
                11) sudo apt-get install -y -qq openjdk-11-jdk || install_java_from_adoptium ;;
                17) sudo apt-get install -y -qq openjdk-17-jdk || install_java_from_adoptium ;;
                21) sudo apt-get install -y -qq openjdk-21-jdk || install_java_from_adoptium ;;
                *)  sudo apt-get install -y -qq openjdk-11-jdk || install_java_from_adoptium ;;
            esac
            ;;
        centos|rhel|rocky|almalinux|opencloudos)
            # 检查版本，旧版本系统可能没有新版 Java
            if [ "${VERSION%%.*}" -le 7 ] && [ "$JAVA_VERSION" -ge 17 ]; then
                log_info "系统版本较旧，使用 Adoptium Temurin..."
                install_java_from_adoptium
            else
                case $JAVA_VERSION in
                    8)  install_package java-1.8.0-openjdk-devel || install_java_from_adoptium ;;
                    11) install_package java-11-openjdk-devel || install_java_from_adoptium ;;
                    17) install_package java-17-openjdk-devel || install_java_from_adoptium ;;
                    21) install_package java-21-openjdk-devel || install_java_from_adoptium ;;
                    *)  install_package java-11-openjdk-devel || install_java_from_adoptium ;;
                esac
            fi
            ;;
        fedora)
            case $JAVA_VERSION in
                8)  install_package java-1.8.0-openjdk-devel || install_java_from_adoptium ;;
                11) install_package java-11-openjdk-devel || install_java_from_adoptium ;;
                17) install_package java-17-openjdk-devel || install_java_from_adoptium ;;
                21) install_package java-21-openjdk-devel || install_java_from_adoptium ;;
                *)  install_package java-11-openjdk-devel || install_java_from_adoptium ;;
            esac
            ;;
    esac

    # 验证安装
    verify_java_installation
}

# 从 Adoptium 安装 Java（多源容错）
install_java_from_adoptium() {
    log_info "从 Adoptium 下载 Java $JAVA_VERSION..."

    local JAVA_INSTALL_DIR="/opt/java/jdk-${JAVA_VERSION}"
    sudo mkdir -p /opt/java
    record_directory "/opt/java"

    # 根据镜像源配置选择下载源
    local urls=()
    if [ "$USE_DOMESTIC_MIRROR" = "true" ]; then
        log_info "使用国内镜像源下载 Java..."
        urls=(
            "https://mirrors.tuna.tsinghua.edu.cn/Adoptium/${JAVA_VERSION}/jdk/${ARCH_NAME}/linux/OpenJDK${JAVA_VERSION}U-jdk_${ARCH_NAME}_linux_hotspot.tar.gz"
            "https://mirrors.huaweicloud.com/openjdk/${JAVA_VERSION}/openjdk-${JAVA_VERSION}_linux-${ARCH_NAME}_bin.tar.gz"
            "https://api.adoptium.net/v3/binary/latest/${JAVA_VERSION}/ga/linux/${ARCH_NAME}/jdk/hotspot/normal/eclipse"
        )
    else
        log_info "使用官方源下载 Java..."
        urls=(
            "https://api.adoptium.net/v3/binary/latest/${JAVA_VERSION}/ga/linux/${ARCH_NAME}/jdk/hotspot/normal/eclipse"
            "https://download.java.net/java/GA/jdk${JAVA_VERSION}/openjdk-${JAVA_VERSION}_linux-${ARCH_NAME}_bin.tar.gz"
        )
    fi

    local downloaded=false
    for url in "${urls[@]}"; do
        log_info "尝试下载源: $url"
        if curl -fSL --connect-timeout 30 --max-time 600 --progress-bar -o /tmp/openjdk.tar.gz "$url" 2>/dev/null; then
            if [ -f /tmp/openjdk.tar.gz ] && [ -s /tmp/openjdk.tar.gz ]; then
                downloaded=true
                log_success "下载成功"
                break
            fi
        fi
        log_warn "下载失败，尝试下一个源..."
    done

    if [ "$downloaded" = false ]; then
        log_error "所有下载源都失败"
        return 1
    fi

    log_info "解压 Java 安装包..."
    sudo tar -xzf /tmp/openjdk.tar.gz -C /opt/java
    rm -f /tmp/openjdk.tar.gz

    # 找到解压后的目录
    local EXTRACTED_DIR=$(ls -d /opt/java/jdk-${JAVA_VERSION}* 2>/dev/null | head -1)
    if [ -z "$EXTRACTED_DIR" ]; then
        EXTRACTED_DIR=$(ls -d /opt/java/jdk${JAVA_VERSION}* 2>/dev/null | head -1)
    fi

    if [ -z "$EXTRACTED_DIR" ]; then
        log_error "无法找到解压后的 Java 目录"
        return 1
    fi

    # 创建符号链接
    sudo ln -sfn "$EXTRACTED_DIR" /opt/java/current
    record_file "/opt/java/current"

    # 设置环境变量
    cat << 'EOF' | sudo tee /etc/profile.d/java.sh
export JAVA_HOME=/opt/java/current
export PATH=$JAVA_HOME/bin:$PATH
EOF
    record_file "/etc/profile.d/java.sh"

    # 立即加载环境变量
    export JAVA_HOME=/opt/java/current
    export PATH=$JAVA_HOME/bin:$PATH

    # 设置 alternatives
    sudo alternatives --install /usr/bin/java java $JAVA_HOME/bin/java 1 2>/dev/null || true
    sudo alternatives --install /usr/bin/javac javac $JAVA_HOME/bin/javac 1 2>/dev/null || true

    log_success "Adoptium Temurin Java $JAVA_VERSION 安装完成"
}

# 验证 Java 安装
verify_java_installation() {
    log_info "验证 Java 安装..."

    if ! command -v java &> /dev/null; then
        log_error "Java 未安装或不在 PATH 中"
        return 1
    fi

    local version_output=$(java -version 2>&1)
    echo "$version_output"

    # 检查版本是否正确
    local installed_version=$(echo "$version_output" | head -1 | grep -oE '[0-9]+' | head -1)

    if [ "$JAVA_VERSION" = "8" ]; then
        if echo "$version_output" | grep -q "1.8"; then
            log_success "Java 8 验证通过"
            return 0
        fi
    elif [ "$installed_version" = "$JAVA_VERSION" ]; then
        log_success "Java $JAVA_VERSION 验证通过"
        return 0
    fi

    log_warn "安装的 Java 版本可能与要求的不同，但可以继续"
    return 0
}

#===============================================================================
# Python 安装
#===============================================================================

install_python() {
    log_info "安装 Python $PYTHON_VERSION..."

    # 检查是否已安装
    if command -v python3 &> /dev/null; then
        local current_version=$(python3 --version 2>&1 | grep -oE '[0-9]+\.[0-9]+' | head -1)
        log_info "当前 Python 版本: $current_version"
    fi

    case $OS in
        ubuntu|debian)
            sudo apt-get update -qq
            sudo apt-get install -y -qq software-properties-common || true
            sudo add-apt-repository -y ppa:deadsnakes/ppa 2>/dev/null || true
            sudo apt-get update -qq
            sudo apt-get install -y -qq python${PYTHON_VERSION} python${PYTHON_VERSION}-venv python${PYTHON_VERSION}-dev python3-pip 2>/dev/null || \
            sudo apt-get install -y -qq python3 python3-venv python3-dev python3-pip
            sudo update-alternatives --install /usr/bin/python3 python3 /usr/bin/python${PYTHON_VERSION} 1 2>/dev/null || true
            ;;
        centos|rhel|fedora|rocky|almalinux|opencloudos)
            install_package python3 || true
            install_package python3-pip || true
            install_package python3-devel || true
            ;;
    esac

    # 验证
    verify_python_installation
}

# 验证 Python 安装
verify_python_installation() {
    log_info "验证 Python 安装..."

    if command -v python3 &> /dev/null; then
        python3 --version
        log_success "Python 验证通过"
    else
        log_warn "Python 未安装，但不影响 Jenkins Agent 运行"
    fi

    if command -v pip3 &> /dev/null; then
        pip3 --version
    fi
}

#===============================================================================
# Git 安装
#===============================================================================

install_git() {
    if [ "$INSTALL_GIT" = "true" ]; then
        log_info "安装 Git..."

        # 检查是否已安装
        if command -v git &> /dev/null; then
            log_info "Git 已安装"
            git --version
            return 0
        fi

        install_package git

        # 验证
        if command -v git &> /dev/null; then
            git --version
            log_success "Git 安装验证通过"
        else
            log_warn "Git 安装可能失败"
        fi
    fi
}

#===============================================================================
# Docker 安装
#===============================================================================

install_docker() {
    if [ "$INSTALL_DOCKER" = "true" ]; then
        log_info "安装 Docker..."

        # 检查 Docker 是否已安装
        if command -v docker &> /dev/null; then
            log_info "Docker 已安装"
            docker --version
            return 0
        fi

        case $OS in
            ubuntu|debian)
                install_docker_debian
                ;;
            centos|rhel|rocky|almalinux|opencloudos)
                install_docker_rhel
                ;;
            fedora)
                install_docker_fedora
                ;;
        esac

        # 启动服务
        sudo systemctl start docker 2>/dev/null || true
        sudo systemctl enable docker 2>/dev/null || true
        sudo usermod -aG docker $USER 2>/dev/null || true

        # 验证
        verify_docker_installation
    fi
}

install_docker_debian() {
    sudo apt-get update -qq
    sudo apt-get install -y -qq ca-certificates curl gnupg
    sudo install -m 0755 -d /etc/apt/keyrings

    # 根据镜像源配置选择下载源
    local mirrors=()
    if [ "$USE_DOMESTIC_MIRROR" = "true" ]; then
        log_info "使用国内镜像源安装 Docker..."
        mirrors=(
            "mirrors.aliyun.com/docker-ce"
            "mirrors.tencent.com/docker-ce"
            "download.docker.com"
        )
    else
        log_info "使用官方源安装 Docker..."
        mirrors=(
            "download.docker.com"
        )
    fi

    local success=false
    for mirror in "${mirrors[@]}"; do
        log_info "尝试 Docker 镜像源: $mirror"
        if curl -fsSL "https://$mirror/linux/$OS/gpg" | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg 2>/dev/null; then
            sudo chmod a+r /etc/apt/keyrings/docker.gpg
            echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://$mirror/linux/$OS $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
            sudo apt-get update -qq
            if sudo apt-get install -y -qq docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin 2>/dev/null; then
                success=true
                break
            fi
        fi
    done

    if [ "$success" = false ]; then
        log_warn "尝试安装 docker.io..."
        sudo apt-get install -y -qq docker.io || log_error "Docker 安装失败"
    fi
}

install_docker_rhel() {
    sudo yum install -y -q yum-utils 2>/dev/null || true

    # 根据镜像源配置选择下载源
    local mirrors=()
    if [ "$USE_DOMESTIC_MIRROR" = "true" ]; then
        log_info "使用国内镜像源安装 Docker..."
        mirrors=(
            "mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo"
            "mirrors.tencent.com/docker-ce/linux/centos/docker-ce.repo"
            "download.docker.com/linux/centos/docker-ce.repo"
        )
    else
        log_info "使用官方源安装 Docker..."
        mirrors=(
            "download.docker.com/linux/centos/docker-ce.repo"
        )
    fi

    local success=false
    for mirror in "${mirrors[@]}"; do
        log_info "尝试 Docker 镜像源: $mirror"
        if sudo yum-config-manager --add-repo "https://$mirror" 2>/dev/null; then
            if sudo yum install -y -q docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin 2>/dev/null; then
                success=true
                break
            fi
        fi
    done

    if [ "$success" = false ]; then
        log_warn "尝试安装系统自带 docker..."
        sudo yum install -y -q docker || log_error "Docker 安装失败"
    fi
}

install_docker_fedora() {
    sudo dnf install -y -q dnf-plugins-core 2>/dev/null || true

    # 根据镜像源配置选择下载源
    local mirrors=()
    if [ "$USE_DOMESTIC_MIRROR" = "true" ]; then
        log_info "使用国内镜像源安装 Docker..."
        mirrors=(
            "mirrors.aliyun.com/docker-ce/linux/fedora/docker-ce.repo"
            "download.docker.com/linux/fedora/docker-ce.repo"
        )
    else
        log_info "使用官方源安装 Docker..."
        mirrors=(
            "download.docker.com/linux/fedora/docker-ce.repo"
        )
    fi

    for mirror in "${mirrors[@]}"; do
        if sudo dnf config-manager --add-repo "https://$mirror" 2>/dev/null; then
            if sudo dnf install -y -q docker-ce docker-ce-cli containerd.io 2>/dev/null; then
                return 0
            fi
        fi
    done

    sudo dnf install -y -q docker || log_warn "Docker 安装失败"
}

# 验证 Docker 安装
verify_docker_installation() {
    log_info "验证 Docker 安装..."

    if command -v docker &> /dev/null; then
        docker --version

        # 检查服务状态
        if sudo systemctl is-active --quiet docker 2>/dev/null; then
            log_success "Docker 服务运行正常"
        else
            log_warn "Docker 已安装但服务未运行"
        fi
    else
        log_warn "Docker 安装可能失败"
    fi
}

#===============================================================================
# Maven 安装
#===============================================================================

install_maven() {
    if [ "$INSTALL_MAVEN" = "true" ]; then
        log_info "安装 Maven $MAVEN_VERSION..."

        # 检查是否已安装
        if command -v mvn &> /dev/null; then
            log_info "Maven 已安装"
            mvn -version
            return 0
        fi

        local MAVEN_INSTALL_DIR="/opt/maven"
        sudo mkdir -p $MAVEN_INSTALL_DIR
        record_directory "$MAVEN_INSTALL_DIR"

        # 根据镜像源配置选择下载源
        local urls=()
        if [ "$USE_DOMESTIC_MIRROR" = "true" ]; then
            log_info "使用国内镜像源下载 Maven..."
            urls=(
                "https://mirrors.aliyun.com/apache/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz"
                "https://mirrors.tuna.tsinghua.edu.cn/apache/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz"
                "https://mirrors.huaweicloud.com/apache/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz"
                "https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz"
                "https://dlcdn.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz"
            )
        else
            log_info "使用官方源下载 Maven..."
            urls=(
                "https://dlcdn.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz"
                "https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz"
            )
        fi

        local downloaded=false
        for url in "${urls[@]}"; do
            log_info "尝试下载: $url"
            if curl -fSL --connect-timeout 30 --max-time 300 --progress-bar -o /tmp/maven.tar.gz "$url" 2>/dev/null; then
                if [ -f /tmp/maven.tar.gz ] && [ -s /tmp/maven.tar.gz ]; then
                    # 验证是否为有效的 tar.gz 文件
                    if file /tmp/maven.tar.gz | grep -q "gzip compressed"; then
                        if sudo tar -xzf /tmp/maven.tar.gz -C $MAVEN_INSTALL_DIR --strip-components=1 2>/dev/null; then
                            downloaded=true
                            rm -f /tmp/maven.tar.gz
                            break
                        fi
                    fi
                fi
            fi
            log_warn "下载失败或文件无效，尝试下一个源..."
        done
        rm -f /tmp/maven.tar.gz 2>/dev/null

        if [ "$downloaded" = false ]; then
            log_warn "Maven 下载失败，跳过 Maven 安装（不影响 Jenkins Agent 运行）"
            sudo rm -rf $MAVEN_INSTALL_DIR 2>/dev/null || true
            return 0
        fi

        # 设置环境变量
        echo "export M2_HOME=$MAVEN_INSTALL_DIR" | sudo tee /etc/profile.d/maven.sh
        echo 'export PATH=$M2_HOME/bin:$PATH' | sudo tee -a /etc/profile.d/maven.sh
        record_file "/etc/profile.d/maven.sh"

        source /etc/profile.d/maven.sh 2>/dev/null || true
        export M2_HOME=$MAVEN_INSTALL_DIR
        export PATH=$M2_HOME/bin:$PATH

        # 验证
        if command -v mvn &> /dev/null; then
            mvn -version
            log_success "Maven 安装验证通过"
        else
            log_warn "Maven 安装可能失败"
        fi
    fi
}

#===============================================================================
# Node.js 安装
#===============================================================================

install_nodejs() {
    if [ "$INSTALL_NODE" = "true" ]; then
        log_info "安装 Node.js $NODE_VERSION..."

        # 检查是否已安装
        if command -v node &> /dev/null; then
            log_info "Node.js 已安装"
            node --version
            return 0
        fi

        local installed=false

        case $OS in
            ubuntu|debian)
                # 根据镜像源配置选择下载源
                local mirrors=()
                if [ "$USE_DOMESTIC_MIRROR" = "true" ]; then
                    log_info "使用国内镜像源安装 Node.js..."
                    mirrors=(
                        "https://mirrors.tuna.tsinghua.edu.cn/nodesource/deb/setup_${NODE_VERSION}.x"
                        "https://deb.nodesource.com/setup_${NODE_VERSION}.x"
                    )
                else
                    log_info "使用官方源安装 Node.js..."
                    mirrors=(
                        "https://deb.nodesource.com/setup_${NODE_VERSION}.x"
                    )
                fi

                for mirror in "${mirrors[@]}"; do
                    if curl -fsSL "$mirror" | sudo -E bash - 2>/dev/null; then
                        if sudo apt-get install -y -qq nodejs 2>/dev/null; then
                            installed=true
                            break
                        fi
                    fi
                done
                ;;
            centos|rhel|fedora|rocky|almalinux|opencloudos)
                # 根据镜像源配置选择下载源
                local mirrors=()
                if [ "$USE_DOMESTIC_MIRROR" = "true" ]; then
                    log_info "使用国内镜像源安装 Node.js..."
                    mirrors=(
                        "https://mirrors.tuna.tsinghua.edu.cn/nodesource/rpm/setup_${NODE_VERSION}.x"
                        "https://rpm.nodesource.com/setup_${NODE_VERSION}.x"
                    )
                else
                    log_info "使用官方源安装 Node.js..."
                    mirrors=(
                        "https://rpm.nodesource.com/setup_${NODE_VERSION}.x"
                    )
                fi

                for mirror in "${mirrors[@]}"; do
                    if curl -fsSL "$mirror" | sudo bash - 2>/dev/null; then
                        if sudo yum install -y -q nodejs 2>/dev/null || sudo dnf install -y -q nodejs 2>/dev/null; then
                            installed=true
                            break
                        fi
                    fi
                done
                ;;
        esac

        # 如果包管理器安装失败，尝试下载二进制包
        if [ "$installed" = false ]; then
            log_warn "包管理器安装失败，尝试下载 Node.js 二进制包..."
            install_nodejs_binary
        fi

        # 验证
        verify_nodejs_installation
    fi
}

# 从二进制包安装 Node.js
install_nodejs_binary() {
    log_info "下载 Node.js ${NODE_VERSION}.x 二进制包..."

    local NODE_INSTALL_DIR="/opt/nodejs"
    sudo mkdir -p $NODE_INSTALL_DIR
    record_directory "$NODE_INSTALL_DIR"

    # 获取最新的 LTS 版本号
    local NODE_FULL_VERSION=""

    # 根据镜像源配置选择下载源
    local base_urls=()
    if [ "$USE_DOMESTIC_MIRROR" = "true" ]; then
        base_urls=(
            "https://mirrors.tuna.tsinghua.edu.cn/nodejs-release"
            "https://mirrors.aliyun.com/nodejs-release"
            "https://nodejs.org/dist"
        )
    else
        base_urls=(
            "https://nodejs.org/dist"
        )
    fi

    # 尝试获取版本列表并找到匹配的版本
    for base_url in "${base_urls[@]}"; do
        log_info "从 $base_url 获取版本信息..."
        NODE_FULL_VERSION=$(curl -fsSL --connect-timeout 10 "$base_url/index.json" 2>/dev/null | grep -oE "\"version\":\"v${NODE_VERSION}\.[0-9]+\.[0-9]+\"" | head -1 | grep -oE "v${NODE_VERSION}\.[0-9]+\.[0-9]+" || true)
        if [ -n "$NODE_FULL_VERSION" ]; then
            break
        fi
    done

    # 如果获取失败，使用默认版本
    if [ -z "$NODE_FULL_VERSION" ]; then
        case $NODE_VERSION in
            18) NODE_FULL_VERSION="v18.20.5" ;;
            20) NODE_FULL_VERSION="v20.18.1" ;;
            22) NODE_FULL_VERSION="v22.12.0" ;;
            *) NODE_FULL_VERSION="v${NODE_VERSION}.0.0" ;;
        esac
        log_warn "无法获取版本信息，使用默认版本: $NODE_FULL_VERSION"
    fi

    log_info "Node.js 版本: $NODE_FULL_VERSION"

    # 确定架构
    local node_arch="x64"
    case $ARCH in
        aarch64|arm64) node_arch="arm64" ;;
        armv7l) node_arch="armv7l" ;;
    esac

    local downloaded=false
    for base_url in "${base_urls[@]}"; do
        local url="${base_url}/${NODE_FULL_VERSION}/node-${NODE_FULL_VERSION}-linux-${node_arch}.tar.xz"
        log_info "尝试下载: $url"

        if curl -fSL --connect-timeout 30 --max-time 300 --progress-bar -o /tmp/nodejs.tar.xz "$url" 2>/dev/null; then
            if [ -f /tmp/nodejs.tar.xz ] && [ -s /tmp/nodejs.tar.xz ]; then
                if file /tmp/nodejs.tar.xz | grep -q "XZ compressed"; then
                    if sudo tar -xJf /tmp/nodejs.tar.xz -C $NODE_INSTALL_DIR --strip-components=1 2>/dev/null; then
                        downloaded=true
                        rm -f /tmp/nodejs.tar.xz
                        break
                    fi
                fi
            fi
        fi
        log_warn "下载失败，尝试下一个源..."
    done
    rm -f /tmp/nodejs.tar.xz 2>/dev/null

    if [ "$downloaded" = false ]; then
        log_warn "Node.js 二进制包下载失败，跳过 Node.js 安装（不影响 Jenkins Agent 运行）"
        sudo rm -rf $NODE_INSTALL_DIR 2>/dev/null || true
        return 0
    fi

    # 设置环境变量
    cat << 'EOF' | sudo tee /etc/profile.d/nodejs.sh
export NODE_HOME=/opt/nodejs
export PATH=$NODE_HOME/bin:$PATH
EOF
    record_file "/etc/profile.d/nodejs.sh"

    # 立即加载环境变量
    export NODE_HOME=/opt/nodejs
    export PATH=$NODE_HOME/bin:$PATH

    # 创建符号链接
    sudo ln -sf $NODE_INSTALL_DIR/bin/node /usr/local/bin/node 2>/dev/null || true
    sudo ln -sf $NODE_INSTALL_DIR/bin/npm /usr/local/bin/npm 2>/dev/null || true
    sudo ln -sf $NODE_INSTALL_DIR/bin/npx /usr/local/bin/npx 2>/dev/null || true

    log_success "Node.js 二进制包安装完成"
}

# 验证 Node.js 安装
verify_nodejs_installation() {
    log_info "验证 Node.js 安装..."

    if command -v node &> /dev/null; then
        node --version
        log_success "Node.js 验证通过"
    else
        log_warn "Node.js 安装可能失败"
    fi

    if command -v npm &> /dev/null; then
        npm --version
    fi
}

#===============================================================================
# Jenkins Agent 配置
#===============================================================================

setup_jenkins_agent() {
    log_info "配置 Jenkins Agent..."

    # 创建工作目录
    sudo mkdir -p $JENKINS_WORK_DIR
    sudo chown $USER:$USER $JENKINS_WORK_DIR
    record_directory "$JENKINS_WORK_DIR"

    # 下载 Jenkins Agent JAR（带多源容错）
    local AGENT_JAR_URL="${JENKINS_URL}/jnlpJars/agent.jar"
    log_info "下载 Jenkins Agent JAR..."

    local retry=0
    local max_retries=5
    local downloaded=false

    while [ $retry -lt $max_retries ] && [ "$downloaded" = false ]; do
        retry=$((retry + 1))
        log_info "下载尝试 $retry/$max_retries: $AGENT_JAR_URL"

        if curl -fSL --connect-timeout 30 --max-time 300 --progress-bar -o "$JENKINS_WORK_DIR/agent.jar" "$AGENT_JAR_URL" 2>/dev/null; then
            if [ -f "$JENKINS_WORK_DIR/agent.jar" ] && [ -s "$JENKINS_WORK_DIR/agent.jar" ]; then
                downloaded=true
                record_file "$JENKINS_WORK_DIR/agent.jar"
            fi
        fi

        if [ "$downloaded" = false ]; then
            log_warn "下载失败，等待重试..."
            sleep 5
        fi
    done

    if [ "$downloaded" = false ]; then
        log_error "无法下载 agent.jar"
        return 1
    fi

    # 验证 JAR 文件
    if ! file "$JENKINS_WORK_DIR/agent.jar" | grep -q "Java archive"; then
        log_warn "agent.jar 可能不是有效的 JAR 文件"
    fi

    # 创建启动脚本
    create_start_script

    # 创建 systemd 服务
    create_systemd_service

    log_success "Jenkins Agent 配置完成"
}

# 创建启动脚本
create_start_script() {
    log_info "创建启动脚本..."

    cat > $JENKINS_WORK_DIR/start-agent.sh << 'SCRIPT_EOF'
#!/bin/bash
#===============================================================================
# Jenkins Agent 启动脚本
#===============================================================================

JENKINS_URL="__JENKINS_URL__"
JENKINS_AGENT_NAME="__JENKINS_AGENT_NAME__"
JENKINS_WORK_DIR="__JENKINS_WORK_DIR__"
JENKINS_SECRET="__JENKINS_SECRET__"

# 日志函数
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

# 加载环境变量
for profile in /etc/profile ~/.bashrc /etc/profile.d/*.sh; do
    [ -f "$profile" ] && source "$profile" 2>/dev/null || true
done

# 设置 JAVA_HOME（如果未设置）
if [ -z "$JAVA_HOME" ]; then
    for java_dir in /opt/java/current /usr/lib/jvm/java-*-openjdk* /usr/lib/jvm/jdk-*; do
        if [ -d "$java_dir" ] && [ -x "$java_dir/bin/java" ]; then
            export JAVA_HOME="$java_dir"
            export PATH="$JAVA_HOME/bin:$PATH"
            break
        fi
    done
fi

cd "$JENKINS_WORK_DIR" || exit 1

log "=========================================="
log "Jenkins Agent 启动"
log "=========================================="
log "Jenkins URL: $JENKINS_URL"
log "Agent Name: $JENKINS_AGENT_NAME"
log "Work Dir: $JENKINS_WORK_DIR"
log "JAVA_HOME: $JAVA_HOME"
log "Java Version: $(java -version 2>&1 | head -1)"
log "=========================================="

# 检查 agent.jar 是否存在
if [ ! -f "agent.jar" ]; then
    log "agent.jar 不存在，尝试下载..."

    retry=0
    max_retries=3
    while [ $retry -lt $max_retries ]; do
        if curl -fsSL --connect-timeout 30 --max-time 300 -o agent.jar "${JENKINS_URL}/jnlpJars/agent.jar"; then
            log "下载成功"
            break
        fi
        retry=$((retry + 1))
        log "下载失败，重试 $retry/$max_retries"
        sleep 5
    done

    if [ ! -f "agent.jar" ]; then
        log "错误: 无法下载 agent.jar"
        exit 1
    fi
fi

# 检查 Secret 是否配置
if [ -z "$JENKINS_SECRET" ]; then
    log "错误: JENKINS_SECRET 未配置"
    exit 1
fi

# 计算 JVM 内存参数
total_mem_mb=$(free -m 2>/dev/null | awk '/^Mem:/ {print $2}')
if [ -n "$total_mem_mb" ]; then
    # 使用总内存的 50%，但不超过 2GB
    heap_size=$((total_mem_mb / 2))
    [ $heap_size -gt 2048 ] && heap_size=2048
    [ $heap_size -lt 256 ] && heap_size=256
    JVM_OPTS="-Xmx${heap_size}m -Xms${heap_size}m"
else
    JVM_OPTS="-Xmx512m -Xms256m"
fi

log "JVM 参数: $JVM_OPTS"
log "启动 Jenkins Agent (WebSocket 模式)..."

# 使用 WebSocket 模式连接
exec java $JVM_OPTS -jar agent.jar \
    -url "$JENKINS_URL" \
    -secret "$JENKINS_SECRET" \
    -name "$JENKINS_AGENT_NAME" \
    -webSocket \
    -workDir "$JENKINS_WORK_DIR"
SCRIPT_EOF

    # 替换变量
    sed -i "s|__JENKINS_URL__|$JENKINS_URL|g" $JENKINS_WORK_DIR/start-agent.sh
    sed -i "s|__JENKINS_AGENT_NAME__|$JENKINS_AGENT_NAME|g" $JENKINS_WORK_DIR/start-agent.sh
    sed -i "s|__JENKINS_WORK_DIR__|$JENKINS_WORK_DIR|g" $JENKINS_WORK_DIR/start-agent.sh
    sed -i "s|__JENKINS_SECRET__|$JENKINS_SECRET|g" $JENKINS_WORK_DIR/start-agent.sh

    chmod +x $JENKINS_WORK_DIR/start-agent.sh
    record_file "$JENKINS_WORK_DIR/start-agent.sh"
}

# 创建 systemd 服务
create_systemd_service() {
    log_info "创建 systemd 服务..."

    sudo tee /etc/systemd/system/jenkins-agent.service > /dev/null << SERVICE_EOF
[Unit]
Description=Jenkins Agent
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=$USER
WorkingDirectory=$JENKINS_WORK_DIR
ExecStart=$JENKINS_WORK_DIR/start-agent.sh
Restart=always
RestartSec=10
StartLimitIntervalSec=60
StartLimitBurst=3

# 资源限制
MemoryMax=80%
CPUQuota=80%

# 环境变量
Environment="JAVA_HOME=/opt/java/current"
Environment="PATH=/opt/java/current/bin:/usr/local/bin:/usr/bin:/bin"

[Install]
WantedBy=multi-user.target
SERVICE_EOF

    record_file "/etc/systemd/system/jenkins-agent.service"
    record_service "jenkins-agent"

    sudo systemctl daemon-reload
    sudo systemctl enable jenkins-agent
}

#===============================================================================
# 启动和验证
#===============================================================================

start_agent() {
    log_info "启动 Jenkins Agent..."
    sudo systemctl start jenkins-agent

    # 等待启动
    local wait_time=0
    local max_wait=30

    while [ $wait_time -lt $max_wait ]; do
        if sudo systemctl is-active --quiet jenkins-agent; then
            break
        fi
        sleep 1
        wait_time=$((wait_time + 1))
        printf "\r  等待服务启动... %ds" $wait_time
    done
    echo ""

    if sudo systemctl is-active --quiet jenkins-agent; then
        log_success "Jenkins Agent 已启动"
        return 0
    else
        log_error "Jenkins Agent 启动失败"
        log_info "查看日志: sudo journalctl -u jenkins-agent -n 50"
        sudo systemctl status jenkins-agent || true
        return 1
    fi
}

# 验证 Agent 连接
verify_agent_connection() {
    log_info "验证 Agent 连接..."

    # 等待连接建立
    sleep 5

    # 检查进程
    if pgrep -f "agent.jar" > /dev/null; then
        log_success "Agent 进程运行中"
    else
        log_warn "未检测到 Agent 进程"
    fi

    # 检查日志中是否有连接成功的信息
    if sudo journalctl -u jenkins-agent --no-pager -n 20 2>/dev/null | grep -qi "connected"; then
        log_success "Agent 已连接到 Jenkins"
    else
        log_info "请在 Jenkins 控制台确认 Agent 连接状态"
    fi
}

#===============================================================================
# 生成部署报告
#===============================================================================

generate_report() {
    local end_time=$(date +%s)
    local duration=$((end_time - SCRIPT_START_TIME))

    echo ""
    echo -e "${GREEN}${BOLD}╔══════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}${BOLD}║              Jenkins Agent 部署完成                              ║${NC}"
    echo -e "${GREEN}${BOLD}╠══════════════════════════════════════════════════════════════════╣${NC}"
    echo -e "${GREEN}${BOLD}║${NC}  部署耗时: ${duration}秒                                              ${GREEN}${BOLD}║${NC}"
    echo -e "${GREEN}${BOLD}║${NC}  工作目录: $JENKINS_WORK_DIR                                    ${GREEN}${BOLD}║${NC}"
    echo -e "${GREEN}${BOLD}║${NC}  服务名称: jenkins-agent                                        ${GREEN}${BOLD}║${NC}"
    echo -e "${GREEN}${BOLD}╠══════════════════════════════════════════════════════════════════╣${NC}"
    echo -e "${GREEN}${BOLD}║${NC}  ${CYAN}管理命令:${NC}                                                     ${GREEN}${BOLD}║${NC}"
    echo -e "${GREEN}${BOLD}║${NC}    启动: sudo systemctl start jenkins-agent                     ${GREEN}${BOLD}║${NC}"
    echo -e "${GREEN}${BOLD}║${NC}    停止: sudo systemctl stop jenkins-agent                      ${GREEN}${BOLD}║${NC}"
    echo -e "${GREEN}${BOLD}║${NC}    状态: sudo systemctl status jenkins-agent                    ${GREEN}${BOLD}║${NC}"
    echo -e "${GREEN}${BOLD}║${NC}    日志: sudo journalctl -u jenkins-agent -f                    ${GREEN}${BOLD}║${NC}"
    echo -e "${GREEN}${BOLD}╠══════════════════════════════════════════════════════════════════╣${NC}"
    echo -e "${GREEN}${BOLD}║${NC}  ${CYAN}已安装组件:${NC}                                                   ${GREEN}${BOLD}║${NC}"

    # Java
    if command -v java &> /dev/null; then
        echo -e "${GREEN}${BOLD}║${NC}    ✓ Java $(java -version 2>&1 | head -1 | cut -d'"' -f2)                                         ${GREEN}${BOLD}║${NC}"
    fi

    # Python
    if command -v python3 &> /dev/null; then
        echo -e "${GREEN}${BOLD}║${NC}    ✓ Python $(python3 --version 2>&1 | cut -d' ' -f2)                                       ${GREEN}${BOLD}║${NC}"
    fi

    # Git
    if command -v git &> /dev/null; then
        echo -e "${GREEN}${BOLD}║${NC}    ✓ Git $(git --version | cut -d' ' -f3)                                          ${GREEN}${BOLD}║${NC}"
    fi

    # Docker
    if command -v docker &> /dev/null; then
        echo -e "${GREEN}${BOLD}║${NC}    ✓ Docker $(docker --version | cut -d' ' -f3 | tr -d ',')                                    ${GREEN}${BOLD}║${NC}"
    fi

    # Maven
    if command -v mvn &> /dev/null; then
        echo -e "${GREEN}${BOLD}║${NC}    ✓ Maven $(mvn -version 2>/dev/null | head -1 | cut -d' ' -f3)                                      ${GREEN}${BOLD}║${NC}"
    fi

    # Node.js
    if command -v node &> /dev/null; then
        echo -e "${GREEN}${BOLD}║${NC}    ✓ Node.js $(node --version)                                       ${GREEN}${BOLD}║${NC}"
    fi

    echo -e "${GREEN}${BOLD}╚══════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

#===============================================================================
# 主函数
#===============================================================================

main() {
    echo ""
    echo -e "${CYAN}${BOLD}╔══════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}${BOLD}║         Jenkins Agent 自动部署脚本 v2.0.0 (生产环境版)           ║${NC}"
    echo -e "${CYAN}${BOLD}╚══════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "  Jenkins URL:    ${BOLD}$JENKINS_URL${NC}"
    echo -e "  Agent 名称:     ${BOLD}$JENKINS_AGENT_NAME${NC}"
    echo -e "  工作目录:       ${BOLD}$JENKINS_WORK_DIR${NC}"
    echo -e "  Java 版本:      ${BOLD}$JAVA_VERSION${NC}"
    echo -e "  Python 版本:    ${BOLD}$PYTHON_VERSION${NC}"
    if [ "$USE_DOMESTIC_MIRROR" = "true" ]; then
        echo -e "  下载镜像源:     ${BOLD}国内镜像（阿里云/清华/腾讯云）${NC}"
    else
        echo -e "  下载镜像源:     ${BOLD}官方源${NC}"
    fi
    echo ""

    # 步骤 1: 系统检测
    log_step "系统环境检测"
    detect_os
    check_disk_space
    check_memory
    check_existing_agent
    check_network

    # 步骤 2: 基础依赖
    log_step "安装基础依赖"
    install_package curl
    install_package wget
    install_package tar
    install_package gzip

    # 步骤 3: Java
    log_step "安装 Java $JAVA_VERSION"
    install_java

    # 步骤 4: Python
    log_step "安装 Python"
    install_python

    # 步骤 5: Git
    log_step "安装 Git"
    install_git

    # 步骤 6: Docker
    log_step "安装 Docker"
    install_docker

    # 步骤 7: Maven
    log_step "安装 Maven"
    install_maven

    # 步骤 8: Node.js
    log_step "安装 Node.js"
    install_nodejs

    # 步骤 9: 配置 Agent
    log_step "配置 Jenkins Agent"
    setup_jenkins_agent

    # 步骤 10: 启动和验证
    log_step "启动并验证 Agent"
    if start_agent; then
        verify_agent_connection
        ROLLBACK_ENABLED=false  # 成功后禁用回滚
        generate_report
        exit 0
    else
        log_error "部署失败"
        exit 1
    fi
}

# 错误处理
handle_error() {
    log_error "脚本执行出错，行号: $1"
    rollback
    exit 1
}

trap 'handle_error $LINENO' ERR

# 执行主函数
main "$@"
