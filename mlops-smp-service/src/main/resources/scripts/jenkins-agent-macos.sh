#!/bin/bash
#===============================================================================
# Jenkins Agent 自动部署脚本 - macOS
# 版本: 1.0.0
# 用途: 一键部署 Jenkins Agent 到 macOS 系统
#===============================================================================

set -e

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

# 日志函数
log_info() {
    echo "[INFO] $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_error() {
    echo "[ERROR] $(date '+%Y-%m-%d %H:%M:%S') - $1" >&2
}

log_success() {
    echo "[SUCCESS] $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

# 检查并安装 Homebrew
check_homebrew() {
    if ! command -v brew &> /dev/null; then
        log_info "安装 Homebrew..."
        /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

        # 配置 Homebrew 环境变量
        if [[ $(uname -m) == "arm64" ]]; then
            echo 'eval "$(/opt/homebrew/bin/brew shellenv)"' >> ~/.zprofile
            eval "$(/opt/homebrew/bin/brew shellenv)"
        else
            echo 'eval "$(/usr/local/bin/brew shellenv)"' >> ~/.zprofile
            eval "$(/usr/local/bin/brew shellenv)"
        fi
    fi
    log_success "Homebrew 已就绪"
}

# 安装 Java
install_java() {
    log_info "安装 Java $JAVA_VERSION..."

    case $JAVA_VERSION in
        8)
            brew install --cask temurin8 2>/dev/null || brew install openjdk@8
            ;;
        11)
            brew install --cask temurin11 2>/dev/null || brew install openjdk@11
            ;;
        17)
            brew install --cask temurin17 2>/dev/null || brew install openjdk@17
            ;;
        21)
            brew install --cask temurin21 2>/dev/null || brew install openjdk@21
            ;;
        *)
            brew install --cask temurin11 2>/dev/null || brew install openjdk@11
            ;;
    esac

    # 设置 JAVA_HOME
    if [[ -d "/Library/Java/JavaVirtualMachines" ]]; then
        JAVA_HOME=$(/usr/libexec/java_home 2>/dev/null || echo "")
        if [[ -n "$JAVA_HOME" ]]; then
            echo "export JAVA_HOME=$JAVA_HOME" >> ~/.zshrc
            echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.zshrc
            export JAVA_HOME
            export PATH=$JAVA_HOME/bin:$PATH
        fi
    fi

    java -version
    log_success "Java 安装完成"
}

# 安装 Python
install_python() {
    log_info "安装 Python $PYTHON_VERSION..."

    brew install python@${PYTHON_VERSION} 2>/dev/null || brew install python3

    # 创建符号链接
    brew link --overwrite python@${PYTHON_VERSION} 2>/dev/null || true

    python3 --version
    pip3 --version
    log_success "Python 安装完成"
}

# 安装 Git
install_git() {
    if [ "$INSTALL_GIT" = "true" ]; then
        log_info "安装 Git..."
        brew install git
        git --version
        log_success "Git 安装完成"
    fi
}

# 安装 Docker
install_docker() {
    if [ "$INSTALL_DOCKER" = "true" ]; then
        log_info "安装 Docker Desktop..."
        brew install --cask docker

        log_info "请手动启动 Docker Desktop 应用程序"
        open -a Docker 2>/dev/null || true

        log_success "Docker Desktop 安装完成"
    fi
}

# 安装 Maven
install_maven() {
    if [ "$INSTALL_MAVEN" = "true" ]; then
        log_info "安装 Maven..."
        brew install maven

        mvn -version
        log_success "Maven 安装完成"
    fi
}

# 安装 Node.js
install_nodejs() {
    if [ "$INSTALL_NODE" = "true" ]; then
        log_info "安装 Node.js $NODE_VERSION..."
        brew install node@$NODE_VERSION 2>/dev/null || brew install node

        # 链接 node
        brew link --overwrite node@$NODE_VERSION 2>/dev/null || true

        node --version
        npm --version
        log_success "Node.js 安装完成"
    fi
}

# 下载并配置 Jenkins Agent
setup_jenkins_agent() {
    log_info "配置 Jenkins Agent..."

    # 创建工作目录
    mkdir -p $JENKINS_WORK_DIR

    # 下载 Jenkins Agent JAR
    AGENT_JAR_URL="${JENKINS_URL}/jnlpJars/agent.jar"
    log_info "下载 Jenkins Agent JAR: $AGENT_JAR_URL"
    curl -fsSL -o $JENKINS_WORK_DIR/agent.jar $AGENT_JAR_URL

    # 创建启动脚本
    cat > $JENKINS_WORK_DIR/start-agent.sh << 'SCRIPT_EOF'
#!/bin/bash
JENKINS_URL="__JENKINS_URL__"
JENKINS_AGENT_NAME="__JENKINS_AGENT_NAME__"
JENKINS_WORK_DIR="__JENKINS_WORK_DIR__"
JENKINS_SECRET="__JENKINS_SECRET__"

cd $JENKINS_WORK_DIR

# 设置 JAVA_HOME
export JAVA_HOME=$(/usr/libexec/java_home 2>/dev/null || echo "")
export PATH=$JAVA_HOME/bin:$PATH

# 如果有 secret，使用 secret 连接
if [ -n "$JENKINS_SECRET" ]; then
    java -jar agent.jar \
        -jnlpUrl "${JENKINS_URL}/computer/${JENKINS_AGENT_NAME}/jenkins-agent.jnlp" \
        -secret "$JENKINS_SECRET" \
        -workDir "$JENKINS_WORK_DIR"
else
    # 使用 WebSocket 连接（无需 secret）
    java -jar agent.jar \
        -jnlpUrl "${JENKINS_URL}/computer/${JENKINS_AGENT_NAME}/jenkins-agent.jnlp" \
        -workDir "$JENKINS_WORK_DIR"
fi
SCRIPT_EOF

    # 替换脚本中的变量
    sed -i '' "s|__JENKINS_URL__|$JENKINS_URL|g" $JENKINS_WORK_DIR/start-agent.sh
    sed -i '' "s|__JENKINS_AGENT_NAME__|$JENKINS_AGENT_NAME|g" $JENKINS_WORK_DIR/start-agent.sh
    sed -i '' "s|__JENKINS_WORK_DIR__|$JENKINS_WORK_DIR|g" $JENKINS_WORK_DIR/start-agent.sh
    sed -i '' "s|__JENKINS_SECRET__|$JENKINS_SECRET|g" $JENKINS_WORK_DIR/start-agent.sh

    chmod +x $JENKINS_WORK_DIR/start-agent.sh

    # 创建 LaunchAgent plist
    mkdir -p ~/Library/LaunchAgents
    cat > ~/Library/LaunchAgents/com.jenkins.agent.plist << PLIST_EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key>
    <string>com.jenkins.agent</string>
    <key>ProgramArguments</key>
    <array>
        <string>$JENKINS_WORK_DIR/start-agent.sh</string>
    </array>
    <key>RunAtLoad</key>
    <true/>
    <key>KeepAlive</key>
    <true/>
    <key>WorkingDirectory</key>
    <string>$JENKINS_WORK_DIR</string>
    <key>StandardOutPath</key>
    <string>$JENKINS_WORK_DIR/jenkins-agent.log</string>
    <key>StandardErrorPath</key>
    <string>$JENKINS_WORK_DIR/jenkins-agent-error.log</string>
</dict>
</plist>
PLIST_EOF

    log_success "Jenkins Agent 配置完成"
}

# 启动 Jenkins Agent
start_agent() {
    log_info "启动 Jenkins Agent..."

    # 卸载旧的服务（如果存在）
    launchctl unload ~/Library/LaunchAgents/com.jenkins.agent.plist 2>/dev/null || true

    # 加载新服务
    launchctl load ~/Library/LaunchAgents/com.jenkins.agent.plist

    sleep 5

    # 检查是否运行
    if launchctl list | grep -q "com.jenkins.agent"; then
        log_success "Jenkins Agent 已启动"
    else
        log_error "Jenkins Agent 启动失败，尝试手动启动..."
        nohup $JENKINS_WORK_DIR/start-agent.sh > $JENKINS_WORK_DIR/jenkins-agent.log 2>&1 &
    fi
}

# 主函数
main() {
    log_info "=========================================="
    log_info "Jenkins Agent 自动部署开始 (macOS)"
    log_info "=========================================="
    log_info "Jenkins URL: $JENKINS_URL"
    log_info "Agent 名称: $JENKINS_AGENT_NAME"
    log_info "工作目录: $JENKINS_WORK_DIR"
    log_info "Java 版本: $JAVA_VERSION"
    log_info "Python 版本: $PYTHON_VERSION"
    log_info "=========================================="

    # 检查 Homebrew
    check_homebrew

    # 安装基础依赖
    brew install curl wget 2>/dev/null || true

    # 安装 Java
    install_java

    # 安装 Python
    install_python

    # 安装可选组件
    install_git
    install_docker
    install_maven
    install_nodejs

    # 配置 Jenkins Agent
    setup_jenkins_agent

    # 启动 Agent
    start_agent

    log_info "=========================================="
    log_success "Jenkins Agent 安装完成!"
    log_info "工作目录: $JENKINS_WORK_DIR"
    log_info "服务名称: com.jenkins.agent"
    log_info "管理命令:"
    log_info "  启动: launchctl load ~/Library/LaunchAgents/com.jenkins.agent.plist"
    log_info "  停止: launchctl unload ~/Library/LaunchAgents/com.jenkins.agent.plist"
    log_info "  日志: tail -f $JENKINS_WORK_DIR/jenkins-agent.log"
    log_info "=========================================="
}

# 执行主函数
main "$@"
