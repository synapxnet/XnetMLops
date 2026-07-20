#!/bin/bash
#===============================================================================
# Hadoop Master 节点部署脚本 (Linux)
# 支持 HDFS, YARN, MapReduce
# 支持标准模式和 HA 模式
# 支持动态 Java 版本和端口配置
#===============================================================================

set -e

#===============================================================================
# 配置变量 (由后端替换)
#===============================================================================
HADOOP_VERSION="${HADOOP_VERSION:-3.3.6}"
OS_TYPE="${OS_TYPE:-linux}"
DEPLOY_MODE="${DEPLOY_MODE:-standard}"
JAVA_VERSION="${JAVA_VERSION:-8}"
HDFS_DATA_DIRS="${HDFS_DATA_DIRS:-/data/hadoop/hdfs}"
HDFS_REPLICATION="${HDFS_REPLICATION:-3}"
HDFS_BLOCK_SIZE="${HDFS_BLOCK_SIZE:-134217728}"
YARN_MEMORY="${YARN_MEMORY:-8192}"
YARN_CPU="${YARN_CPU:-4}"
MASTER_HOST="${MASTER_HOST:-localhost}"
HA_MASTER_HOST="${HA_MASTER_HOST:-}"
ZK_CLUSTER="${ZK_CLUSTER:-}"

# 端口配置
NAMENODE_PORT="${NAMENODE_PORT:-9000}"
NAMENODE_HTTP_PORT="${NAMENODE_HTTP_PORT:-9870}"
DATANODE_PORT="${DATANODE_PORT:-9866}"
SECONDARY_NAMENODE_HTTP_PORT="${SECONDARY_NAMENODE_HTTP_PORT:-9868}"
RESOURCEMANAGER_PORT="${RESOURCEMANAGER_PORT:-8032}"
RESOURCEMANAGER_WEB_PORT="${RESOURCEMANAGER_WEB_PORT:-8088}"
NODEMANAGER_PORT="${NODEMANAGER_PORT:-8042}"
JOBHISTORY_PORT="${JOBHISTORY_PORT:-10020}"
JOBHISTORY_WEB_PORT="${JOBHISTORY_WEB_PORT:-19888}"

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
    local pkg_prefix=""
    local java_pkg=""
    local alt_pkg=""
    
    case $JAVA_VERSION in
        8)
            java_pkg="java-1.8.0-openjdk java-1.8.0-openjdk-devel"
            alt_pkg="java-8-konajdk java-8-konajdk-devel"
            pkg_prefix="java-1.8.0-openjdk"
            ;;
        11)
            java_pkg="java-11-openjdk java-11-openjdk-devel"
            alt_pkg="java-11-konajdk java-11-konajdk-devel"
            pkg_prefix="java-11-openjdk"
            ;;
        17)
            java_pkg="java-17-openjdk java-17-openjdk-devel"
            alt_pkg="java-17-konajdk java-17-konajdk-devel"
            pkg_prefix="java-17-openjdk"
            ;;
        21)
            java_pkg="java-21-openjdk java-21-openjdk-devel"
            alt_pkg="java-21-konajdk java-21-konajdk-devel"
            pkg_prefix="java-21-openjdk"
            ;;
        *)
            log_warn "不支持的 Java 版本: $JAVA_VERSION，使用 Java 8"
            java_pkg="java-1.8.0-openjdk java-1.8.0-openjdk-devel"
            alt_pkg="java-8-konajdk java-8-konajdk-devel"
            pkg_prefix="java-1.8.0-openjdk"
            ;;
    esac
    
    # 尝试安装 Java
    if command -v dnf &> /dev/null; then
        sudo dnf install -y $java_pkg 2>/dev/null || sudo dnf install -y $alt_pkg 2>/dev/null || true
    else
        sudo yum install -y $java_pkg 2>/dev/null || sudo yum install -y $alt_pkg 2>/dev/null || true
    fi
    
    # 智能查找 JAVA_HOME - 优先查找与用户指定版本匹配的 Java
    JAVA_HOME=""
    
    # 1. 首先在 /usr/lib/jvm 中查找与版本匹配的目录（优先级最高）
    log_info "查找 Java ${JAVA_VERSION} 安装路径..."
    
    # 定义版本匹配的搜索模式
    local version_patterns=(
        "java-${JAVA_VERSION}-konajdk*"
        "java-${JAVA_VERSION}-openjdk*"
        "jdk-${JAVA_VERSION}*"
        "java-${JAVA_VERSION}*"
        "temurin-${JAVA_VERSION}*"
    )
    
    for pattern in "${version_patterns[@]}"; do
        local found=$(find /usr/lib/jvm -maxdepth 1 -type d -name "$pattern" 2>/dev/null | sort -V | tail -1)
        if [ -n "$found" ] && [ -x "$found/bin/java" ]; then
            JAVA_HOME="$found"
            log_info "找到版本匹配的 Java: $JAVA_HOME"
            break
        fi
    done
    
    # 2. 如果没有直接匹配，检查子目录 (有些发行版在 jre 子目录下)
    if [ -z "$JAVA_HOME" ] || [ ! -x "$JAVA_HOME/bin/java" ]; then
        for pattern in "${version_patterns[@]}"; do
            local found=$(find /usr/lib/jvm -maxdepth 1 -type d -name "$pattern" 2>/dev/null | sort -V | tail -1)
            if [ -n "$found" ]; then
                # 检查 jre 子目录
                if [ -x "$found/jre/bin/java" ]; then
                    JAVA_HOME="$found/jre"
                    log_info "找到版本匹配的 Java JRE: $JAVA_HOME"
                    break
                fi
            fi
        done
    fi
    
    # 3. 使用 alternatives 设置默认 Java（确保新安装的版本被使用）
    if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
        log_info "设置 Java ${JAVA_VERSION} 为系统默认..."
        if command -v alternatives &> /dev/null; then
            sudo alternatives --set java "$JAVA_HOME/bin/java" 2>/dev/null || true
        fi
    fi
    
    # 4. 最后备选：使用 which java
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
    
    # 配置 SSH 免密登录
    sudo -u $HADOOP_USER mkdir -p /home/$HADOOP_USER/.ssh
    sudo chmod 700 /home/$HADOOP_USER/.ssh
    
    if [ ! -f /home/$HADOOP_USER/.ssh/id_rsa ]; then
        sudo -u $HADOOP_USER ssh-keygen -t rsa -N "" -f /home/$HADOOP_USER/.ssh/id_rsa
        log_success "SSH 密钥生成完成"
    fi
    
    # 配置 authorized_keys
    sudo -u $HADOOP_USER cat /home/$HADOOP_USER/.ssh/id_rsa.pub | sudo -u $HADOOP_USER tee /home/$HADOOP_USER/.ssh/authorized_keys > /dev/null
    sudo chmod 600 /home/$HADOOP_USER/.ssh/authorized_keys
    sudo chown $HADOOP_USER:$HADOOP_USER /home/$HADOOP_USER/.ssh/authorized_keys
    
    # 添加所有可能的主机名到 known_hosts 以避免 SSH 首次连接提示
    log_info "配置 SSH known_hosts..."
    local known_hosts_file="/home/$HADOOP_USER/.ssh/known_hosts"
    
    # 获取当前主机的 IP 地址
    local current_ip=$(hostname -I 2>/dev/null | awk '{print $1}')
    
    # 清空并重新配置 known_hosts
    sudo rm -f "$known_hosts_file" 2>/dev/null || true
    sudo touch "$known_hosts_file"
    sudo chown $HADOOP_USER:$HADOOP_USER "$known_hosts_file"
    sudo chmod 600 "$known_hosts_file"
    
    # 使用 StrictHostKeyChecking=no 配置
    local ssh_config="/home/$HADOOP_USER/.ssh/config"
    sudo bash -c "cat > $ssh_config << 'SSHCONF'
Host *
    StrictHostKeyChecking no
    UserKnownHostsFile /dev/null
    LogLevel ERROR
SSHCONF"
    sudo chown $HADOOP_USER:$HADOOP_USER "$ssh_config"
    sudo chmod 600 "$ssh_config"
    
    # 添加主机到 known_hosts（备用方案）
    for host in localhost 127.0.0.1 0.0.0.0 $(hostname) $(hostname -f 2>/dev/null) $current_ip $MASTER_HOST; do
        if [ -n "$host" ]; then
            ssh-keyscan -H "$host" 2>/dev/null | sudo tee -a "$known_hosts_file" > /dev/null
        fi
    done
    
    log_success "SSH 免密登录配置完成"
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
        sudo mkdir -p "$dir/namenode"
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
    echo "export HDFS_NAMENODE_USER=$HADOOP_USER" | sudo tee -a $HADOOP_CONF_DIR/hadoop-env.sh
    echo "export HDFS_DATANODE_USER=$HADOOP_USER" | sudo tee -a $HADOOP_CONF_DIR/hadoop-env.sh
    echo "export HDFS_SECONDARYNAMENODE_USER=$HADOOP_USER" | sudo tee -a $HADOOP_CONF_DIR/hadoop-env.sh
    echo "export YARN_RESOURCEMANAGER_USER=$HADOOP_USER" | sudo tee -a $HADOOP_CONF_DIR/hadoop-env.sh
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
    <property>
        <name>io.file.buffer.size</name>
        <value>131072</value>
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
    
    # 获取第一个数据目录作为示例
    IFS=',' read -ra DIRS <<< "$HDFS_DATA_DIRS"
    NAMENODE_DIR="${DIRS[0]}/namenode"
    DATANODE_DIR="${DIRS[0]}/datanode"
    
    cat > $HADOOP_CONF_DIR/hdfs-site.xml << EOF
<?xml version="1.0" encoding="UTF-8"?>
<?xml-stylesheet type="text/xsl" href="configuration.xsl"?>
<configuration>
    <property>
        <name>dfs.replication</name>
        <value>$HDFS_REPLICATION</value>
    </property>
    <property>
        <name>dfs.blocksize</name>
        <value>$HDFS_BLOCK_SIZE</value>
    </property>
    <property>
        <name>dfs.namenode.name.dir</name>
        <value>file://$NAMENODE_DIR</value>
    </property>
    <property>
        <name>dfs.datanode.data.dir</name>
        <value>file://$DATANODE_DIR</value>
    </property>
    <!-- NameNode RPC 地址（客户端使用公网IP连接） -->
    <property>
        <name>dfs.namenode.rpc-address</name>
        <value>$MASTER_HOST:$NAMENODE_PORT</value>
    </property>
    <!-- NameNode RPC 绑定到所有网卡（解决公网IP无法绑定问题） -->
    <property>
        <name>dfs.namenode.rpc-bind-host</name>
        <value>0.0.0.0</value>
    </property>
    <!-- NameNode 服务绑定到所有网卡 -->
    <property>
        <name>dfs.namenode.servicerpc-bind-host</name>
        <value>0.0.0.0</value>
    </property>
    <!-- NameNode HTTP 绑定到所有网卡 -->
    <property>
        <name>dfs.namenode.http-bind-host</name>
        <value>0.0.0.0</value>
    </property>
    <property>
        <name>dfs.namenode.http-address</name>
        <value>$MASTER_HOST:$NAMENODE_HTTP_PORT</value>
    </property>
    <property>
        <name>dfs.namenode.secondary.http-address</name>
        <value>$MASTER_HOST:$SECONDARY_NAMENODE_HTTP_PORT</value>
    </property>
    <property>
        <name>dfs.datanode.address</name>
        <value>0.0.0.0:$DATANODE_PORT</value>
    </property>
    <property>
        <name>dfs.webhdfs.enabled</name>
        <value>true</value>
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
    <!-- ResourceManager 绑定到所有网卡 -->
    <property>
        <name>yarn.resourcemanager.bind-host</name>
        <value>0.0.0.0</value>
    </property>
    <property>
        <name>yarn.resourcemanager.address</name>
        <value>$MASTER_HOST:$RESOURCEMANAGER_PORT</value>
    </property>
    <property>
        <name>yarn.resourcemanager.webapp.address</name>
        <value>0.0.0.0:$RESOURCEMANAGER_WEB_PORT</value>
    </property>
    <property>
        <name>yarn.nodemanager.aux-services</name>
        <value>mapreduce_shuffle</value>
    </property>
    <property>
        <name>yarn.nodemanager.aux-services.mapreduce_shuffle.class</name>
        <value>org.apache.hadoop.mapred.ShuffleHandler</value>
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
    <!-- NodeManager 绑定到所有网卡 -->
    <property>
        <name>yarn.nodemanager.bind-host</name>
        <value>0.0.0.0</value>
    </property>
    <property>
        <name>yarn.scheduler.minimum-allocation-mb</name>
        <value>1024</value>
    </property>
    <property>
        <name>yarn.scheduler.maximum-allocation-mb</name>
        <value>$YARN_MEMORY</value>
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
    <property>
        <name>yarn.app.mapreduce.am.env</name>
        <value>HADOOP_MAPRED_HOME=$HADOOP_HOME</value>
    </property>
    <property>
        <name>mapreduce.map.env</name>
        <value>HADOOP_MAPRED_HOME=$HADOOP_HOME</value>
    </property>
    <property>
        <name>mapreduce.reduce.env</name>
        <value>HADOOP_MAPRED_HOME=$HADOOP_HOME</value>
    </property>
    <property>
        <name>mapreduce.jobhistory.address</name>
        <value>$MASTER_HOST:$JOBHISTORY_PORT</value>
    </property>
    <property>
        <name>mapreduce.jobhistory.webapp.address</name>
        <value>$MASTER_HOST:$JOBHISTORY_WEB_PORT</value>
    </property>
</configuration>
EOF
    
    sudo chown $HADOOP_USER:$HADOOP_USER $HADOOP_CONF_DIR/mapred-site.xml
    log_success "mapred-site.xml 配置完成"
}

#===============================================================================
# 配置 workers
#===============================================================================
configure_workers() {
    log_info "配置 workers..."
    
    echo "$MASTER_HOST" > $HADOOP_CONF_DIR/workers
    sudo chown $HADOOP_USER:$HADOOP_USER $HADOOP_CONF_DIR/workers
    
    log_success "workers 配置完成"
}

#===============================================================================
# 格式化 NameNode
#===============================================================================
format_namenode() {
    log_info "格式化 NameNode..."
    
    # 检查是否已格式化
    IFS=',' read -ra DIRS <<< "$HDFS_DATA_DIRS"
    NAMENODE_DIR="${DIRS[0]}/namenode"
    
    if [ -d "$NAMENODE_DIR/current" ]; then
        log_warn "NameNode 已格式化，跳过"
        return
    fi
    
    sudo -u $HADOOP_USER $HADOOP_HOME/bin/hdfs namenode -format -force -nonInteractive
    
    log_success "NameNode 格式化完成"
}

#===============================================================================
# 创建 systemd 服务
#===============================================================================
create_systemd_services() {
    log_info "创建 systemd 服务..."
    
    # HDFS NameNode 服务
    cat > /tmp/hadoop-namenode.service << EOF
[Unit]
Description=Hadoop HDFS NameNode
After=network.target

[Service]
Type=forking
User=$HADOOP_USER
Group=$HADOOP_USER
Environment="JAVA_HOME=$JAVA_HOME"
Environment="HADOOP_HOME=$HADOOP_HOME"
ExecStart=$HADOOP_HOME/bin/hdfs --daemon start namenode
ExecStop=$HADOOP_HOME/bin/hdfs --daemon stop namenode
Restart=on-failure

[Install]
WantedBy=multi-user.target
EOF
    sudo mv /tmp/hadoop-namenode.service /etc/systemd/system/
    
    # YARN ResourceManager 服务
    cat > /tmp/hadoop-resourcemanager.service << EOF
[Unit]
Description=Hadoop YARN ResourceManager
After=network.target hadoop-namenode.service

[Service]
Type=forking
User=$HADOOP_USER
Group=$HADOOP_USER
Environment="JAVA_HOME=$JAVA_HOME"
Environment="HADOOP_HOME=$HADOOP_HOME"
ExecStart=$HADOOP_HOME/bin/yarn --daemon start resourcemanager
ExecStop=$HADOOP_HOME/bin/yarn --daemon stop resourcemanager
Restart=on-failure

[Install]
WantedBy=multi-user.target
EOF
    sudo mv /tmp/hadoop-resourcemanager.service /etc/systemd/system/
    
    sudo systemctl daemon-reload
    
    log_success "systemd 服务创建完成"
}

#===============================================================================
# 启动服务
#===============================================================================
start_services() {
    log_info "启动 Hadoop 服务..."
    
    # 确保环境变量配置文件存在
    source /etc/profile.d/hadoop.sh 2>/dev/null || true
    
    # 先停止可能存在的旧服务（避免进程冲突）
    log_info "停止旧服务（如果存在）..."
    sudo -u $HADOOP_USER -i bash -c "export JAVA_HOME=$JAVA_HOME && export HADOOP_HOME=$HADOOP_HOME && $HADOOP_HOME/sbin/stop-all.sh" 2>/dev/null || true
    sleep 3
    
    # 清理可能残留的 PID 文件
    sudo rm -f /tmp/hadoop-${HADOOP_USER}*.pid 2>/dev/null || true
    
    # 使用 -i 选项确保加载 hadoop 用户的完整环境并传递 JAVA_HOME
    log_info "启动 HDFS..."
    sudo -u $HADOOP_USER -i bash -c "export JAVA_HOME=$JAVA_HOME && export HADOOP_HOME=$HADOOP_HOME && export PATH=\$HADOOP_HOME/bin:\$HADOOP_HOME/sbin:\$PATH && $HADOOP_HOME/sbin/start-dfs.sh"
    
    # 等待 HDFS 启动
    sleep 5
    
    log_info "启动 YARN..."
    sudo -u $HADOOP_USER -i bash -c "export JAVA_HOME=$JAVA_HOME && export HADOOP_HOME=$HADOOP_HOME && export PATH=\$HADOOP_HOME/bin:\$HADOOP_HOME/sbin:\$PATH && $HADOOP_HOME/sbin/start-yarn.sh"
    
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
    
    # 检查 HDFS
    log_info "检查 HDFS..."
    sudo -u $HADOOP_USER $HADOOP_HOME/bin/hdfs dfsadmin -report | head -10
    
    log_success "安装验证完成"
}

#===============================================================================
# 生成报告
#===============================================================================
generate_report() {
    echo ""
    echo -e "${GREEN}${BOLD}╔══════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}${BOLD}║             Hadoop Master 部署完成                                ║${NC}"
    echo -e "${GREEN}${BOLD}╚══════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${BOLD}Hadoop 信息:${NC}"
    echo -e "  版本:            $HADOOP_VERSION"
    echo -e "  Java版本:        $JAVA_VERSION"
    echo -e "  JAVA_HOME:       $JAVA_HOME"
    echo -e "  安装目录:        $HADOOP_HOME"
    echo -e "  配置目录:        $HADOOP_CONF_DIR"
    echo ""
    echo -e "${BOLD}HDFS 信息:${NC}"
    echo -e "  NameNode:        hdfs://$MASTER_HOST:$NAMENODE_PORT"
    echo -e "  NameNode Web UI: http://$MASTER_HOST:$NAMENODE_HTTP_PORT"
    echo -e "  数据目录:        $HDFS_DATA_DIRS"
    echo -e "  副本数:          $HDFS_REPLICATION"
    echo ""
    echo -e "${BOLD}YARN 信息:${NC}"
    echo -e "  ResourceManager: $MASTER_HOST:$RESOURCEMANAGER_PORT"
    echo -e "  Web UI:          http://$MASTER_HOST:$RESOURCEMANAGER_WEB_PORT"
    echo -e "  内存:            ${YARN_MEMORY}MB"
    echo -e "  CPU:             ${YARN_CPU} 核"
    echo ""
    echo -e "${BOLD}MapReduce JobHistory:${NC}"
    echo -e "  Server:          $MASTER_HOST:$JOBHISTORY_PORT"
    echo -e "  Web UI:          http://$MASTER_HOST:$JOBHISTORY_WEB_PORT"
    echo ""
    echo -e "${BOLD}常用命令:${NC}"
    echo -e "  启动 HDFS:       start-dfs.sh"
    echo -e "  停止 HDFS:       stop-dfs.sh"
    echo -e "  启动 YARN:       start-yarn.sh"
    echo -e "  停止 YARN:       stop-yarn.sh"
    echo -e "  查看进程:        jps"
    echo -e "  HDFS 报告:       hdfs dfsadmin -report"
    echo ""
}

#===============================================================================
# 主函数
#===============================================================================
main() {
    log_info "开始部署 Hadoop Master..."
    log_info "Hadoop 版本: $HADOOP_VERSION"
    log_info "Java 版本: $JAVA_VERSION"
    log_info "部署模式: $DEPLOY_MODE"
    
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
    configure_workers
    format_namenode
    create_systemd_services
    start_services
    verify_installation
    generate_report
    
    log_success "Hadoop Master 部署完成!"
}

# 执行主函数
main "$@"
