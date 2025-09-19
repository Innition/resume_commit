// 日志管理页面JavaScript

let currentSystemPage = 1;
let currentAccessPage = 1;
let currentSecurityPage = 1;
let currentErrorPage = 1;
const pageSize = 20;

// 获取认证头
function getAuthHeaders() {
    const token = localStorage.getItem('token');
    return {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
    };
}

// 检查用户权限
async function checkUserPermission() {
    const token = localStorage.getItem('token');
    if (!token) {
        showPermissionError('请先登录');
        return;
    }
    
    try {
        // 验证token并获取用户信息 - 使用日志统计API来验证权限
        const response = await fetch('/logs/statistics', {
            headers: getAuthHeaders()
        });
        
        if (!response.ok) {
            if (response.status === 401) {
                showPermissionError('登录已过期，请重新登录');
                return;
            } else if (response.status === 403) {
                showPermissionError('您没有权限访问日志管理页面');
                return;
            }
        }
        
        // 如果能成功访问日志统计API，说明用户有ROOT权限
        // 权限检查通过，显示页面内容并加载数据
        document.body.style.display = 'block';
        loadStatistics();
        loadSystemLogs();
    } catch (error) {
        console.error('权限检查失败:', error);
        showPermissionError('权限检查失败，请重新登录');
    }
}

// 显示权限错误
function showPermissionError(message) {
    document.body.innerHTML = `
        <div class="container mt-5">
            <div class="row justify-content-center">
                <div class="col-md-6">
                    <div class="alert alert-danger text-center">
                        <h4>访问被拒绝</h4>
                        <p>${message}</p>
                        <a href="/index.html" class="btn btn-primary">返回首页</a>
                    </div>
                </div>
            </div>
        </div>
    `;
}

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', function() {
    // 先隐藏页面内容
    document.body.style.display = 'none';
    
    // 检查用户权限
    checkUserPermission();
    
    // 设置默认时间范围（最近7天）
    const endTime = new Date();
    const startTime = new Date();
    startTime.setDate(startTime.getDate() - 7);
    
    // 安全地设置时间值，检查元素是否存在
    const timeElements = [
        'systemStartTime', 'systemEndTime',
        'accessStartTime', 'accessEndTime', 
        'securityStartTime', 'securityEndTime',
        'errorStartTime', 'errorEndTime'
    ];
    
    timeElements.forEach((elementId, index) => {
        const element = document.getElementById(elementId);
        if (element) {
            element.value = formatDateTimeLocal(index % 2 === 0 ? startTime : endTime);
        }
    });
    
    // 添加选项卡切换事件监听
    setTimeout(() => {
        document.querySelectorAll('[data-bs-toggle="tab"]').forEach(tab => {
            tab.addEventListener('shown.bs.tab', function(event) {
                const target = event.target.getAttribute('data-bs-target');
                if (target === '#security') {
                    // 安全日志选项卡激活时自动查询
                    loadSecurityLogs(1);
                } else if (target === '#error') {
                    // 错误日志选项卡激活时自动查询
                    loadErrorLogs(1);
                }
            });
        });
    }, 100);
});

// 加载统计信息
async function loadStatistics() {
    try {
        const response = await fetch('/logs/statistics', {
            headers: getAuthHeaders()
        });
        const data = await response.json();
        
        document.getElementById('systemLogCount').textContent = data.systemLogCount || 0;
        document.getElementById('accessLogCount').textContent = data.accessLogCount || 0;
        document.getElementById('securityLogCount').textContent = data.securityLogCount || 0;
        document.getElementById('errorLogCount').textContent = data.errorLogCount || 0;
    } catch (error) {
        console.error('加载统计信息失败:', error);
    }
}

// 加载系统日志
async function loadSystemLogs(page = 1) {
    try {
        const params = new URLSearchParams({
            page: page,
            size: document.getElementById('systemPageSize').value || pageSize
        });
        
        const level = document.getElementById('systemLevel').value;
        const module = document.getElementById('systemModule').value;
        const startTime = document.getElementById('systemStartTime').value;
        const endTime = document.getElementById('systemEndTime').value;
        
        if (level) params.append('level', level);
        if (module) params.append('module', module);
        if (startTime) params.append('startTime', startTime);
        if (endTime) params.append('endTime', endTime);
        
        const response = await fetch(`/logs/system?${params}`, {
            headers: getAuthHeaders()
        });
        const data = await response.json();
        
        renderSystemLogs(data.records);
        renderPagination('systemPagination', data, page, loadSystemLogs);
        currentSystemPage = page;
    } catch (error) {
        console.error('加载系统日志失败:', error);
        showAlert('加载系统日志失败', 'danger');
    }
}

// 渲染系统日志
function renderSystemLogs(logs) {
    const tbody = document.getElementById('systemLogTableBody');
    tbody.innerHTML = '';
    
    if (!logs || logs.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" class="text-center">暂无数据</td></tr>';
        return;
    }
    
    logs.forEach(log => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${formatDateTime(log.createdAt)}</td>
            <td><span class="log-level-${log.logLevel}">${log.logLevel}</span></td>
            <td>${log.module || '-'}</td>
            <td>${log.operation || '-'}</td>
            <td>${log.username || '-'}</td>
            <td>${log.ipAddress || '-'}</td>
            <td class="log-details">${log.message || '-'}</td>
            <td>
                <button class="btn btn-sm btn-outline-primary" onclick="showLogDetail(${JSON.stringify(log).replace(/"/g, '&quot;')})">
                    <i class="bi bi-eye"></i> 详情
                </button>
            </td>
        `;
        tbody.appendChild(row);
    });
}

// 加载访问日志
async function loadAccessLogs(page = 1) {
    try {
        const params = new URLSearchParams({
            page: page,
            size: pageSize
        });
        
        const username = document.getElementById('accessUsername').value;
        const ipAddress = document.getElementById('accessIpAddress').value;
        const method = document.getElementById('accessMethod').value;
        const statusCode = document.getElementById('accessStatusCode').value;
        const startTime = document.getElementById('accessStartTime').value;
        const endTime = document.getElementById('accessEndTime').value;
        
        if (username) params.append('username', username);
        if (ipAddress) params.append('ipAddress', ipAddress);
        if (method) params.append('requestMethod', method);
        if (statusCode) params.append('responseCode', statusCode);
        if (startTime) params.append('startTime', startTime);
        if (endTime) params.append('endTime', endTime);
        
        const response = await fetch(`/logs/access?${params}`, {
            headers: getAuthHeaders()
        });
        const data = await response.json();
        
        renderAccessLogs(data.records);
        renderPagination('accessPagination', data, page, loadAccessLogs);
        currentAccessPage = page;
    } catch (error) {
        console.error('加载访问日志失败:', error);
        showAlert('加载访问日志失败', 'danger');
    }
}

// 渲染访问日志
function renderAccessLogs(logs) {
    const tbody = document.getElementById('accessLogTableBody');
    tbody.innerHTML = '';
    
    if (!logs || logs.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" class="text-center">暂无数据</td></tr>';
        return;
    }
    
    logs.forEach(log => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${formatDateTime(log.createdAt)}</td>
            <td>${log.username || '-'}</td>
            <td>${log.ipAddress || '-'}</td>
            <td><span class="badge bg-${getMethodColor(log.requestMethod)}">${log.requestMethod || '-'}</span></td>
            <td class="log-details">${log.requestUrl || '-'}</td>
            <td><span class="badge bg-${getStatusCodeColor(log.responseCode)}">${log.responseCode || '-'}</span></td>
            <td>${log.responseTime ? log.responseTime + 'ms' : '-'}</td>
            <td class="log-details">${log.userAgent || '-'}</td>
        `;
        tbody.appendChild(row);
    });
}

// 加载安全日志
async function loadSecurityLogs(page = 1) {
    try {
        const params = new URLSearchParams({
            page: page,
            size: pageSize
        });
        
        const logType = document.getElementById('securityLogType').value;
        const riskLevel = document.getElementById('securityRiskLevel').value;
        const username = document.getElementById('securityUsername').value;
        const startTime = document.getElementById('securityStartTime').value;
        const endTime = document.getElementById('securityEndTime').value;
        
        if (logType) params.append('logType', logType);
        if (riskLevel) params.append('riskLevel', riskLevel);
        if (username) params.append('username', username);
        if (startTime) params.append('startTime', startTime);
        if (endTime) params.append('endTime', endTime);
        
        const response = await fetch(`/logs/security?${params}`, {
            headers: getAuthHeaders()
        });
        const data = await response.json();
        
        renderSecurityLogs(data.records);
        renderPagination('securityPagination', data, page, loadSecurityLogs);
        currentSecurityPage = page;
    } catch (error) {
        console.error('加载安全日志失败:', error);
        showAlert('加载安全日志失败', 'danger');
    }
}

// 渲染安全日志
function renderSecurityLogs(logs) {
    const tbody = document.getElementById('securityLogTableBody');
    tbody.innerHTML = '';
    
    if (!logs || logs.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center">暂无数据</td></tr>';
        return;
    }
    
    logs.forEach(log => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${formatDateTime(log.createdAt)}</td>
            <td>${log.logType || '-'}</td>
            <td>${log.username || '-'}</td>
            <td>${log.ipAddress || '-'}</td>
            <td class="log-details">${log.eventDescription || '-'}</td>
            <td><span class="risk-level-${log.riskLevel}">${log.riskLevel || '-'}</span></td>
            <td>
                <button class="btn btn-sm btn-outline-primary" onclick="showLogDetail(${JSON.stringify(log).replace(/"/g, '&quot;')})">
                    <i class="bi bi-eye"></i> 详情
                </button>
            </td>
        `;
        tbody.appendChild(row);
    });
}

// 渲染分页
function renderPagination(containerId, data, currentPage, loadFunction) {
    const container = document.getElementById(containerId);
    container.innerHTML = '';
    
    if (data.pages <= 1) return;
    
    const totalPages = data.pages;
    const startPage = Math.max(1, currentPage - 2);
    const endPage = Math.min(totalPages, currentPage + 2);
    
    // 上一页
    if (currentPage > 1) {
        const prevLi = document.createElement('li');
        prevLi.className = 'page-item';
        prevLi.innerHTML = `<a class="page-link" href="#" onclick="loadFunction(${currentPage - 1}); return false;">上一页</a>`;
        container.appendChild(prevLi);
    }
    
    // 页码
    for (let i = startPage; i <= endPage; i++) {
        const li = document.createElement('li');
        li.className = `page-item ${i === currentPage ? 'active' : ''}`;
        li.innerHTML = `<a class="page-link" href="#" onclick="loadFunction(${i}); return false;">${i}</a>`;
        container.appendChild(li);
    }
    
    // 下一页
    if (currentPage < totalPages) {
        const nextLi = document.createElement('li');
        nextLi.className = 'page-item';
        nextLi.innerHTML = `<a class="page-link" href="#" onclick="loadFunction(${currentPage + 1}); return false;">下一页</a>`;
        container.appendChild(nextLi);
    }
}

// 显示日志详情
function showLogDetail(log) {
    const content = document.getElementById('logDetailContent');
    content.textContent = JSON.stringify(log, null, 2);
    
    const modal = new bootstrap.Modal(document.getElementById('logDetailModal'));
    modal.show();
}

// 清空系统日志筛选条件
function clearSystemFilters() {
    document.getElementById('systemLevel').value = '';
    document.getElementById('systemModule').value = '';
    document.getElementById('systemStartTime').value = '';
    document.getElementById('systemEndTime').value = '';
    loadSystemLogs(1);
}

// 清空访问日志筛选条件
function clearAccessFilters() {
    document.getElementById('accessUsername').value = '';
    document.getElementById('accessIpAddress').value = '';
    document.getElementById('accessMethod').value = '';
    document.getElementById('accessStatusCode').value = '';
    document.getElementById('accessStartTime').value = '';
    document.getElementById('accessEndTime').value = '';
    loadAccessLogs(1);
}

// 清空安全日志筛选条件
function clearSecurityFilters() {
    document.getElementById('securityLogType').value = '';
    document.getElementById('securityRiskLevel').value = '';
    document.getElementById('securityUsername').value = '';
    document.getElementById('securityStartTime').value = '';
    document.getElementById('securityEndTime').value = '';
    loadSecurityLogs(1);
}

// 加载错误日志
async function loadErrorLogs(page = 1) {
    try {
        const params = new URLSearchParams({
            page: page,
            size: pageSize
        });
        
        const module = document.getElementById('errorModule').value;
        const operation = document.getElementById('errorOperation').value;
        const username = document.getElementById('errorUsername').value;
        const startTime = document.getElementById('errorStartTime').value;
        const endTime = document.getElementById('errorEndTime').value;
        
        if (module) params.append('module', module);
        if (operation) params.append('operation', operation);
        if (username) params.append('username', username);
        if (startTime) params.append('startTime', startTime);
        if (endTime) params.append('endTime', endTime);
        
        const response = await fetch(`/logs/error?${params}`, {
            headers: getAuthHeaders()
        });
        const data = await response.json();
        
        renderErrorLogs(data.records);
        renderPagination('errorPagination', data, page, loadErrorLogs);
        currentErrorPage = page;
    } catch (error) {
        console.error('加载错误日志失败:', error);
        showAlert('加载错误日志失败', 'danger');
    }
}

// 渲染错误日志
function renderErrorLogs(logs) {
    const tbody = document.getElementById('errorLogTableBody');
    tbody.innerHTML = '';
    
    if (!logs || logs.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center">暂无数据</td></tr>';
        return;
    }
    
    logs.forEach(log => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${formatDateTime(log.createdAt)}</td>
            <td>${log.module || '-'}</td>
            <td>${log.operation || '-'}</td>
            <td>${log.username || '-'}</td>
            <td>${log.ipAddress || '-'}</td>
            <td class="log-details">${log.errorMessage || '-'}</td>
            <td>
                <button class="btn btn-sm btn-outline-primary" onclick="showLogDetail(${JSON.stringify(log).replace(/"/g, '&quot;')})">
                    <i class="bi bi-eye"></i> 详情
                </button>
            </td>
        `;
        tbody.appendChild(row);
    });
}

// 清空错误日志筛选条件
function clearErrorFilters() {
    document.getElementById('errorModule').value = '';
    document.getElementById('errorOperation').value = '';
    document.getElementById('errorUsername').value = '';
    document.getElementById('errorStartTime').value = '';
    document.getElementById('errorEndTime').value = '';
    loadErrorLogs(1);
}

// 工具函数
function formatDateTime(dateTimeStr) {
    if (!dateTimeStr) return '-';
    const date = new Date(dateTimeStr);
    return date.toLocaleString('zh-CN');
}

function formatDateTimeLocal(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day}T${hours}:${minutes}`;
}

function getMethodColor(method) {
    switch (method) {
        case 'GET': return 'success';
        case 'POST': return 'primary';
        case 'PUT': return 'warning';
        case 'DELETE': return 'danger';
        default: return 'secondary';
    }
}

function getStatusCodeColor(code) {
    if (code >= 200 && code < 300) return 'success';
    if (code >= 300 && code < 400) return 'info';
    if (code >= 400 && code < 500) return 'warning';
    if (code >= 500) return 'danger';
    return 'secondary';
}

function showAlert(message, type = 'info') {
    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${type} alert-dismissible fade show position-fixed`;
    alertDiv.style.top = '20px';
    alertDiv.style.right = '20px';
    alertDiv.style.zIndex = '9999';
    alertDiv.innerHTML = `
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    document.body.appendChild(alertDiv);
    
    setTimeout(() => {
        if (alertDiv.parentNode) {
            alertDiv.parentNode.removeChild(alertDiv);
        }
    }, 5000);
}
