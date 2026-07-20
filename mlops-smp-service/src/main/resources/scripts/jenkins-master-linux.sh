#!/bin/bash
#===============================================================================
# Jenkins Master 自动部署脚本 - Linux (生产环境版)
# 版本: 2.0.0
# 支持: Ubuntu/Debian/CentOS/RHEL/Rocky/AlmaLinux/OpenCloudOS/Fedora
# 特性: 多源容错、进度显示、自动配置管理员账号、国内镜像、环境变量配置
#===============================================================================

set -o pipefail

# 配置参数（由Java后端替换）
JENKINS_VERSION="${JENKINS_VERSION}"
JENKINS_PORT="${JENKINS_PORT}"
JENKINS_HOME="${JENKINS_HOME}"
JAVA_VERSION="${JAVA_VERSION}"
JAVA_OPTS="${JAVA_OPTS}"
ADMIN_USERNAME="${ADMIN_USERNAME}"
ADMIN_PASSWORD="${ADMIN_PASSWORD}"
ADMIN_EMAIL="${ADMIN_EMAIL}"
INSTALL_SUGGESTED_PLUGINS="${INSTALL_SUGGESTED_PLUGINS}"
# 主机IP地址（由后端传入）
HOST_IP="${HOST_IP}"
# 时区配置
TIMEZONE="${TIMEZONE}"

# 全局变量
OS=""
VERSION=""
ARCH=""
SCRIPT_START_TIME=$(date +%s)
TOTAL_STEPS=12
CURRENT_STEP=0
DETECTED_IP=""

# Jenkins WAR下载镜像源（官方源优先）
JENKINS_WAR_MIRRORS=(
    "https://get.jenkins.io/war-stable/${JENKINS_VERSION}/jenkins.war"
    "https://mirrors.huaweicloud.com/jenkins/war-stable/${JENKINS_VERSION}/jenkins.war"
)

# Jenkins 插件更新站点（使用官方HTTPS源）
JENKINS_UPDATE_CENTER="https://updates.jenkins.io/update-center.json"

#===============================================================================
# 颜色定义
#===============================================================================
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'
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

#===============================================================================
# 系统检测
#===============================================================================
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

    ARCH=$(uname -m)

    log_success "操作系统: $OS $VERSION ($ARCH)"

    case $OS in
        ubuntu|debian|centos|rhel|fedora|rocky|almalinux|opencloudos)
            log_info "操作系统受支持"
            ;;
        *)
            log_error "不支持的操作系统: $OS"
            exit 1
            ;;
    esac
}

detect_ip() {
    log_info "检测主机 IP 地址..."

    # 优先使用后端传入的 IP
    if [ -n "$HOST_IP" ] && [ "$HOST_IP" != "\${HOST_IP}" ]; then
        DETECTED_IP="$HOST_IP"
        log_success "使用配置的 IP: $DETECTED_IP"
        return
    fi

    # 尝试多种方式获取 IP
    DETECTED_IP=$(hostname -I 2>/dev/null | awk '{print $1}')

    if [ -z "$DETECTED_IP" ]; then
        DETECTED_IP=$(ip route get 1 2>/dev/null | awk '{print $7; exit}')
    fi

    if [ -z "$DETECTED_IP" ]; then
        DETECTED_IP=$(ifconfig 2>/dev/null | grep -Eo 'inet (addr:)?([0-9]*\.){3}[0-9]*' | grep -Eo '([0-9]*\.){3}[0-9]*' | grep -v '127.0.0.1' | head -1)
    fi

    if [ -z "$DETECTED_IP" ]; then
        DETECTED_IP="127.0.0.1"
        log_warn "无法检测 IP，使用回环地址"
    else
        log_success "检测到主机 IP: $DETECTED_IP"
    fi
}

check_disk_space() {
    log_info "检查磁盘空间..."

    local available_mb=$(df -m /opt 2>/dev/null | awk 'NR==2 {print $4}')
    local required_mb=5120  # 5GB

    if [ -n "$available_mb" ] && [ "$available_mb" -lt "$required_mb" ]; then
        log_error "磁盘空间不足！可用: ${available_mb}MB, 需要: ${required_mb}MB"
        exit 1
    fi

    log_success "磁盘空间检查通过 (可用: ${available_mb}MB)"
}

check_memory() {
    log_info "检查系统内存..."

    local total_mb=$(free -m 2>/dev/null | awk '/^Mem:/ {print $2}')
    local required_mb=2048  # 2GB

    if [ -n "$total_mb" ]; then
        log_info "总内存: ${total_mb}MB"
        if [ "$total_mb" -lt "$required_mb" ]; then
            log_warn "内存低于推荐值 (${required_mb}MB)，Jenkins可能运行缓慢"
        fi
    fi
}

check_port() {
    log_info "检查端口 ${JENKINS_PORT}..."

    if netstat -tuln 2>/dev/null | grep -q ":${JENKINS_PORT} " || ss -tuln 2>/dev/null | grep -q ":${JENKINS_PORT} "; then
        log_error "端口 ${JENKINS_PORT} 已被占用"
        exit 1
    fi

    log_success "端口 ${JENKINS_PORT} 可用"
}

#===============================================================================
# 包安装
#===============================================================================
install_package() {
    local package=$1
    log_info "安装 $package..."

    case $OS in
        ubuntu|debian)
            sudo apt-get update -qq 2>/dev/null
            sudo apt-get install -y -qq $package 2>/dev/null
            ;;
        centos|rhel|fedora|rocky|almalinux|opencloudos)
            if command -v dnf &> /dev/null; then
                sudo dnf install -y -q $package 2>/dev/null
            else
                sudo yum install -y -q $package 2>/dev/null
            fi
            ;;
    esac
}

#===============================================================================
# Java 安装
#===============================================================================
install_java() {
    log_info "安装 Java $JAVA_VERSION..."

    # 检查是否已安装
    if command -v java &> /dev/null; then
        local current=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
        if [ "$current" = "$JAVA_VERSION" ]; then
            log_info "Java $JAVA_VERSION 已安装"
            java -version
            return 0
        fi
    fi

    case $OS in
        ubuntu|debian)
            sudo apt-get update -qq
            sudo apt-get install -y -qq openjdk-${JAVA_VERSION}-jdk 2>/dev/null || install_java_from_adoptium
            ;;
        centos|rhel|rocky|almalinux|opencloudos)
            install_package java-${JAVA_VERSION}-openjdk-devel || install_java_from_adoptium
            ;;
        fedora)
            install_package java-${JAVA_VERSION}-openjdk-devel || install_java_from_adoptium
            ;;
    esac

    java -version
    log_success "Java 安装完成"
}

install_java_from_adoptium() {
    log_info "从 Adoptium 下载 Java $JAVA_VERSION..."

    sudo mkdir -p /opt/java

    local arch_name="x64"
    [ "$(uname -m)" = "aarch64" ] && arch_name="aarch64"

    local urls=(
        "https://mirrors.tuna.tsinghua.edu.cn/Adoptium/${JAVA_VERSION}/jdk/${arch_name}/linux/OpenJDK${JAVA_VERSION}U-jdk_${arch_name}_linux_hotspot.tar.gz"
        "https://api.adoptium.net/v3/binary/latest/${JAVA_VERSION}/ga/linux/${arch_name}/jdk/hotspot/normal/eclipse"
    )

    for url in "${urls[@]}"; do
        log_info "尝试下载: $url"
        if curl -fSL --connect-timeout 30 --max-time 600 --progress-bar -o /tmp/openjdk.tar.gz "$url" 2>/dev/null; then
            sudo tar -xzf /tmp/openjdk.tar.gz -C /opt/java
            rm -f /tmp/openjdk.tar.gz

            local extracted=$(ls -d /opt/java/jdk-${JAVA_VERSION}* 2>/dev/null | head -1)
            if [ -n "$extracted" ]; then
                sudo ln -sfn "$extracted" /opt/java/current

                cat << 'EOF' | sudo tee /etc/profile.d/java.sh
export JAVA_HOME=/opt/java/current
export PATH=$JAVA_HOME/bin:$PATH
EOF
                export JAVA_HOME=/opt/java/current
                export PATH=$JAVA_HOME/bin:$PATH

                log_success "Adoptium Java 安装完成"
                return 0
            fi
        fi
    done

    log_error "Java 安装失败"
    exit 1
}

#===============================================================================
# Jenkins 安装
#===============================================================================
install_jenkins() {
    log_info "下载 Jenkins ${JENKINS_VERSION}..."

    sudo mkdir -p /opt/jenkins
    sudo mkdir -p $JENKINS_HOME

    # 多镜像源下载
    local downloaded=false
    for url in "${JENKINS_WAR_MIRRORS[@]}"; do
        log_info "尝试下载: $url"
        if curl -fSL --connect-timeout 30 --max-time 900 --progress-bar -o /opt/jenkins/jenkins.war "$url" 2>/dev/null; then
            if [ -f /opt/jenkins/jenkins.war ] && [ -s /opt/jenkins/jenkins.war ]; then
                downloaded=true
                log_success "Jenkins WAR 下载成功"
                break
            fi
        fi
        log_warn "下载失败，尝试下一个镜像..."
    done

    if [ "$downloaded" = false ]; then
        log_error "所有镜像源下载失败"
        exit 1
    fi

    # 验证WAR文件
    if ! file /opt/jenkins/jenkins.war | grep -q "Java archive"; then
        log_warn "WAR文件格式验证失败，但继续安装"
    fi
}

#===============================================================================
# 配置国内镜像源
#===============================================================================
configure_update_center() {
    log_info "配置 Jenkins 插件更新站点（国内镜像）..."

    sudo mkdir -p $JENKINS_HOME/updates

    # 下载并修改 update-center.json
    local downloaded=false
    for url in "${JENKINS_UPDATE_MIRRORS[@]}"; do
        log_info "尝试从镜像下载更新中心配置: $url"
        if curl -fsSL --connect-timeout 15 --max-time 60 -o "$JENKINS_HOME/updates/default.json" "$url" 2>/dev/null; then
            if [ -f "$JENKINS_HOME/updates/default.json" ] && [ -s "$JENKINS_HOME/updates/default.json" ]; then
                downloaded=true
                log_success "更新中心配置下载成功"
                break
            fi
        fi
    done

    if [ "$downloaded" = false ]; then
        log_warn "无法下载更新中心配置，将使用默认设置"
    fi

    # 创建 hudson.model.UpdateCenter.xml 配置文件（使用官方HTTPS源）
    cat > $JENKINS_HOME/hudson.model.UpdateCenter.xml << 'UPDATE_CENTER_EOF'
<?xml version='1.1' encoding='UTF-8'?>
<sites>
  <site>
    <id>default</id>
    <url>https://updates.jenkins.io/update-center.json</url>
  </site>
</sites>
UPDATE_CENTER_EOF

    log_success "Jenkins 更新站点配置完成（使用官方源）"
}

#===============================================================================
# 创建启动脚本
#===============================================================================
create_start_script() {
    log_info "创建启动脚本..."

    cat > /opt/jenkins/start-jenkins.sh << 'SCRIPT_EOF'
#!/bin/bash

export JENKINS_HOME=__JENKINS_HOME__
# 基础 JVM 参数
BASE_JAVA_OPTS="__JAVA_OPTS__"
# 禁用设置向导，允许 Jenkins 直接启动
WIZARD_OPTS="-Djenkins.install.runSetupWizard=false"
# 合并所有 JVM 参数
export JAVA_OPTS="$BASE_JAVA_OPTS $WIZARD_OPTS"

# 设置时区
if [ -n "__TIMEZONE__" ] && [ "__TIMEZONE__" != "" ]; then
    export TZ="__TIMEZONE__"
fi

# 加载Java环境
[ -f /etc/profile.d/java.sh ] && source /etc/profile.d/java.sh

# 查找Java
if [ -z "$JAVA_HOME" ]; then
    for jdir in /opt/java/current /usr/lib/jvm/java-*; do
        if [ -x "$jdir/bin/java" ]; then
            export JAVA_HOME="$jdir"
            export PATH="$JAVA_HOME/bin:$PATH"
            break
        fi
    done
fi

cd /opt/jenkins

echo "=========================================="
echo "Jenkins Master 启动"
echo "=========================================="
echo "JENKINS_HOME: $JENKINS_HOME"
echo "JAVA_HOME: $JAVA_HOME"
echo "Java版本: $(java -version 2>&1 | head -1)"
echo "Jenkins端口: __JENKINS_PORT__"
echo "时区: ${TZ:-系统默认}"
echo "设置向导: 已禁用"
echo "=========================================="

exec java $JAVA_OPTS -jar jenkins.war --httpPort=__JENKINS_PORT__
SCRIPT_EOF

    sed -i "s|__JENKINS_HOME__|$JENKINS_HOME|g" /opt/jenkins/start-jenkins.sh
    sed -i "s|__JAVA_OPTS__|$JAVA_OPTS|g" /opt/jenkins/start-jenkins.sh
    sed -i "s|__JENKINS_PORT__|$JENKINS_PORT|g" /opt/jenkins/start-jenkins.sh
    sed -i "s|__TIMEZONE__|$TIMEZONE|g" /opt/jenkins/start-jenkins.sh

    chmod +x /opt/jenkins/start-jenkins.sh
    log_success "启动脚本创建完成"
}

#===============================================================================
# 创建 systemd 服务
#===============================================================================
create_systemd_service() {
    log_info "创建 systemd 服务..."

    # 设置时区（如果指定）
    local tz_env=""
    if [ -n "$TIMEZONE" ]; then
        tz_env="Environment=\"TZ=$TIMEZONE\""
        log_info "Jenkins 时区设置为: $TIMEZONE"
    fi

    sudo tee /etc/systemd/system/jenkins.service > /dev/null << SERVICE_EOF
[Unit]
Description=Jenkins Automation Server
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=root
Environment="JENKINS_HOME=$JENKINS_HOME"
${tz_env}
ExecStart=/opt/jenkins/start-jenkins.sh
Restart=always
RestartSec=10
StartLimitIntervalSec=60
StartLimitBurst=3

# 资源限制
MemoryMax=80%
CPUQuota=80%

[Install]
WantedBy=multi-user.target
SERVICE_EOF

    sudo systemctl daemon-reload
    sudo systemctl enable jenkins
    log_success "systemd 服务创建完成"
}

#===============================================================================
# 启动 Jenkins
#===============================================================================
start_jenkins() {
    log_info "启动 Jenkins..."

    sudo systemctl start jenkins

    # 等待启动
    log_info "等待 Jenkins 启动..."
    local max_wait=180
    local wait_time=0

    while [ $wait_time -lt $max_wait ]; do
        local http_code=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:$JENKINS_PORT" 2>/dev/null || echo "000")
        if [ "$http_code" = "403" ] || [ "$http_code" = "200" ]; then
            log_success "Jenkins 已启动 (HTTP: $http_code)"
            break
        fi
        sleep 5
        wait_time=$((wait_time + 5))
        printf "."
    done
    echo ""

    if [ $wait_time -ge $max_wait ]; then
        log_warn "Jenkins 启动超时，请手动检查"
    fi
}

#===============================================================================
# 获取初始密码
#===============================================================================
get_initial_password() {
    log_info "获取初始管理员密码..."

    # 等待初始密码文件生成
    local max_wait=60
    local wait_time=0
    local password_file="$JENKINS_HOME/secrets/initialAdminPassword"

    while [ $wait_time -lt $max_wait ]; do
        if [ -f "$password_file" ]; then
            INITIAL_PASSWORD=$(cat "$password_file")
            log_success "初始密码: $INITIAL_PASSWORD"
            echo ""
            echo "INITIAL_PASSWORD=$INITIAL_PASSWORD" > /tmp/jenkins_init_result.txt
            return 0
        fi
        sleep 5
        wait_time=$((wait_time + 5))
    done

    log_warn "未能获取初始密码，请手动查看: $password_file"
}

#===============================================================================
# 自动配置 (通过 Groovy init 脚本)
#===============================================================================
setup_admin_account() {
    log_info "配置管理员账号和系统设置..."

    sudo mkdir -p $JENKINS_HOME/init.groovy.d

    # 01. 创建管理员账号
    cat > $JENKINS_HOME/init.groovy.d/01_create_admin.groovy << 'GROOVY_EOF'
import jenkins.model.*
import hudson.security.*

def instance = Jenkins.getInstance()

println "=== 创建管理员账号 ==="

// 创建安全域
def hudsonRealm = new HudsonPrivateSecurityRealm(false)
hudsonRealm.createAccount("__ADMIN_USERNAME__", "__ADMIN_PASSWORD__")
instance.setSecurityRealm(hudsonRealm)

// 设置授权策略
def strategy = new FullControlOnceLoggedInAuthorizationStrategy()
strategy.setAllowAnonymousRead(false)
instance.setAuthorizationStrategy(strategy)

instance.save()
println "管理员账号创建成功: __ADMIN_USERNAME__"
GROOVY_EOF

    sed -i "s|__ADMIN_USERNAME__|$ADMIN_USERNAME|g" $JENKINS_HOME/init.groovy.d/01_create_admin.groovy
    sed -i "s|__ADMIN_PASSWORD__|$ADMIN_PASSWORD|g" $JENKINS_HOME/init.groovy.d/01_create_admin.groovy

    # 02. 跳过安装向导
    cat > $JENKINS_HOME/init.groovy.d/02_skip_wizard.groovy << 'GROOVY_EOF'
import jenkins.model.*
import jenkins.install.*

def instance = Jenkins.getInstance()

println "=== 跳过安装向导 ==="
instance.setInstallState(InstallState.INITIAL_SETUP_COMPLETED)
instance.save()
println "安装向导已跳过"
GROOVY_EOF

    # 03. 配置 Jenkins URL 和基本设置（使用实际 IP）
    cat > $JENKINS_HOME/init.groovy.d/03_basic_config.groovy << GROOVY_EOF
import jenkins.model.*

def instance = Jenkins.getInstance()

println "=== 配置基本设置 ==="

// 设置执行器数量
instance.setNumExecutors(2)

// 设置 Jenkins URL（使用实际 IP 地址）
def jenkinsUrl = "http://${DETECTED_IP}:${JENKINS_PORT}/"
def config = JenkinsLocationConfiguration.get()
config.setUrl(jenkinsUrl)
config.setAdminAddress("${ADMIN_EMAIL}")
config.save()

println "Jenkins URL: \${jenkinsUrl}"

instance.save()
println "基本配置完成"
GROOVY_EOF

    # 04. 配置全局环境变量
    cat > $JENKINS_HOME/init.groovy.d/04_global_env.groovy << 'GROOVY_EOF'
import jenkins.model.*
import hudson.slaves.EnvironmentVariablesNodeProperty
import hudson.slaves.EnvironmentVariablesNodeProperty.Entry

def instance = Jenkins.getInstance()

println "=== 配置全局环境变量 ==="

// 获取或创建环境变量属性
def globalNodeProperties = instance.getGlobalNodeProperties()
def envVarsNodePropertyList = globalNodeProperties.getAll(EnvironmentVariablesNodeProperty.class)

def envVars = null
if (envVarsNodePropertyList == null || envVarsNodePropertyList.size() == 0) {
    def newEnvVarsNodeProperty = new EnvironmentVariablesNodeProperty()
    globalNodeProperties.add(newEnvVarsNodeProperty)
    envVars = newEnvVarsNodeProperty.getEnvVars()
} else {
    envVars = envVarsNodePropertyList.get(0).getEnvVars()
}

// 设置环境变量
def envMap = [
    "JAVA_HOME": "/opt/java/current",
    "M2_HOME": "/opt/maven",
    "MAVEN_HOME": "/opt/maven",
    "PATH+EXTRA": "/opt/java/current/bin:/opt/maven/bin:/usr/local/bin"
]

envMap.each { key, value ->
    envVars.put(key, value)
    println "设置环境变量: ${key}=${value}"
}

instance.save()
println "全局环境变量配置完成"
GROOVY_EOF

    # 05. 配置工具位置（JDK、Maven、Git）
    cat > $JENKINS_HOME/init.groovy.d/05_tool_locations.groovy << 'GROOVY_EOF'
import jenkins.model.*
import hudson.model.*
import hudson.tools.*
import hudson.tasks.*

def instance = Jenkins.getInstance()

println "=== 配置工具位置 ==="

// 配置 JDK
def jdkDescriptor = instance.getDescriptorByType(hudson.model.JDK.DescriptorImpl.class)
def jdkInstallations = []

// 检测已安装的 JDK
def jdkPaths = [
    "/opt/java/current",
    "/usr/lib/jvm/java-17-openjdk-amd64",
    "/usr/lib/jvm/java-17-openjdk",
    "/usr/lib/jvm/java-11-openjdk-amd64",
    "/usr/lib/jvm/java-11-openjdk"
]

jdkPaths.each { path ->
    def javaExec = new File("${path}/bin/java")
    if (javaExec.exists()) {
        def jdkName = path.contains("17") ? "JDK17" : (path.contains("11") ? "JDK11" : "JDK")
        if (path == "/opt/java/current") jdkName = "JDK-Default"
        jdkInstallations.add(new hudson.model.JDK(jdkName, path))
        println "添加 JDK: ${jdkName} -> ${path}"
    }
}

if (jdkInstallations.size() > 0) {
    jdkDescriptor.setInstallations(jdkInstallations.toArray(new hudson.model.JDK[0]))
}

// 配置 Git
def gitDescriptor = instance.getDescriptorByType(hudson.plugins.git.GitTool.DescriptorImpl.class)
if (gitDescriptor != null) {
    def gitPath = "/usr/bin/git"
    if (new File(gitPath).exists()) {
        def gitInstallations = [new hudson.plugins.git.GitTool("Default", gitPath, null)]
        gitDescriptor.setInstallations(gitInstallations.toArray(new hudson.plugins.git.GitTool[0]))
        println "添加 Git: Default -> ${gitPath}"
    }
} else {
    println "Git 插件未安装，跳过 Git 工具配置"
}

// 配置 Maven（如果已安装）
def mavenDescriptor = instance.getDescriptorByType(hudson.tasks.Maven.DescriptorImpl.class)
if (mavenDescriptor != null) {
    def mavenPaths = ["/opt/maven", "/usr/share/maven"]
    def mavenInstallations = []

    mavenPaths.each { path ->
        def mvnExec = new File("${path}/bin/mvn")
        if (mvnExec.exists()) {
            mavenInstallations.add(new hudson.tasks.Maven.MavenInstallation("Maven", path, null))
            println "添加 Maven: Maven -> ${path}"
        }
    }

    if (mavenInstallations.size() > 0) {
        mavenDescriptor.setInstallations(mavenInstallations.toArray(new hudson.tasks.Maven.MavenInstallation[0]))
    }
} else {
    println "Maven 插件未安装，跳过 Maven 工具配置"
}

instance.save()
println "工具位置配置完成"
GROOVY_EOF

    # 06. 配置更新中心镜像（使用国内镜像提高插件下载速度）
    log_info "配置更新中心镜像..."
    cat > $JENKINS_HOME/init.groovy.d/05_update_center.groovy << 'GROOVY_EOF'
import jenkins.model.*

def instance = Jenkins.getInstance()

println "=== 配置更新中心 ==="

def uc = instance.getUpdateCenter()
def site = uc.getSites()[0]

// 确保使用 Jenkins 官方 HTTPS 更新中心
try {
    def officialUrl = "https://updates.jenkins.io/update-center.json"
    def field = site.getClass().getDeclaredField("url")
    field.setAccessible(true)
    field.set(site, new URL(officialUrl))
    println "更新中心已配置为: ${officialUrl}"
} catch (Exception e) {
    println "配置更新中心失败: ${e.message}，使用默认配置"
}

// 刷新更新中心
try {
    uc.updateAllSites()
    println "更新中心刷新请求已发送"
} catch (Exception e) {
    println "更新中心刷新失败: ${e.message}"
}

instance.save()
println "更新中心配置完成"
GROOVY_EOF

    # 07. 安装推荐插件（如果启用）
    if [ "$INSTALL_SUGGESTED_PLUGINS" = "true" ]; then
        log_info "配置自动安装推荐插件..."
        cat > $JENKINS_HOME/init.groovy.d/06_install_plugins.groovy << 'GROOVY_EOF'
import jenkins.model.*
import hudson.model.*
import hudson.PluginManager
import hudson.PluginWrapper

def instance = Jenkins.getInstance()

println "=== 安装推荐插件 ==="

// 推荐插件列表（仅包含稳定可用的核心功能插件）
def plugins = [
    // ========== 凭证管理 ==========
    "credentials",                    // 凭证核心插件
    "credentials-binding",            // 凭证绑定
    "ssh-credentials",                // SSH凭证支持
    "ssh-agent",                      // SSH Agent支持
    "plain-credentials",              // 纯文本凭证
    
    // ========== Git 集成 ==========
    "git",                            // Git 核心插件
    "git-client",                     // Git 客户端
    "github",                         // GitHub 集成
    "github-branch-source",           // GitHub 分支源
    "gitlab-plugin",                  // GitLab 集成
    "gitee",                          // Gitee 集成（国内常用）
    
    // ========== Pipeline ==========
    "workflow-aggregator",            // Pipeline 核心
    "pipeline-stage-view",            // Pipeline 阶段视图
    "pipeline-graph-analysis",        // Pipeline 图形分析
    "pipeline-input-step",            // Pipeline 输入步骤
    "pipeline-build-step",            // Pipeline 构建步骤
    "pipeline-stage-step",            // Pipeline 阶段步骤
    "blueocean",                      // Blue Ocean 现代UI
    
    // ========== 构建工具 ==========
    "ant",                            // Ant 构建
    "gradle",                         // Gradle 构建
    "maven-plugin",                   // Maven 插件
    "nodejs",                         // Node.js 支持
    
    // ========== Docker & Kubernetes ==========
    "docker-workflow",                // Docker Pipeline
    "docker-plugin",                  // Docker 插件
    "docker-commons",                 // Docker 公共库
    "kubernetes",                     // Kubernetes 支持
    "kubernetes-credentials",         // K8s 凭证
    
    // ========== 安全与权限 ==========
    "matrix-auth",                    // 矩阵授权
    "role-strategy",                  // 角色策略
    "authorize-project",              // 项目授权
    
    // ========== 配置管理 ==========
    "configuration-as-code",          // 配置即代码
    "job-dsl",                        // Job DSL
    
    // ========== 界面与显示 ==========
    "locale",                         // 本地化
    "localization-zh-cn",             // 中文本地化
    "timestamper",                    // 时间戳
    "build-name-setter",              // 构建名称设置
    "ansicolor",                      // ANSI颜色支持
    "dashboard-view",                 // 仪表盘视图
    
    // ========== 工作区与构建 ==========
    "ws-cleanup",                     // 工作区清理
    "build-timeout",                  // 构建超时
    "rebuild",                        // 重新构建
    "build-user-vars-plugin",         // 构建用户变量
    "parameterized-trigger",          // 参数化触发器
    
    // ========== 通知与报告 ==========
    "email-ext",                      // 扩展邮件
    "mailer",                         // 邮件
    "slack",                          // Slack 通知
    "dingtalk",                       // 钉钉通知（国内常用）
    
    // ========== 测试报告 ==========
    "junit",                          // JUnit 测试报告
    "htmlpublisher",                  // HTML 报告发布
    
    // ========== 实用工具 ==========
    "http_request",                   // HTTP 请求
    "copyartifact",                   // 复制构件
    "publish-over-ssh",               // SSH 发布
    "file-operations"                 // 文件操作
]

def pm = instance.getPluginManager()
def uc = instance.getUpdateCenter()

// 使用 Jenkins 官方更新中心（最可靠）
println "=== 配置更新中心 ==="
def site = uc.getSites()[0]

// 确保使用官方 HTTPS 更新中心
try {
    def officialUrl = "https://updates.jenkins.io/update-center.json"
    def field = site.getClass().getDeclaredField("url")
    field.setAccessible(true)
    field.set(site, new URL(officialUrl))
    println "更新中心: ${officialUrl}"
} catch (Exception e) {
    println "配置更新中心失败: ${e.message}，使用默认配置"
}

// 刷新更新中心，最多尝试3次
println "刷新更新中心..."
def refreshSuccess = false
for (int retry = 0; retry < 3 && !refreshSuccess; retry++) {
    try {
        uc.updateAllSites()
        Thread.sleep(3000)
        // 检查更新中心是否有数据
        if (uc.getSites()[0].getData() != null) {
            refreshSuccess = true
            println "更新中心刷新成功"
        } else {
            println "更新中心数据为空，重试 ${retry + 1}/3..."
            Thread.sleep(2000)
        }
    } catch (Exception e) {
        println "刷新失败 ${retry + 1}/3: ${e.message}"
        Thread.sleep(2000)
    }
}

if (!refreshSuccess) {
    println "警告: 更新中心刷新失败，插件安装可能不完整"
}

// 等待更新中心完全就绪
Thread.sleep(3000)

// 安装失败的插件列表（用于重试）
def failedPlugins = []

// 检查并安装插件
plugins.each { pluginName ->
    if (!pm.getPlugin(pluginName)) {
        println "安装插件: ${pluginName}"
        def plugin = uc.getPlugin(pluginName)
        if (plugin) {
            try {
                def future = plugin.deploy(true)
                future.get()
                println "插件 ${pluginName} 安装成功"
            } catch (Exception e) {
                println "插件 ${pluginName} 安装失败: ${e.message}"
                failedPlugins.add(pluginName)
            }
        } else {
            println "插件 ${pluginName} 在更新中心未找到"
        }
    } else {
        println "插件 ${pluginName} 已安装"
    }
}

// 对失败的插件进行一次重试
if (failedPlugins.size() > 0) {
    println ""
    println "=== 重试安装失败的插件 ==="
    Thread.sleep(3000)
    failedPlugins.each { pluginName ->
        def plugin = uc.getPlugin(pluginName)
        if (plugin) {
            try {
                plugin.deploy(true).get()
                println "插件 ${pluginName} 重试安装成功"
            } catch (Exception e) {
                println "插件 ${pluginName} 重试安装仍失败: ${e.message}"
            }
        }
    }
}

instance.save()
println ""
println "推荐插件安装完成"
println "注意: 部分插件需要重启 Jenkins 生效"
GROOVY_EOF
    else
        log_info "跳过推荐插件安装 (INSTALL_SUGGESTED_PLUGINS=false)"
    fi

    # 08. 创建凭证配置脚本（如果有凭证需要配置）
    log_info "配置凭证初始化脚本..."
    cat > $JENKINS_HOME/init.groovy.d/07_create_credentials.groovy << 'GROOVY_EOF'
import jenkins.model.*
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.impl.*
import com.cloudbees.plugins.credentials.domains.*

def instance = Jenkins.getInstance()

println "=== 创建预配置凭证 ==="

// 检查凭证插件是否可用
def pm = instance.getPluginManager()
if (!pm.getPlugin("credentials")) {
    println "凭证插件未安装，跳过凭证创建"
    return
}

def domain = Domain.global()
def store = null

try {
    store = instance.getExtensionList('com.cloudbees.plugins.credentials.SystemCredentialsProvider')[0].getStore()
} catch (Exception e) {
    println "无法获取凭证存储: ${e.message}"
    return
}

// 创建 Username/Password 凭证的函数
def createUsernamePasswordCredential = { id, description, username, password ->
    try {
        // 检查是否已存在
        def existing = com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials(
            com.cloudbees.plugins.credentials.common.StandardUsernameCredentials.class,
            instance, null, null
        ).find { it.id == id }
        
        if (existing != null) {
            println "凭证已存在，跳过: ${id}"
            return
        }
        
        def credential = new UsernamePasswordCredentialsImpl(
            CredentialsScope.GLOBAL,
            id,
            description,
            username,
            password
        )
        store.addCredentials(domain, credential)
        println "凭证创建成功: ${id}"
    } catch (Exception e) {
        println "创建凭证失败 ${id}: ${e.message}"
    }
}

// 创建 SSH 凭证的函数
def createSshCredential = { id, description, username, privateKeyBase64, passphrase ->
    try {
        // 检查 SSH 凭证插件是否可用
        if (!pm.getPlugin("ssh-credentials")) {
            println "SSH凭证插件未安装，跳过: ${id}"
            return
        }
        
        // 检查是否已存在
        def existing = com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials(
            com.cloudbees.plugins.credentials.common.StandardUsernameCredentials.class,
            instance, null, null
        ).find { it.id == id }
        
        if (existing != null) {
            println "SSH凭证已存在，跳过: ${id}"
            return
        }
        
        // 解码 Base64 私钥
        def privateKey = new String(privateKeyBase64.decodeBase64())
        
        def privateKeySource = new com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey.DirectEntryPrivateKeySource(privateKey)
        def credential = new com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey(
            CredentialsScope.GLOBAL,
            id,
            username,
            privateKeySource,
            passphrase ?: "",
            description
        )
        store.addCredentials(domain, credential)
        println "SSH凭证创建成功: ${id}"
    } catch (Exception e) {
        println "创建SSH凭证失败 ${id}: ${e.message}"
    }
}

// ===== 以下由后端动态生成 =====
${CREDENTIALS_GROOVY_SCRIPT}
// ===== 动态生成结束 =====

instance.save()
println "凭证初始化完成"
GROOVY_EOF

    log_success "Groovy 初始化脚本创建完成"
    log_info "重启 Jenkins 以应用配置..."

    sudo systemctl restart jenkins

    # 等待 Jenkins 重启完成
    log_info "等待 Jenkins 重启..."
    sleep 10
    local max_wait=120
    local wait_time=0

    while [ $wait_time -lt $max_wait ]; do
        local http_code=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:$JENKINS_PORT/login" 2>/dev/null || echo "000")
        if [ "$http_code" = "200" ]; then
            log_success "Jenkins 重启完成"
            break
        fi
        sleep 5
        wait_time=$((wait_time + 5))
        printf "."
    done
    echo ""
}

#===============================================================================
# 执行凭证创建脚本（凭证将在 Jenkins 完全初始化后自动创建）
#===============================================================================
execute_credentials_script() {
    local cred_script="$JENKINS_HOME/init.groovy.d/07_create_credentials.groovy"
    
    if [ ! -f "$cred_script" ]; then
        log_info "没有凭证配置脚本，跳过"
        return
    fi
    
    # 检查是否有实际的凭证配置（不只是注释）
    if grep -q "# 没有配置凭证" "$cred_script" 2>/dev/null; then
        log_info "没有配置凭证，跳过凭证创建"
        return
    fi
    
    log_info "检测到预配置凭证..."
    log_info "凭证脚本已创建: $cred_script"
    
    # 由于 Jenkins 初始化（包括插件安装）需要很长时间，
    # 我们不在这里等待，而是让凭证脚本保留在 init.groovy.d 中
    # 当用户首次登录 Jenkins 并完成初始化后，重启 Jenkins 即可自动创建凭证
    
    log_success "凭证配置已就绪"
    log_info "凭证将在 Jenkins 完全初始化并重启后自动创建"
    log_info "提示: 首次登录 Jenkins 完成初始化后，运行: sudo systemctl restart jenkins"
}

#===============================================================================
# 清理初始化脚本
#===============================================================================
cleanup_init_scripts() {
    log_info "清理初始化脚本（防止重复执行）..."

    # 移动脚本到备份目录（保留凭证脚本）
    sudo mkdir -p $JENKINS_HOME/init.groovy.d.backup
    
    # 移动除凭证脚本外的所有脚本到备份目录
    for script in $JENKINS_HOME/init.groovy.d/*.groovy; do
        local script_name=$(basename "$script")
        if [ "$script_name" != "07_create_credentials.groovy" ]; then
            sudo mv "$script" $JENKINS_HOME/init.groovy.d.backup/ 2>/dev/null || true
        fi
    done
    
    # 检查是否有凭证脚本保留
    if [ -f "$JENKINS_HOME/init.groovy.d/07_create_credentials.groovy" ]; then
        log_info "凭证脚本已保留，将在 Jenkins 下次重启时执行"
    fi

    log_success "初始化脚本已备份到 $JENKINS_HOME/init.groovy.d.backup/"
}

#===============================================================================
# 生成报告
#===============================================================================
generate_report() {
    local end_time=$(date +%s)
    local duration=$((end_time - SCRIPT_START_TIME))

    echo ""
    echo -e "${GREEN}${BOLD}╔══════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}${BOLD}║              Jenkins Master 部署完成                             ║${NC}"
    echo -e "${GREEN}${BOLD}╠══════════════════════════════════════════════════════════════════╣${NC}"
    echo -e "${GREEN}${BOLD}║${NC}  部署耗时: ${duration}秒                                              ${GREEN}${BOLD}║${NC}"
    echo -e "${GREEN}${BOLD}║${NC}  访问地址: http://${DETECTED_IP}:${JENKINS_PORT}                      ${GREEN}${BOLD}║${NC}"
    echo -e "${GREEN}${BOLD}║${NC}  管理员: ${ADMIN_USERNAME}                                           ${GREEN}${BOLD}║${NC}"
    echo -e "${GREEN}${BOLD}║${NC}  Jenkins Home: ${JENKINS_HOME}                                ${GREEN}${BOLD}║${NC}"
    echo -e "${GREEN}${BOLD}║${NC}  更新站点: 清华镜像                                               ${GREEN}${BOLD}║${NC}"
    echo -e "${GREEN}${BOLD}╠══════════════════════════════════════════════════════════════════╣${NC}"
    echo -e "${GREEN}${BOLD}║${NC}  ${CYAN}已配置:${NC}                                                       ${GREEN}${BOLD}║${NC}"
    echo -e "${GREEN}${BOLD}║${NC}    - Jenkins URL: http://${DETECTED_IP}:${JENKINS_PORT}               ${GREEN}${BOLD}║${NC}"
    echo -e "${GREEN}${BOLD}║${NC}    - 全局环境变量 (JAVA_HOME, M2_HOME, PATH)                      ${GREEN}${BOLD}║${NC}"
    echo -e "${GREEN}${BOLD}║${NC}    - 工具位置 (JDK, Git, Maven)                                  ${GREEN}${BOLD}║${NC}"
    echo -e "${GREEN}${BOLD}║${NC}    - 国内镜像更新站点                                            ${GREEN}${BOLD}║${NC}"
    if [ "$INSTALL_SUGGESTED_PLUGINS" = "true" ]; then
    echo -e "${GREEN}${BOLD}║${NC}    - 推荐插件自动安装                                            ${GREEN}${BOLD}║${NC}"
    fi
    echo -e "${GREEN}${BOLD}╠══════════════════════════════════════════════════════════════════╣${NC}"
    echo -e "${GREEN}${BOLD}║${NC}  ${CYAN}管理命令:${NC}                                                     ${GREEN}${BOLD}║${NC}"
    echo -e "${GREEN}${BOLD}║${NC}    启动: sudo systemctl start jenkins                           ${GREEN}${BOLD}║${NC}"
    echo -e "${GREEN}${BOLD}║${NC}    停止: sudo systemctl stop jenkins                            ${GREEN}${BOLD}║${NC}"
    echo -e "${GREEN}${BOLD}║${NC}    状态: sudo systemctl status jenkins                          ${GREEN}${BOLD}║${NC}"
    echo -e "${GREEN}${BOLD}║${NC}    日志: sudo journalctl -u jenkins -f                          ${GREEN}${BOLD}║${NC}"
    echo -e "${GREEN}${BOLD}╚══════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

#===============================================================================
# 主函数
#===============================================================================
main() {
    echo ""
    echo -e "${CYAN}${BOLD}╔══════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}${BOLD}║         Jenkins Master 自动部署脚本 v2.0.0                        ║${NC}"
    echo -e "${CYAN}${BOLD}╚══════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "  Jenkins版本:    ${BOLD}${JENKINS_VERSION}${NC}"
    echo -e "  HTTP端口:       ${BOLD}${JENKINS_PORT}${NC}"
    echo -e "  Jenkins Home:   ${BOLD}${JENKINS_HOME}${NC}"
    echo -e "  Java版本:       ${BOLD}${JAVA_VERSION}${NC}"
    echo -e "  管理员用户:     ${BOLD}${ADMIN_USERNAME}${NC}"
    echo -e "  安装推荐插件:   ${BOLD}${INSTALL_SUGGESTED_PLUGINS}${NC}"
    echo ""

    # 步骤1: 系统检测
    log_step "系统环境检测"
    detect_os
    detect_ip
    check_disk_space
    check_memory
    check_port

    # 步骤2: 基础依赖
    log_step "安装基础依赖"
    install_package curl
    install_package wget
    install_package tar
    install_package gzip
    install_package net-tools
    install_package git

    # 步骤3: Java
    log_step "安装 Java ${JAVA_VERSION}"
    install_java

    # 步骤4: 下载Jenkins
    log_step "下载 Jenkins ${JENKINS_VERSION}"
    install_jenkins

    # 步骤5: 配置镜像源
    log_step "配置国内镜像源"
    configure_update_center

    # 步骤6: 创建目录
    log_step "创建目录和脚本"
    create_start_script

    # 步骤7: 创建服务
    log_step "创建 systemd 服务"
    create_systemd_service

    # 步骤8: 启动Jenkins
    log_step "启动 Jenkins"
    start_jenkins

    # 步骤9: 获取初始密码
    log_step "获取初始密码"
    get_initial_password

    # 步骤10: 自动配置
    log_step "配置管理员账号和系统设置"
    setup_admin_account

    # 步骤11: 执行凭证创建脚本
    log_step "创建预配置凭证"
    execute_credentials_script

    # 步骤12: 清理初始化脚本
    log_step "清理初始化脚本"
    cleanup_init_scripts

    # 步骤13: 生成报告
    log_step "部署完成"
    generate_report

    echo ""
    echo "=== Jenkins Master 安装完成 ==="
}

# 错误处理
handle_error() {
    log_error "脚本执行出错，行号: $1"
    exit 1
}

trap 'handle_error $LINENO' ERR

# 执行主函数
main "$@"
