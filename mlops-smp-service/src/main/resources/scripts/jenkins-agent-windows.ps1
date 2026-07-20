#===============================================================================
# Jenkins Agent 自动部署脚本 - Windows (PowerShell)
# 版本: 1.0.0
# 用途: 一键部署 Jenkins Agent 到 Windows 系统
# 使用方法: 以管理员身份运行 PowerShell
#===============================================================================

# 配置参数（由Java后端替换）
$JENKINS_URL = "${JENKINS_URL}"
$JENKINS_AGENT_NAME = "${JENKINS_AGENT_NAME}"
$JENKINS_WORK_DIR = "${JENKINS_WORK_DIR}"
$JAVA_VERSION = "${JAVA_VERSION}"
$PYTHON_VERSION = "${PYTHON_VERSION}"
$AGENT_VERSION = "${AGENT_VERSION}"
$JENKINS_SECRET = "${JENKINS_SECRET}"
$LABELS = "${LABELS}"

# 额外配置
$INSTALL_DOCKER = "${INSTALL_DOCKER}"
$INSTALL_GIT = "${INSTALL_GIT}"
$INSTALL_MAVEN = "${INSTALL_MAVEN}"
$MAVEN_VERSION = "${MAVEN_VERSION}"
$INSTALL_NODE = "${INSTALL_NODE}"
$NODE_VERSION = "${NODE_VERSION}"

# 设置错误处理
$ErrorActionPreference = "Stop"

# 日志函数
function Write-Log {
    param([string]$Level, [string]$Message)
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    Write-Host "[$Level] $timestamp - $Message"
}

function Write-Info { Write-Log "INFO" $args[0] }
function Write-Error { Write-Log "ERROR" $args[0] }
function Write-Success { Write-Log "SUCCESS" $args[0] }

# 检查管理员权限
function Test-Administrator {
    $currentUser = New-Object Security.Principal.WindowsPrincipal([Security.Principal.WindowsIdentity]::GetCurrent())
    return $currentUser.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

# 安装 Chocolatey
function Install-Chocolatey {
    if (!(Get-Command choco -ErrorAction SilentlyContinue)) {
        Write-Info "安装 Chocolatey 包管理器..."
        Set-ExecutionPolicy Bypass -Scope Process -Force
        [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072
        Invoke-Expression ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))

        # 刷新环境变量
        $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
    }
    Write-Success "Chocolatey 已就绪"
}

# 安装 Java
function Install-Java {
    Write-Info "安装 Java $JAVA_VERSION..."

    switch ($JAVA_VERSION) {
        "8" { choco install temurin8 -y --no-progress }
        "11" { choco install temurin11 -y --no-progress }
        "17" { choco install temurin17 -y --no-progress }
        "21" { choco install temurin21 -y --no-progress }
        default { choco install temurin11 -y --no-progress }
    }

    # 刷新环境变量
    $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")

    # 验证安装
    java -version
    Write-Success "Java 安装完成"
}

# 安装 Python
function Install-Python {
    Write-Info "安装 Python $PYTHON_VERSION..."

    choco install python --version=$PYTHON_VERSION -y --no-progress

    # 刷新环境变量
    $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")

    # 验证安装
    python --version
    pip --version
    Write-Success "Python 安装完成"
}

# 安装 Git
function Install-Git {
    if ($INSTALL_GIT -eq "true") {
        Write-Info "安装 Git..."
        choco install git -y --no-progress

        # 刷新环境变量
        $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")

        git --version
        Write-Success "Git 安装完成"
    }
}

# 安装 Docker Desktop
function Install-Docker {
    if ($INSTALL_DOCKER -eq "true") {
        Write-Info "安装 Docker Desktop..."
        choco install docker-desktop -y --no-progress
        Write-Info "请在安装完成后重启计算机并手动启动 Docker Desktop"
        Write-Success "Docker Desktop 安装完成"
    }
}

# 安装 Maven
function Install-Maven {
    if ($INSTALL_MAVEN -eq "true") {
        Write-Info "安装 Maven $MAVEN_VERSION..."
        choco install maven --version=$MAVEN_VERSION -y --no-progress

        # 刷新环境变量
        $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")

        mvn -version
        Write-Success "Maven 安装完成"
    }
}

# 安装 Node.js
function Install-NodeJS {
    if ($INSTALL_NODE -eq "true") {
        Write-Info "安装 Node.js $NODE_VERSION..."
        choco install nodejs --version=$NODE_VERSION -y --no-progress

        # 刷新环境变量
        $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")

        node --version
        npm --version
        Write-Success "Node.js 安装完成"
    }
}

# 配置 Jenkins Agent
function Setup-JenkinsAgent {
    Write-Info "配置 Jenkins Agent..."

    # 创建工作目录
    if (!(Test-Path $JENKINS_WORK_DIR)) {
        New-Item -ItemType Directory -Path $JENKINS_WORK_DIR -Force | Out-Null
    }

    # 下载 Jenkins Agent JAR
    $agentJarUrl = "$JENKINS_URL/jnlpJars/agent.jar"
    $agentJarPath = Join-Path $JENKINS_WORK_DIR "agent.jar"
    Write-Info "下载 Jenkins Agent JAR: $agentJarUrl"
    Invoke-WebRequest -Uri $agentJarUrl -OutFile $agentJarPath

    # 创建启动脚本
    $startScript = @"
@echo off
setlocal

set JENKINS_URL=$JENKINS_URL
set JENKINS_AGENT_NAME=$JENKINS_AGENT_NAME
set JENKINS_WORK_DIR=$JENKINS_WORK_DIR
set JENKINS_SECRET=$JENKINS_SECRET

cd /d %JENKINS_WORK_DIR%

if defined JENKINS_SECRET (
    java -jar agent.jar -jnlpUrl "%JENKINS_URL%/computer/%JENKINS_AGENT_NAME%/jenkins-agent.jnlp" -secret "%JENKINS_SECRET%" -workDir "%JENKINS_WORK_DIR%"
) else (
    java -jar agent.jar -jnlpUrl "%JENKINS_URL%/computer/%JENKINS_AGENT_NAME%/jenkins-agent.jnlp" -workDir "%JENKINS_WORK_DIR%"
)

endlocal
"@

    $startScriptPath = Join-Path $JENKINS_WORK_DIR "start-agent.bat"
    $startScript | Out-File -FilePath $startScriptPath -Encoding ASCII

    # 创建 Windows 服务安装脚本
    $serviceScript = @"
@echo off
echo Installing Jenkins Agent as Windows Service...

sc create JenkinsAgent binPath= "cmd /c $startScriptPath" start= auto displayname= "Jenkins Agent"
sc description JenkinsAgent "Jenkins Build Agent Service"

echo Service installed successfully.
echo To start the service: sc start JenkinsAgent
echo To stop the service: sc stop JenkinsAgent
"@

    $serviceScriptPath = Join-Path $JENKINS_WORK_DIR "install-service.bat"
    $serviceScript | Out-File -FilePath $serviceScriptPath -Encoding ASCII

    # 使用 NSSM 安装服务（更可靠）
    Write-Info "安装 NSSM 服务管理器..."
    choco install nssm -y --no-progress 2>$null

    # 刷新环境变量
    $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")

    # 使用 NSSM 创建服务
    try {
        nssm stop JenkinsAgent 2>$null
        nssm remove JenkinsAgent confirm 2>$null
    } catch {}

    nssm install JenkinsAgent $startScriptPath
    nssm set JenkinsAgent AppDirectory $JENKINS_WORK_DIR
    nssm set JenkinsAgent DisplayName "Jenkins Agent"
    nssm set JenkinsAgent Description "Jenkins Build Agent Service for $JENKINS_AGENT_NAME"
    nssm set JenkinsAgent Start SERVICE_AUTO_START
    nssm set JenkinsAgent AppStdout (Join-Path $JENKINS_WORK_DIR "jenkins-agent.log")
    nssm set JenkinsAgent AppStderr (Join-Path $JENKINS_WORK_DIR "jenkins-agent-error.log")

    Write-Success "Jenkins Agent 配置完成"
}

# 启动 Jenkins Agent
function Start-JenkinsAgent {
    Write-Info "启动 Jenkins Agent..."

    nssm start JenkinsAgent

    Start-Sleep -Seconds 5

    $service = Get-Service -Name "JenkinsAgent" -ErrorAction SilentlyContinue
    if ($service -and $service.Status -eq "Running") {
        Write-Success "Jenkins Agent 已启动"
    } else {
        Write-Error "Jenkins Agent 启动失败，尝试手动启动..."
        Start-Process -FilePath (Join-Path $JENKINS_WORK_DIR "start-agent.bat") -WindowStyle Hidden
    }
}

# 主函数
function Main {
    Write-Info "=========================================="
    Write-Info "Jenkins Agent 自动部署开始 (Windows)"
    Write-Info "=========================================="
    Write-Info "Jenkins URL: $JENKINS_URL"
    Write-Info "Agent 名称: $JENKINS_AGENT_NAME"
    Write-Info "工作目录: $JENKINS_WORK_DIR"
    Write-Info "Java 版本: $JAVA_VERSION"
    Write-Info "Python 版本: $PYTHON_VERSION"
    Write-Info "=========================================="

    # 检查管理员权限
    if (!(Test-Administrator)) {
        Write-Error "请以管理员身份运行此脚本!"
        exit 1
    }

    # 安装 Chocolatey
    Install-Chocolatey

    # 安装 Java
    Install-Java

    # 安装 Python
    Install-Python

    # 安装可选组件
    Install-Git
    Install-Docker
    Install-Maven
    Install-NodeJS

    # 配置 Jenkins Agent
    Setup-JenkinsAgent

    # 启动 Agent
    Start-JenkinsAgent

    Write-Info "=========================================="
    Write-Success "Jenkins Agent 安装完成!"
    Write-Info "工作目录: $JENKINS_WORK_DIR"
    Write-Info "服务名称: JenkinsAgent"
    Write-Info "管理命令:"
    Write-Info "  启动: nssm start JenkinsAgent"
    Write-Info "  停止: nssm stop JenkinsAgent"
    Write-Info "  状态: nssm status JenkinsAgent"
    Write-Info "  日志: Get-Content $JENKINS_WORK_DIR\jenkins-agent.log -Tail 50"
    Write-Info "=========================================="
}

# 执行主函数
Main
