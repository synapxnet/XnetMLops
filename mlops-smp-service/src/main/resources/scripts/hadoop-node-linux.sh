#!/bin/bash
#===============================================================================
# Hadoop Node 节点部署脚本 (Linux)
# 用于部署 DataNode 和 NodeManager
# 支持动态 Java 版本和端口配置
#===============================================================================

set -e

#===============================================================================
# 配置变量 (由后端替换)
#===============================================================================
HADOOP_VERSION="${HADOOP_VERSION:-3.3.6}"
OS_TYPE="${OS_TYPE:-linux}"
JAVA_VERSION="${JAVA_VERSION:-8}"
MASTER_HOST="${MASTER_HOST:-localhost}"
HDFS_DATA_DIRS="${HDFS_DATA_DIRS:-/data/hadoop/hdfs}"
YARN_MEMORY="${YARN_MEMORY:-8192}"
YARN_CPU="${YARN_CPU:-4}"

# 端口配置
NAMENODE_PORT="${NAMENODE_PORT:-9000}"
NAMENODE_HTTP_PORT="${NAMENODE_HTTP_PORT:-9870}"
DATANODE_PORT="${DATANODE_PORT:-9866}"
RESOURCEMANAGER_PORT="${RESOURCEMANAGER_PORT:-8032}"
RESOURCEMANAGER_WEB_PORT="${RESOURCEMANAGER_WEB_PORT:-8088}"
NODEMANAGER_PORT="${NODEMANAGER_PORT:-8042}"

#===============================================================================
# 固定配置
#===============================================================================
HADOOP_HOME="/opt/hadoop"
HADOOP_CONF_DIR="$HADOOP_HOME/etc/hadoop"
HADOOP_USER="hadoop"
JAVA_HOME=""  # 将在 install_java 中设置

# 日志颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'
BOLD='\033[1m'

#===============================================================================
# 日志函数
#===============================================================================
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

#===============================================================================
# 系统检测
#===============================================================================
detect_os() {
    log_info "检测操作系统..."
    
    if [ -f /etc/os-release ]; then
        . /etc/os-release
        OS_NAME=$ID
        OS_VERSION=$VERSION_ID
    elif [ -f /etc/redhat-release ]; then
        OS_NAME="centos"
        OS_VERSION=$(cat /etc/redhat-release | grep -oE '[0-9]+' | head -1)
    else
        OS_NAME="unknown"
        OS_VERSION="unknown"
    fi
    
    log_info "检测到操作系统: $OS_NAME $OS_VERSION"
}

#===============================================================================
# 安装 Java
#===============================================================================
install_java() {
    log_info "安装 Java $JAVA_VERSION..."
    
    # 检查现有 Java 版本
    if command -v java &> /dev/null; then
        CURRENT_JAVA=$(java -version 2>&1 | head -1)
        log_info "检测到已安装 Java: $CURRENT_JAVA"
    fi
    
    case $OS_NAME in
        centos|rhel|rocky|almalinux|opencloudos|tencentos|anolis|alinux|kylin|uos|fedora)
            install_java_rhel
            ;;
        ubuntu|debian)
            install_java_debian
            ;;
        *)
            log_warn "未知操作系统，尝试通用安装方式..."
            install_java_rhel
            ;;
    esac
    
    # 验证 Java 安装
    if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
        log_success "Java 安装完成: $JAVA_HOME"
        $JAVA_HOME/bin/java -version
    else
        log_error "Java 安装失败，JAVA_HOME: $JAVA_HOME"
        exit 1
    fi
}

install_java_rhel() {
    local java_pkg=""
    local alt_pkg=""
    local pkg_prefix=""
    local java_home_path=""

    case $JAVA_VERSION in
        8)
            java_pkg="java-8-konajdk"
            alt_pkg="java-8-konajdk-devel"
            pkg_prefix="java-8-konajdk"
            ;;
        11)
            java_pkg="java-11-konajdk"
            alt_pkg="java-11-konajdk-devel"
            pkg_prefix="java-11-konajdk"
            ;;
        17)
            java_pkg="java-17-konajdk"
            alt_pkg="java-17-konajdk-devel"
            pkg_prefix="java-17-konajdk"
            ;;
        21)
            java_pkg="java-21-konajdk"
            alt_pkg="java-21-konajdk-devel"
            pkg_prefix="java-21-konajdk"
            ;;
        *)
            log_warn "不支持的 Java 版本: $JAVA_VERSION，使用 Java 8"
            java_pkg="java-8-konajdk"
            alt_pkg="java-8-konajdk-devel"
            pkg_prefix="java-8-konajdk"
            ;;
    esac
    
    # 尝试安装 konajdk，如果失败则尝试 openjdk
    if command -v dnf &> /dev/null; then
        sudo dnf install -y $java_pkg $alt_pkg 2>/dev/null || \
        sudo dnf install -y java-${JAVA_VERSION}-openjdk java-${JAVA_VERSION}-openjdk-devel 2>/dev/null || \
        sudo dnf install -y java-1.8.0-openjdk java-1.8.0-openjdk-devel 2>/dev/null || true
    else
        sudo yum install -y $java_pkg $alt_pkg 2>/dev/null || \
        sudo yum install -y java-${JAVA_VERSION}-openjdk java-${JAVA_VERSION}-openjdk-devel 2>/dev/null || \
        sudo yum install -y java-1.8.0-openjdk java-1.8.0-openjdk-devel 2>/dev/null || true
    fi
    
    # 智能查找 JAVA_HOME - 优先查找与用户指定版本匹配的 Java
    JAVA_HOME=""
    
    # 1. 首先在 /usr/lib/jvm 中查找与版本匹配的目录（优先级最高）
    log_info "查找 Java ${JAVA_VERSION} 安装路径..."
    
    # 定义版本匹配的搜索模式
    local version_patterns=(
        "java-${JAVA_VERSION}-konajdk*"
        "java-${JAVA_VERSION}-openjdk*"
        "java-1.${JAVA_VERSION}.0-*"
        "jdk-${JAVA_VERSION}*"
        "java-${JAVA_VERSION}*"
    )
    
    for pattern in "${version_patterns[@]}"; do
        local found=$(find /usr/lib/jvm -maxdepth 1 -type d -name "$pattern" 2>/dev/null | sort -V | tail -1)
        if [ -n "$found" ] && [ -x "$found/bin/java" ]; then
            JAVA_HOME="$found"
            log_info "找到版本匹配的 Java: $JAVA_HOME"
            break
        fi
    done
    
    # 2. 如果没有直接匹配，检查子目录
    if [ -z "$JAVA_HOME" ] || [ ! -x "$JAVA_HOME/bin/java" ]; then
        for pattern in "${version_patterns[@]}"; do
            local found=$(find /usr/lib/jvm -maxdepth 1 -type d -name "$pattern" 2>/dev/null | sort -V | tail -1)
            if [ -n "$found" ]; then
                if [ -x "$found/jre/bin/java" ]; then
                    JAVA_HOME="$found/jre"
                    log_info "找到版本匹配的 Java JRE: $JAVA_HOME"
                    break
                fi
            fi
        done
    fi
    
    # 3. 最后备选：使用 which java
    if [ -z "$JAVA_HOME" ] || [ ! -x "$JAVA_HOME/bin/java" ]; then
        log_warn "未找到指定版本的 Java，使用系统默认..."
        local java_bin=$(which java 2>/dev/null)
        if [ -n "$java_bin" ]; then
            local real_path=$(readlink -f "$java_bin")
            JAVA_HOME=$(dirname $(dirname "$real_path"))
        fi
    fi
    
    log_info "检测到 JAVA_HOME: $JAVA_HOME"
}

install_java_debian() {
    local java_pkg=""
    
    case $JAVA_VERSION in
        8)
            java_pkg="openjdk-8-jdk"
            JAVA_HOME="/usr/lib/jvm/java-8-openjdk-amd64"
            ;;
        11)
            java_pkg="openjdk-11-jdk"
            JAVA_HOME="/usr/lib/jvm/java-11-openjdk-amd64"
            ;;
        17)
            java_pkg="openjdk-17-jdk"
            JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"
            ;;
        21)
            java_pkg="openjdk-21-jdk"
            JAVA_HOME="/usr/lib/jvm/java-21-openjdk-amd64"
            ;;
        *)
            log_warn "不支持的 Java 版本: $JAVA_VERSION，使用 Java 8"
            java_pkg="openjdk-8-jdk"
            JAVA_HOME="/usr/lib/jvm/java-8-openjdk-amd64"
            ;;
    esac
    
    sudo apt-get update
    sudo apt-get install -y $java_pkg
}

#===============================================================================
# 创建 Hadoop 用户
#===============================================================================
create_hadoop_user() {
    log_info "创建 Hadoop 用户..."
    
    if id "$HADOOP_USER" &>/dev/null; then
        log_info "用户 $HADOOP_USER 已存在"
    else
        sudo useradd -m -s /bin/bash $HADOOP_USER
        echo "$HADOOP_USER:hadoop123" | sudo chpasswd
        log_success "用户 $HADOOP_USER 创建完成"
    fi
}

#===============================================================================
# 下载并安装 Hadoop
#===============================================================================
install_hadoop() {
    log_info "安装 Hadoop $HADOOP_VERSION..."
    
    if [ -d "$HADOOP_HOME" ]; then
        log_info "Hadoop 已安装在 $HADOOP_HOME"
        return
    fi
    
    # 下载 Hadoop
    HADOOP_URL="https://archive.apache.org/dist/hadoop/common/hadoop-$HADOOP_VERSION/hadoop-$HADOOP_VERSION.tar.gz"
    HADOOP_MIRROR_URL="https://mirrors.tuna.tsinghua.edu.cn/apache/hadoop/common/hadoop-$HADOOP_VERSION/hadoop-$HADOOP_VERSION.tar.gz"
    
    log_info "下载 Hadoop..."
    cd /tmp
    if ! wget -q "$HADOOP_MIRROR_URL" -O hadoop-$HADOOP_VERSION.tar.gz 2>/dev/null; then
        log_warn "镜像下载失败，尝试官方源..."
        wget -q "$HADOOP_URL" -O hadoop-$HADOOP_VERSION.tar.gz
    fi
    
    # 解压安装
    log_info "解压 Hadoop..."
    sudo tar -xzf hadoop-$HADOOP_VERSION.tar.gz -C /opt
    sudo mv /opt/hadoop-$HADOOP_VERSION $HADOOP_HOME
    sudo chown -R $HADOOP_USER:$HADOOP_USER $HADOOP_HOME
    
    # 清理
    rm -f hadoop-$HADOOP_VERSION.tar.gz
    
    log_success "Hadoop $HADOOP_VERSION 安装完成"
}

#===============================================================================
# 创建数据目录
#===============================================================================
create_data_dirs() {
    log_info "创建数据目录..."
    
    # 创建 HDFS 数据目录
    IFS=',' read -ra DIRS <<< "$HDFS_DATA_DIRS"
    for dir in "${DIRS[@]}"; do
        sudo mkdir -p "$dir/datanode"
        sudo chown -R $HADOOP_USER:$HADOOP_USER "$dir"
    done
    
    # 创建日志目录
    sudo mkdir -p /var/log/hadoop
    sudo chown -R $HADOOP_USER:$HADOOP_USER /var/log/hadoop
    
    log_success "数据目录创建完成"
}

#===============================================================================
# 配置环境变量
#===============================================================================
configure_env() {
    log_info "配置环境变量..."
    
    # 配置 Hadoop 环境变量
    cat > /tmp/hadoop-env.sh << EOF
# Hadoop 环境变量
export JAVA_HOME=$JAVA_HOME
export HADOOP_HOME=$HADOOP_HOME
export HADOOP_CONF_DIR=$HADOOP_CONF_DIR
export PATH=\$PATH:\$HADOOP_HOME/bin:\$HADOOP_HOME/sbin
export HADOOP_OPTS="-Djava.library.path=\$HADOOP_HOME/lib/native"
EOF
    
    sudo mv /tmp/hadoop-env.sh /etc/profile.d/hadoop.sh
    source /etc/profile.d/hadoop.sh
    
    # 配置 hadoop-env.sh
    echo "export JAVA_HOME=$JAVA_HOME" | sudo tee -a $HADOOP_CONF_DIR/hadoop-env.sh
    echo "export HDFS_DATANODE_USER=$HADOOP_USER" | sudo tee -a $HADOOP_CONF_DIR/hadoop-env.sh
    echo "export YARN_NODEMANAGER_USER=$HADOOP_USER" | sudo tee -a $HADOOP_CONF_DIR/hadoop-env.sh
    
    log_success "环境变量配置完成"
}

#===============================================================================
# 配置 core-site.xml
#===============================================================================
configure_core_site() {
    log_info "配置 core-site.xml..."
    
    cat > $HADOOP_CONF_DIR/core-site.xml << EOF
<?xml version="1.0" encoding="UTF-8"?>
<?xml-stylesheet type="text/xsl" href="configuration.xsl"?>
<configuration>
    <property>
        <name>fs.defaultFS</name>
        <value>hdfs://$MASTER_HOST:$NAMENODE_PORT</value>
    </property>
    <property>
        <name>hadoop.tmp.dir</name>
        <value>/tmp/hadoop-\${user.name}</value>
    </property>
</configuration>
EOF
    
    sudo chown $HADOOP_USER:$HADOOP_USER $HADOOP_CONF_DIR/core-site.xml
    log_success "core-site.xml 配置完成"
}

#===============================================================================
# 配置 hdfs-site.xml
#===============================================================================
configure_hdfs_site() {
    log_info "配置 hdfs-site.xml..."
    
    # 获取第一个数据目录
    IFS=',' read -ra DIRS <<< "$HDFS_DATA_DIRS"
    DATANODE_DIR="${DIRS[0]}/datanode"
    
    cat > $HADOOP_CONF_DIR/hdfs-site.xml << EOF
<?xml version="1.0" encoding="UTF-8"?>
<?xml-stylesheet type="text/xsl" href="configuration.xsl"?>
<configuration>
    <property>
        <name>dfs.datanode.data.dir</name>
        <value>file://$DATANODE_DIR</value>
    </property>
    <property>
        <name>dfs.datanode.address</name>
        <value>0.0.0.0:$DATANODE_PORT</value>
    </property>
</configuration>
EOF
    
    sudo chown $HADOOP_USER:$HADOOP_USER $HADOOP_CONF_DIR/hdfs-site.xml
    log_success "hdfs-site.xml 配置完成"
}

#===============================================================================
# 配置 yarn-site.xml
#===============================================================================
configure_yarn_site() {
    log_info "配置 yarn-site.xml..."
    
    cat > $HADOOP_CONF_DIR/yarn-site.xml << EOF
<?xml version="1.0" encoding="UTF-8"?>
<?xml-stylesheet type="text/xsl" href="configuration.xsl"?>
<configuration>
    <property>
        <name>yarn.resourcemanager.hostname</name>
        <value>$MASTER_HOST</value>
    </property>
    <property>
        <name>yarn.resourcemanager.address</name>
        <value>$MASTER_HOST:$RESOURCEMANAGER_PORT</value>
    </property>
    <property>
        <name>yarn.nodemanager.aux-services</name>
        <value>mapreduce_shuffle</value>
    </property>
    <property>
        <name>yarn.nodemanager.resource.memory-mb</name>
        <value>$YARN_MEMORY</value>
    </property>
    <property>
        <name>yarn.nodemanager.resource.cpu-vcores</name>
        <value>$YARN_CPU</value>
    </property>
    <property>
        <name>yarn.nodemanager.webapp.address</name>
        <value>0.0.0.0:$NODEMANAGER_PORT</value>
    </property>
</configuration>
EOF
    
    sudo chown $HADOOP_USER:$HADOOP_USER $HADOOP_CONF_DIR/yarn-site.xml
    log_success "yarn-site.xml 配置完成"
}

#===============================================================================
# 配置 mapred-site.xml
#===============================================================================
configure_mapred_site() {
    log_info "配置 mapred-site.xml..."
    
    cat > $HADOOP_CONF_DIR/mapred-site.xml << EOF
<?xml version="1.0" encoding="UTF-8"?>
<?xml-stylesheet type="text/xsl" href="configuration.xsl"?>
<configuration>
    <property>
        <name>mapreduce.framework.name</name>
        <value>yarn</value>
    </property>
    <property>
        <name>mapreduce.application.classpath</name>
        <value>\$HADOOP_HOME/share/hadoop/mapreduce/*:\$HADOOP_HOME/share/hadoop/mapreduce/lib/*</value>
    </property>
</configuration>
EOF
    
    sudo chown $HADOOP_USER:$HADOOP_USER $HADOOP_CONF_DIR/mapred-site.xml
    log_success "mapred-site.xml 配置完成"
}

#===============================================================================
# 创建 systemd 服务
#===============================================================================
create_systemd_services() {
    log_info "创建 systemd 服务..."
    
    # DataNode 服务
    cat > /tmp/hadoop-datanode.service << EOF
[Unit]
Description=Hadoop HDFS DataNode
After=network.target

[Service]
Type=forking
User=$HADOOP_USER
Group=$HADOOP_USER
Environment="JAVA_HOME=$JAVA_HOME"
Environment="HADOOP_HOME=$HADOOP_HOME"
ExecStart=$HADOOP_HOME/bin/hdfs --daemon start datanode
ExecStop=$HADOOP_HOME/bin/hdfs --daemon stop datanode
Restart=on-failure

[Install]
WantedBy=multi-user.target
EOF
    sudo mv /tmp/hadoop-datanode.service /etc/systemd/system/
    
    # NodeManager 服务
    cat > /tmp/hadoop-nodemanager.service << EOF
[Unit]
Description=Hadoop YARN NodeManager
After=network.target hadoop-datanode.service

[Service]
Type=forking
User=$HADOOP_USER
Group=$HADOOP_USER
Environment="JAVA_HOME=$JAVA_HOME"
Environment="HADOOP_HOME=$HADOOP_HOME"
ExecStart=$HADOOP_HOME/bin/yarn --daemon start nodemanager
ExecStop=$HADOOP_HOME/bin/yarn --daemon stop nodemanager
Restart=on-failure

[Install]
WantedBy=multi-user.target
EOF
    sudo mv /tmp/hadoop-nodemanager.service /etc/systemd/system/
    
    sudo systemctl daemon-reload
    
    log_success "systemd 服务创建完成"
}

#===============================================================================
# 启动服务
#===============================================================================
start_services() {
    log_info "启动 Hadoop 服务..."
    
    source /etc/profile.d/hadoop.sh
    
    # 启动 DataNode
    sudo -u $HADOOP_USER $HADOOP_HOME/bin/hdfs --daemon start datanode
    
    # 启动 NodeManager
    sudo -u $HADOOP_USER $HADOOP_HOME/bin/yarn --daemon start nodemanager
    
    log_success "Hadoop 服务启动完成"
}

#===============================================================================
# 验证安装
#===============================================================================
verify_installation() {
    log_info "验证安装..."
    
    # 检查进程
    log_info "检查 Hadoop 进程..."
    sudo -u $HADOOP_USER jps
    
    log_success "安装验证完成"
}

#===============================================================================
# 生成报告
#===============================================================================
generate_report() {
    echo ""
    echo -e "${GREEN}${BOLD}╔══════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}${BOLD}║             Hadoop Node 部署完成                                  ║${NC}"
    echo -e "${GREEN}${BOLD}╚══════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${BOLD}Hadoop 信息:${NC}"
    echo -e "  版本:            $HADOOP_VERSION"
    echo -e "  Java版本:        $JAVA_VERSION"
    echo -e "  JAVA_HOME:       $JAVA_HOME"
    echo -e "  安装目录:        $HADOOP_HOME"
    echo -e "  Master 节点:     $MASTER_HOST"
    echo ""
    echo -e "${BOLD}DataNode 信息:${NC}"
    echo -e "  数据目录:        $HDFS_DATA_DIRS"
    echo -e "  DataNode 端口:   $DATANODE_PORT"
    echo ""
    echo -e "${BOLD}NodeManager 信息:${NC}"
    echo -e "  内存:            ${YARN_MEMORY}MB"
    echo -e "  CPU:             ${YARN_CPU} 核"
    echo -e "  Web UI 端口:     $NODEMANAGER_PORT"
    echo ""
    echo -e "${BOLD}常用命令:${NC}"
    echo -e "  启动 DataNode:   hdfs --daemon start datanode"
    echo -e "  停止 DataNode:   hdfs --daemon stop datanode"
    echo -e "  启动 NodeManager: yarn --daemon start nodemanager"
    echo -e "  停止 NodeManager: yarn --daemon stop nodemanager"
    echo -e "  查看进程:        jps"
    echo ""
}

#===============================================================================
# 主函数
#===============================================================================
main() {
    log_info "开始部署 Hadoop Node..."
    log_info "Hadoop 版本: $HADOOP_VERSION"
    log_info "Java 版本: $JAVA_VERSION"
    log_info "Master 节点: $MASTER_HOST"
    
    detect_os
    install_java
    create_hadoop_user
    install_hadoop
    create_data_dirs
    configure_env
    configure_core_site
    configure_hdfs_site
    configure_yarn_site
    configure_mapred_site
    create_systemd_services
    start_services
    verify_installation
    generate_report
    
    log_success "Hadoop Node 部署完成!"
}

# 执行主函数
main "$@"
