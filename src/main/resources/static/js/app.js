// 全局变量
let currentUser = null;
let currentRecords = [];
let filteredRecords = [];
let isFiltered = false;
let userPositionPreferences = {}; // 用户岗位显示偏好
let currentViewMode = 'card'; // 当前显示模式：'card' 或 'list'

// 防抖函数
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

// API基础URL
const API_BASE = '';

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', function() {
    checkAuth();

    // 添加搜索框回车事件
    document.getElementById('searchInput').addEventListener('keypress', function(e) {
        if (e.key === 'Enter') {
            applyFilters();
        }
    });

    // 添加筛选条件变化事件
    document.getElementById('finalResultFilter').addEventListener('change', applyFilters);
    document.getElementById('currentStatusFilter').addEventListener('change', applyFilters);
    document.getElementById('minSalaryFilter').addEventListener('input', debounce(applyFilters, 500));

    // 加载用户岗位偏好
    loadUserPositionPreferences();
});

// 检查用户认证状态
function checkAuth() {
    const token = localStorage.getItem('token');
    if (token) {
        // 验证token有效性
        fetch(`${API_BASE}/records`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        })
            .then(response => {
                if (response.ok) {
                    loadUserInfo();
                    loadRecords();
                } else {
                    showLoginModal();
                }
            })
            .catch(() => {
                showLoginModal();
            });
    } else {
        showLoginModal();
    }
}

// 加载用户信息
function loadUserInfo() {
    const token = localStorage.getItem('token');
    try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        currentUser = {
            username: payload.sub,
            role: payload.role
        };

        document.getElementById('userInfo').textContent = `欢迎, ${currentUser.username}`;

        // 如果是ROOT用户，显示生成邀请码按钮和日志管理按钮
        if (currentUser.role === 'ROOT') {
            document.getElementById('inviteCodeBtn').style.display = 'inline-block';
            document.getElementById('logManagementBtn').style.display = 'inline-block';
        }
    } catch (error) {
        console.error('解析token失败:', error);
        // 如果token解析失败，清除token并显示登录框
        localStorage.removeItem('token');
        showLoginModal();
    }
}

// 用户登录
function login() {
    const username = document.getElementById('loginUsername').value;
    const password = document.getElementById('loginPassword').value;

    if (!username || !password) {
        alert('请输入用户名和密码');
        return;
    }

    fetch(`${API_BASE}/auth/login`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ username, password })
    })
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }
            return response.json();
        })
        .then(data => {
            if (data.success) {
                localStorage.setItem('token', data.token);
                bootstrap.Modal.getInstance(document.getElementById('loginModal')).hide();
                // 清空登录表单
                document.getElementById('loginForm').reset();
                // 重新加载用户信息和数据
                loadUserInfo();
                loadRecords();
            } else {
                alert('登录失败: ' + (data.message || '未知错误'));
            }
        })
        .catch(error => {
            console.error('登录错误:', error);
            alert('登录失败: ' + error.message);
        });
}

// 用户注册
function register() {
    const username = document.getElementById('registerUsername').value;
    const password = document.getElementById('registerPassword').value;
    const email = document.getElementById('registerEmail').value;
    const inviteCode = document.getElementById('inviteCode').value;

    fetch(`${API_BASE}/auth/register`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ username, password, email, inviteCode })
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                localStorage.setItem('token', data.token);
                bootstrap.Modal.getInstance(document.getElementById('registerModal')).hide();
                // 清空注册表单
                document.getElementById('registerForm').reset();
                // 重新加载用户信息和数据
                loadUserInfo();
                loadRecords();
            } else {
                alert('注册失败: ' + data.message);
            }
        })
        .catch(error => {
            console.error('注册错误:', error);
            alert('注册失败，请重试');
        });
}

// 用户退出
function logout() {
    localStorage.removeItem('token');
    currentUser = null;
    currentRecords = [];

    // 隐藏主界面内容
    document.getElementById('userInfo').textContent = '';
    document.getElementById('recordsTableBody').innerHTML = '';
    document.getElementById('inviteCodeBtn').style.display = 'none';
    document.getElementById('logManagementBtn').style.display = 'none';

    // 显示登录模态框
    showLoginModal();
}

// 显示登录模态框
function showLoginModal() {
    const modal = new bootstrap.Modal(document.getElementById('loginModal'));
    modal.show();
}

// 显示注册模态框
function showRegisterModal() {
    bootstrap.Modal.getInstance(document.getElementById('loginModal')).hide();
    const modal = new bootstrap.Modal(document.getElementById('registerModal'));
    modal.show();
}

// 加载投递记录
function loadRecords() {
    const token = localStorage.getItem('token');

    fetch(`${API_BASE}/records`, {
        headers: {
            'Authorization': `Bearer ${token}`
        }
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                currentRecords = data.data;
                renderRecords();
            } else {
                console.error('加载记录失败:', data.message);
            }
        })
        .catch(error => {
            console.error('加载记录错误:', error);
        });
}

// 切换显示模式
function switchViewMode(mode) {
    currentViewMode = mode;
    
    // 更新按钮状态
    document.getElementById('cardViewBtn').classList.toggle('active', mode === 'card');
    document.getElementById('listViewBtn').classList.toggle('active', mode === 'list');
    
    // 切换显示区域
    const cardView = document.getElementById('processGrid');
    const listView = document.getElementById('listView');
    
    if (mode === 'card') {
        cardView.style.display = 'flex';
        listView.classList.remove('active');
    } else {
        cardView.style.display = 'none';
        listView.classList.add('active');
    }
    
    // 重新渲染记录
    renderRecords();
}

// 渲染投递记录流程
function renderRecords(records = null) {
    // 使用传入的records参数，如果没有则使用当前筛选结果
    const recordsToRender = records || (isFiltered ? filteredRecords : currentRecords);

    if (currentViewMode === 'card') {
        renderCardView(recordsToRender);
    } else {
        renderListView(recordsToRender);
    }
}

// 渲染卡片视图
function renderCardView(recordsToRender) {
    const container = document.getElementById('processGrid');
    container.innerHTML = '';

    if (!recordsToRender || recordsToRender.length === 0) {
        container.innerHTML = '<div class="col-12 text-center text-muted py-5">暂无记录</div>';
        return;
    }

    recordsToRender.forEach((record, index) => {
        console.log('处理记录:', record.companyName, 'companyGroupId:', record.companyGroupId, 'positions:', record.positions);

        // 应用用户岗位偏好
        if (record.positions && record.positions.length > 0) {
            const preferredPositionId = userPositionPreferences[record.companyGroupId];
            console.log('应用偏好:', record.companyGroupId, preferredPositionId, record.positions);

            let currentPosition = null;

            if (preferredPositionId) {
                // 使用用户偏好的岗位
                currentPosition = record.positions.find(p => p.id == preferredPositionId);
                console.log('找到偏好岗位:', currentPosition);
            }

            if (!currentPosition) {
                // 如果没有偏好或找不到偏好岗位，使用默认岗位（第一个或主要岗位）
                currentPosition = record.currentPosition || record.positions[0];
                console.log('使用默认岗位:', currentPosition);
            }

            // 确保currentPosition不为null
            if (currentPosition) {
                record.currentPosition = currentPosition;
                // 更新记录的基本信息为当前岗位的信息
                record.position = currentPosition.position;
                record.finalResult = currentPosition.finalResult;
                record.currentStatus = currentPosition.currentStatus;
                record.currentStatusDate = currentPosition.currentStatusDate;
                record.expectedSalaryType = currentPosition.expectedSalaryType;
                record.expectedSalaryValue = currentPosition.expectedSalaryValue;
                record.remarks = currentPosition.remarks;
                record.interviews = currentPosition.interviews;
                record.poolDays = currentPosition.poolDays;
                console.log('更新后的记录信息:', record.position, record.finalResult);
            } else {
                // 如果仍然没有currentPosition，使用第一个岗位作为默认值
                console.warn('没有找到有效的岗位，使用第一个岗位作为默认值');
                const firstPosition = record.positions[0];
                if (firstPosition) {
                    record.currentPosition = firstPosition;
                    record.position = firstPosition.position;
                    record.finalResult = firstPosition.finalResult;
                    record.currentStatus = firstPosition.currentStatus;
                    record.currentStatusDate = firstPosition.currentStatusDate;
                    record.expectedSalaryType = firstPosition.expectedSalaryType;
                    record.expectedSalaryValue = firstPosition.expectedSalaryValue;
                    record.remarks = firstPosition.remarks;
                    record.interviews = firstPosition.interviews;
                    record.poolDays = firstPosition.poolDays;
                }
            }
        } else {
            // 单岗位记录，确保有currentPosition
            if (!record.currentPosition) {
                record.currentPosition = record;
            }
        }

        const processItem = document.createElement('div');
        processItem.className = 'process-item';

        // 最终结果样式
        let resultClass = '';
        switch(record.finalResult) {
            case 'PENDING': resultClass = 'status-pending'; break;
            case 'OC': resultClass = 'status-oc'; break;
            case '简历挂':
            case '测评挂':
            case '笔试挂':
            case '面试挂': resultClass = 'status-rejected'; break;
        }

        // 生成流程时间线 - 使用当前岗位的所有信息
        const timelineData = {
            ...record, // 基础信息（companyName, baseLocation等）
            // 从currentPosition获取所有岗位相关信息
            position: record.currentPosition ? record.currentPosition.position : record.position,
            finalResult: record.currentPosition ? record.currentPosition.finalResult : record.finalResult,
            currentStatus: record.currentPosition ? record.currentPosition.currentStatus : record.currentStatus,
            currentStatusDate: record.currentPosition ? record.currentPosition.currentStatusDate : record.currentStatusDate,
            // 从currentPosition获取流程时间信息
            applyTime: record.currentPosition ? record.currentPosition.applyTime : record.applyTime,
            testTime: record.currentPosition ? record.currentPosition.testTime : record.testTime,
            writtenExamTime: record.currentPosition ? record.currentPosition.writtenExamTime : record.writtenExamTime,
            interviews: record.currentPosition ? record.currentPosition.interviews : record.interviews
        };
        const timeline = generateTimeline(timelineData);

        // 生成公司名称（如果有URL则作为超链接）
        const companyNameHtml = record.companyUrl
            ? `<a href="${record.companyUrl}" target="_blank" class="company-link">${record.companyName}</a>`
            : record.companyName;

        // 生成岗位选择器（下拉框形式）
        let positionSelector = '';
        console.log('生成岗位选择器:', record.companyName, 'positions length:', record.positions ? record.positions.length : 0);

        if (record.positions && record.positions.length > 1) {
            console.log('创建多岗位选择器，岗位列表:', record.positions);
            positionSelector = `
                <div class="process-info-item">
                    <span class="process-info-label">岗位</span>
                    <select class="form-select form-select-sm position-dropdown" 
                            onchange="switchPosition('${record.companyGroupId}', this.value)"
                            style="display: inline-block; width: auto; min-width: 120px;">
                        ${record.positions.map(pos => `
                            <option value="${pos.id}" ${pos.id == record.currentPosition?.id ? 'selected' : ''}>
                                ${pos.position}
                            </option>
                        `).join('')}
                    </select>
                </div>
            `;
        } else if (record.positions && record.positions.length === 1) {
            // 单岗位记录
            positionSelector = `
                <div class="process-info-item">
                    <span class="process-info-label">岗位</span>
                    <span class="process-info-value">${record.position}</span>
                </div>
            `;
        }

        processItem.innerHTML = `
            <div class="process-content">
                <div class="process-header">
                    <h6 class="process-title">${companyNameHtml}</h6>
                    <span class="process-status ${resultClass}">${record.finalResult}</span>
                </div>
                
                <div class="process-info">
                    ${positionSelector}
                    <div class="process-info-item">
                        <span class="process-info-label">地点</span>
                        <span class="process-info-value">${record.baseLocation || '-'}</span>
                    </div>
                    <div class="process-info-item">
                        <span class="process-info-label">泡池时间</span>
                        <span class="process-info-value">${record.poolDays}天</span>
                    </div>
                    ${record.finalResult === 'OC' && record.expectedSalaryType && record.expectedSalaryValue ? `
                    <div class="process-info-item">
                        <span class="process-info-label">预期薪资</span>
                        <span class="process-info-value salary-info">${formatSalary(record.expectedSalaryType, record.expectedSalaryValue)}</span>
                    </div>
                    ` : ''}
                </div>
                
                <div class="process-timeline">
                    <div class="timeline-title">流程进度</div>
                    <div class="timeline-steps">
                        ${timeline}
                    </div>
                </div>
            </div>
            
            <div class="process-actions">
                <button class="btn btn-outline-primary btn-sm" onclick="editRecord(${record.currentPosition ? record.currentPosition.id : (record.positions && record.positions.length > 0 ? record.positions[0].id : record.id)})">
                    <i class="bi bi-pencil"></i> 编辑
                </button>
                <button class="btn btn-outline-danger btn-sm" onclick="deleteRecord(${record.currentPosition ? record.currentPosition.id : (record.positions && record.positions.length > 0 ? record.positions[0].id : record.id)})">
                    <i class="bi bi-trash"></i> 删除
                </button>
            </div>
            
            ${record.remarks ? `
            <div class="remarks-tooltip" id="remarks-${record.id}">
                <div class="remarks-content">
                    <strong>备注：</strong>${record.remarks}
                </div>
            </div>
            ` : ''}
        `;

        // 添加鼠标悬浮事件监听
        if (record.remarks) {
            let hoverTimer;

            processItem.addEventListener('mouseenter', function() {
                hoverTimer = setTimeout(() => {
                    const tooltip = document.getElementById(`remarks-${record.id}`);
                    if (tooltip) {
                        tooltip.style.display = 'block';
                    }
                }, 250); // 1秒后显示
            });

            processItem.addEventListener('mouseleave', function() {
                clearTimeout(hoverTimer);
                const tooltip = document.getElementById(`remarks-${record.id}`);
                if (tooltip) {
                    tooltip.style.display = 'none';
                }
            });
        }

        container.appendChild(processItem);
    });
}

// 渲染列表视图
function renderListView(recordsToRender) {
    const tbody = document.getElementById('recordsTableBody');
    tbody.innerHTML = '';

    if (!recordsToRender || recordsToRender.length === 0) {
        tbody.innerHTML = '<tr><td colspan="11" class="text-center text-muted py-4">暂无记录</td></tr>';
        return;
    }

    recordsToRender.forEach((record, index) => {
        // 应用用户岗位偏好（与卡片视图相同的逻辑）
        if (record.positions && record.positions.length > 0) {
            const preferredPositionId = userPositionPreferences[record.companyGroupId];
            let currentPosition = null;

            if (preferredPositionId) {
                currentPosition = record.positions.find(p => p.id == preferredPositionId);
            }

            if (!currentPosition) {
                currentPosition = record.currentPosition || record.positions[0];
            }

            if (currentPosition) {
                record.currentPosition = currentPosition;
                record.position = currentPosition.position;
                record.finalResult = currentPosition.finalResult;
                record.currentStatus = currentPosition.currentStatus;
                record.currentStatusDate = currentPosition.currentStatusDate;
                record.expectedSalaryType = currentPosition.expectedSalaryType;
                record.expectedSalaryValue = currentPosition.expectedSalaryValue;
                record.remarks = currentPosition.remarks;
                record.interviews = currentPosition.interviews;
                record.poolDays = currentPosition.poolDays;
            }
        } else {
            if (!record.currentPosition) {
                record.currentPosition = record;
            }
        }

        // 生成公司名称（如果有URL则作为超链接）
        const companyNameHtml = record.companyUrl
            ? `<a href="${record.companyUrl}" target="_blank" class="company-link">${record.companyName}</a>`
            : record.companyName;

        // 生成岗位选择器
        let positionCell = '';
        if (record.positions && record.positions.length > 1) {
            positionCell = `
                <select class="form-select form-select-sm position-selector" 
                        onchange="switchPosition('${record.companyGroupId}', this.value)">
                    ${record.positions.map(pos => `
                        <option value="${pos.id}" ${pos.id == record.currentPosition?.id ? 'selected' : ''}>
                            ${pos.position}
                        </option>
                    `).join('')}
                </select>
            `;
        } else {
            positionCell = record.position || '-';
        }

        // 生成状态徽章
        let statusBadge = '';
        if (record.finalResult) {
            let badgeClass = '';
            switch(record.finalResult) {
                case 'PENDING': badgeClass = 'status-pending'; break;
                case 'OC': badgeClass = 'status-oc'; break;
                case '简历挂':
                case '测评挂':
                case '笔试挂':
                case '面试挂': badgeClass = 'status-rejected'; break;
            }
            statusBadge = `<span class="status-badge ${badgeClass}">${record.finalResult}</span>`;
        }

        // 生成当前状态
        const currentStatusText = record.currentStatus ? 
            `${record.currentStatus}${record.currentStatusDate ? ' (' + formatDateShort(record.currentStatusDate) + ')' : ''}` : 
            '-';

        // 生成薪资信息
        const salaryText = record.finalResult === 'OC' && record.expectedSalaryType && record.expectedSalaryValue ? 
            formatSalary(record.expectedSalaryType, record.expectedSalaryValue) : 
            '-';

        // 生成流程进度（简化版）
        const timelineData = {
            ...record,
            position: record.currentPosition ? record.currentPosition.position : record.position,
            finalResult: record.currentPosition ? record.currentPosition.finalResult : record.finalResult,
            currentStatus: record.currentPosition ? record.currentPosition.currentStatus : record.currentStatus,
            currentStatusDate: record.currentPosition ? record.currentPosition.currentStatusDate : record.currentStatusDate,
            applyTime: record.currentPosition ? record.currentPosition.applyTime : record.applyTime,
            testTime: record.currentPosition ? record.currentPosition.testTime : record.testTime,
            writtenExamTime: record.currentPosition ? record.currentPosition.writtenExamTime : record.writtenExamTime,
            interviews: record.currentPosition ? record.currentPosition.interviews : record.interviews
        };
        const timelineSteps = generateTimelineSteps(timelineData);

        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${companyNameHtml}</td>
            <td>${positionCell}</td>
            <td>${record.baseLocation || '-'}</td>
            <td>${record.applyTime ? formatDateShort(record.applyTime) : '-'}</td>
            <td>${currentStatusText}</td>
            <td>${statusBadge}</td>
            <td>${record.poolDays || 0}天</td>
            <td>${salaryText}</td>
            <td class="timeline-compact" title="${timelineSteps}">${timelineSteps}</td>
            <td title="${record.remarks || ''}">${record.remarks ? (record.remarks.length > 20 ? record.remarks.substring(0, 20) + '...' : record.remarks) : '-'}</td>
            <td>
                <div class="btn-group btn-group-sm" role="group">
                    <button class="btn btn-outline-primary btn-sm" onclick="editRecord(${record.currentPosition ? record.currentPosition.id : (record.positions && record.positions.length > 0 ? record.positions[0].id : record.id)})" title="编辑">
                        <i class="bi bi-pencil"></i>
                    </button>
                    <button class="btn btn-outline-danger btn-sm" onclick="deleteRecord(${record.currentPosition ? record.currentPosition.id : (record.positions && record.positions.length > 0 ? record.positions[0].id : record.id)})" title="删除">
                        <i class="bi bi-trash"></i>
                    </button>
                </div>
            </td>
        `;

        tbody.appendChild(row);
    });
}

// 生成简化的流程步骤文本
function generateTimelineSteps(record) {
    const steps = [];

    // 如果有当前状态，只显示投递-当前状态
    if (record.currentStatus && record.currentStatusDate) {
        if (record.applyTime) {
            steps.push('投递');
        }
        steps.push(record.currentStatus);
    } else {
        // 原有逻辑：显示所有填写的流程步骤
        if (record.applyTime) {
            steps.push('投递');
        }
        if (record.testTime) {
            steps.push('测评');
        }
        if (record.writtenExamTime) {
            steps.push('笔试');
        }
        if (record.interviews && record.interviews.length > 0) {
            const sortedInterviews = record.interviews
                .filter(interview => interview.interviewTime)
                .sort((a, b) => new Date(a.interviewTime) - new Date(b.interviewTime));
            sortedInterviews.forEach(interview => {
                steps.push(interview.interviewType);
            });
        }
        if (record.finalResult === 'OC') {
            steps.push('Offer');
        }
    }

    return steps.join(' → ');
}

// 应用筛选条件
function applyFilters() {
    const keywords = document.getElementById('searchInput').value.trim();
    const finalResult = document.getElementById('finalResultFilter').value;
    const currentStatus = document.getElementById('currentStatusFilter').value;
    const minSalary = document.getElementById('minSalaryFilter').value ? parseFloat(document.getElementById('minSalaryFilter').value) : null;

    // 检查是否有任何筛选条件
    const hasFilters = keywords || finalResult || currentStatus || (minSalary !== null && minSalary > 0);

    if (!hasFilters) {
        // 如果没有筛选条件，显示所有记录
        isFiltered = false;
        filteredRecords = [];
        renderRecords();
        hideFilterResult();
        return;
    }

    // 构建查询参数
    const params = new URLSearchParams();
    if (keywords) params.append('keywords', keywords);
    if (finalResult) params.append('finalResult', finalResult);
    if (currentStatus) params.append('currentStatus', currentStatus);
    if (minSalary !== null && minSalary > 0) params.append('minSalary', minSalary);

    // 发送搜索请求
    fetch(`${API_BASE}/records/search?${params.toString()}`, {
        method: 'GET',
        headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                filteredRecords = data.data;
                isFiltered = true;
                renderRecords();

                // 显示筛选结果数量
                const resultCount = filteredRecords.length;
                const totalCount = currentRecords.length;
                showFilterResult(resultCount, totalCount);
            } else {
                alert('搜索失败: ' + data.message);
            }
        })
        .catch(error => {
            console.error('搜索错误:', error);
            alert('搜索失败: ' + error.message);
        });
}

// 清空筛选条件
function clearFilters() {
    document.getElementById('searchInput').value = '';
    document.getElementById('finalResultFilter').value = '';
    document.getElementById('currentStatusFilter').value = '';
    document.getElementById('minSalaryFilter').value = '';

    isFiltered = false;
    filteredRecords = [];
    renderRecords();

    // 隐藏筛选结果提示
    hideFilterResult();
}

// 显示筛选结果
function showFilterResult(resultCount, totalCount) {
    let resultDiv = document.getElementById('filterResult');
    if (!resultDiv) {
        resultDiv = document.createElement('div');
        resultDiv.id = 'filterResult';
        resultDiv.className = 'alert alert-info mb-3';
        document.getElementById('processGrid').parentNode.insertBefore(resultDiv, document.getElementById('processGrid'));
    }

    resultDiv.innerHTML = `
        <i class="bi bi-info-circle"></i>
        筛选结果：找到 ${resultCount} 条记录（共 ${totalCount} 条）
        <button type="button" class="btn-close float-end" onclick="clearFilters()"></button>
    `;
}

// 隐藏筛选结果
function hideFilterResult() {
    const resultDiv = document.getElementById('filterResult');
    if (resultDiv) {
        resultDiv.remove();
    }
}

// 生成流程时间线
function generateTimeline(record) {
    console.log('生成流程时间线，记录信息:', record);
    const steps = [];

    // 如果有当前状态，只显示投递-当前状态
    if (record.currentStatus && record.currentStatusDate) {
        // 1. 投递时间（必须）
        if (record.applyTime) {
            steps.push({ key: 'apply', label: '简历投递', time: record.applyTime, number: 1 });
        }

        // 2. 当前状态
        steps.push({
            key: 'current',
            label: record.currentStatus,
            time: record.currentStatusDate,
            number: 2
        });
    } else {
        // 原有逻辑：显示所有填写的流程步骤
        // 1. 投递时间（必须）
        if (record.applyTime) {
            steps.push({ key: 'apply', label: '简历投递', time: record.applyTime, number: 1 });
        }

        // 2. 测评时间
        if (record.testTime) {
            steps.push({ key: 'test', label: '测评', time: record.testTime, number: steps.length + 1 });
        }

        // 3. 笔试时间
        if (record.writtenExamTime) {
            steps.push({ key: 'written', label: '笔试', time: record.writtenExamTime, number: steps.length + 1 });
        }

        // 4. 多轮面试时间（按时间排序）
        if (record.interviews && record.interviews.length > 0) {
            const sortedInterviews = record.interviews
                .filter(interview => interview.interviewTime)
                .sort((a, b) => new Date(a.interviewTime) - new Date(b.interviewTime));

            sortedInterviews.forEach(interview => {
                steps.push({
                    key: 'interview',
                    label: interview.interviewType,
                    time: interview.interviewTime,
                    number: steps.length + 1
                });
            });
        }

        // 5. Offer（只有OC状态才显示）
        if (record.finalResult === 'OC') {
            steps.push({ key: 'offer', label: 'Offer', time: new Date().toISOString(), number: steps.length + 1 });
        }
    }

    let timelineHtml = '';

    steps.forEach((step, index) => {
        const isActive = true; // 所有显示的步骤都是已完成的
        const timeStr = step.time ? formatDateShort(step.time) : '';

        timelineHtml += `
            <div class="timeline-step active">
                <div class="timeline-circle active">
                    ${step.number}
                </div>
                <div class="timeline-label">${step.label}</div>
                <div class="timeline-date">${timeStr}</div>
            </div>
        `;
    });

    console.log('生成的流程步骤数量:', steps.length, '步骤:', steps);
    console.log('生成的HTML:', timelineHtml);
    return timelineHtml;
}

// 获取最新面试时间
function getLatestInterviewTime(interviews) {
    if (!interviews || interviews.length === 0) return null;

    let latestTime = null;
    interviews.forEach(interview => {
        if (interview.interviewTime) {
            const time = new Date(interview.interviewTime);
            if (!latestTime || time > latestTime) {
                latestTime = time;
            }
        }
    });

    return latestTime ? latestTime.toISOString() : null;
}

// 格式化日期为短格式（月/日）
function formatDateShort(dateTimeStr) {
    if (!dateTimeStr) return '';
    const date = new Date(dateTimeStr);
    const month = date.getMonth() + 1;
    const day = date.getDate();
    return `${month}/${day}`;
}

// 获取薪资值（根据类型处理）
function getSalaryValue() {
    const salaryType = document.getElementById('expectedSalaryType').value;

    if (salaryType === '月薪') {
        const monthlySalary = document.getElementById('monthlySalary').value;
        const monthlyCount = document.getElementById('monthlyCount').value;
        if (monthlySalary && monthlyCount) {
            return `${monthlySalary}k×${monthlyCount}`;
        }
        return null;
    } else if (salaryType === '总包') {
        return document.getElementById('expectedSalaryValue').value || null;
    } else if (salaryType === '待商议') {
        return '待商议';
    }

    return null;
}

// 格式化薪资显示
function formatSalary(salaryType, salaryValue) {
    if (!salaryType || !salaryValue) return '-';

    switch(salaryType) {
        case '总包':
            return `${salaryValue}w`;
        case '月薪':
            return `${salaryValue}`;
        case '待商议':
            return '待商议';
        default:
            return salaryValue;
    }
}

// 显示添加记录模态框
function showAddModal() {
    document.getElementById('recordModalTitle').textContent = '添加投递记录';
    document.getElementById('recordForm').reset();
    document.getElementById('recordId').value = '';
    document.getElementById('interviewRecords').innerHTML = '';
    document.getElementById('salarySection').style.display = 'none';
    document.getElementById('currentStatusDateSection').style.display = 'none';
    document.getElementById('clearCurrentStatusBtn').style.display = 'none';
    document.getElementById('monthlySalaryContainer').style.display = 'none';
    
    // 清空岗位输入区域
    const positionContainer = document.getElementById('positionContainer');
    if (positionContainer) {
        positionContainer.innerHTML = `
            <div class="input-group mb-2">
                <input type="text" class="form-control position-input" placeholder="请输入岗位名称" required>
                <button class="btn btn-outline-success" type="button" onclick="addPosition()">
                    <i class="bi bi-plus"></i>
                </button>
            </div>
        `;
    }

    const modal = new bootstrap.Modal(document.getElementById('recordModal'));
    modal.show();
}

// 编辑记录
function editRecord(id) {
    // 在多岗位功能中，需要查找具体的岗位记录
    let record = null;
    let positionRecord = null;

    for (const r of currentRecords) {
        if (r.positions && r.positions.length > 0) {
            // 多岗位记录，查找具体岗位
            positionRecord = r.positions.find(p => p.id === id);
            if (positionRecord) {
                record = r;
                break;
            }
        } else if (r.id === id) {
            // 单岗位记录（兼容旧数据）
            record = r;
            positionRecord = r;
            break;
        }
    }

    if (!record) return;

    document.getElementById('recordModalTitle').textContent = '编辑投递记录';
    document.getElementById('recordId').value = positionRecord.id; // 使用具体岗位的ID
    document.getElementById('companyName').value = record.companyName;
    document.getElementById('baseLocation').value = record.baseLocation || '';
    document.getElementById('companyUrl').value = record.companyUrl || '';
    document.getElementById('applyTime').value = formatDateTimeForInput(record.applyTime);
    document.getElementById('testTime').value = record.testTime ? formatDateTimeForInput(record.testTime) : '';
    document.getElementById('writtenExamTime').value = record.writtenExamTime ? formatDateTimeForInput(record.writtenExamTime) : '';
    document.getElementById('finalResult').value = positionRecord.finalResult;

    // 处理多岗位显示 - 在编辑模式下显示岗位选择下拉框
    if (record.positions && record.positions.length > 1) {
        // 多岗位记录，显示岗位选择下拉框
        showPositionSelector(record.positions, positionRecord.id);
    } else {
        // 单岗位记录，也显示岗位管理界面（允许新增岗位）
        const singlePositionArray = [{
            id: positionRecord.id,
            position: positionRecord.position,
            finalResult: positionRecord.finalResult,
            currentStatus: positionRecord.currentStatus,
            currentStatusDate: positionRecord.currentStatusDate,
            expectedSalaryType: positionRecord.expectedSalaryType,
            expectedSalaryValue: positionRecord.expectedSalaryValue,
            remarks: positionRecord.remarks,
            applyTime: positionRecord.applyTime,
            testTime: positionRecord.testTime,
            writtenExamTime: positionRecord.writtenExamTime,
            interviews: positionRecord.interviews || []
        }];
        showPositionSelector(singlePositionArray, positionRecord.id);
    }

    // 使用具体岗位的数据
    document.getElementById('currentStatus').value = positionRecord.currentStatus || '';
    document.getElementById('currentStatusDate').value = positionRecord.currentStatusDate ? formatDateTimeForInput(positionRecord.currentStatusDate) : '';
    document.getElementById('expectedSalaryType').value = positionRecord.expectedSalaryType || '';

    // 处理薪资值显示
    if (positionRecord.expectedSalaryType === '月薪' && positionRecord.expectedSalaryValue) {
        // 解析月薪格式 "15k×12"
        const match = positionRecord.expectedSalaryValue.match(/(\d+(?:\.\d+)?)k×(\d+)/);
        if (match) {
            document.getElementById('monthlySalary').value = match[1];
            document.getElementById('monthlyCount').value = match[2];
        }
        document.getElementById('expectedSalaryValue').value = '';
    } else {
        document.getElementById('expectedSalaryValue').value = positionRecord.expectedSalaryValue || '';
        document.getElementById('monthlySalary').value = '';
        document.getElementById('monthlyCount').value = '12';
    }

    document.getElementById('remarks').value = positionRecord.remarks || '';

    // 渲染面试记录
    renderInterviewRecords(positionRecord.interviews || []);

    // 处理薪资显示
    handleFinalResultChange();

    // 处理薪资类型显示
    handleSalaryTypeChange();

    // 处理当前状态显示
    handleCurrentStatusChange();

    // 确保删除按钮状态正确
    const currentStatus = document.getElementById('currentStatus').value;
    const clearCurrentStatusBtn = document.getElementById('clearCurrentStatusBtn');
    if (currentStatus) {
        clearCurrentStatusBtn.style.display = 'block';
    } else {
        clearCurrentStatusBtn.style.display = 'none';
    }

    const modal = new bootstrap.Modal(document.getElementById('recordModal'));
    modal.show();
}

// 渲染面试记录
function renderInterviewRecords(interviews) {
    const container = document.getElementById('interviewRecords');
    container.innerHTML = '';

    interviews.forEach((interview, index) => {
        const div = document.createElement('div');
        div.className = 'row mb-2';
        div.innerHTML = `
            <div class="col-md-5">
                <select class="form-select" name="interviewType">
                    <option value="AI面" ${interview.interviewType === 'AI面' ? 'selected' : ''}>AI面</option>
                    <option value="一面" ${interview.interviewType === '一面' ? 'selected' : ''}>一面</option>
                    <option value="二面" ${interview.interviewType === '二面' ? 'selected' : ''}>二面</option>
                    <option value="三面" ${interview.interviewType === '三面' ? 'selected' : ''}>三面</option>
                    <option value="四面" ${interview.interviewType === '四面' ? 'selected' : ''}>四面</option>
                    <option value="五面" ${interview.interviewType === '五面' ? 'selected' : ''}>五面</option>
                    <option value="六面" ${interview.interviewType === '六面' ? 'selected' : ''}>六面</option>
                    <option value="七面" ${interview.interviewType === '七面' ? 'selected' : ''}>七面</option>
                    <option value="八面" ${interview.interviewType === '八面' ? 'selected' : ''}>八面</option>
                    <option value="九面" ${interview.interviewType === '九面' ? 'selected' : ''}>九面</option>
                    <option value="十面" ${interview.interviewType === '十面' ? 'selected' : ''}>十面</option>
                </select>
            </div>
            <div class="col-md-5">
                <input type="datetime-local" class="form-control" name="interviewTime" value="${formatDateTimeForInput(interview.interviewTime)}">
            </div>
            <div class="col-md-2">
                <button type="button" class="btn btn-outline-danger btn-sm" onclick="removeInterviewRecord(this)">
                    <i class="bi bi-trash"></i>
                </button>
            </div>
        `;
        container.appendChild(div);
    });
}

// 添加面试记录
function addInterviewRecord() {
    const container = document.getElementById('interviewRecords');
    const div = document.createElement('div');
    div.className = 'row mb-2';
    div.innerHTML = `
        <div class="col-md-5">
            <select class="form-select" name="interviewType">
                <option value="AI面">AI面</option>
                <option value="一面">一面</option>
                <option value="二面">二面</option>
                <option value="三面">三面</option>
                <option value="四面">四面</option>
                <option value="五面">五面</option>
                <option value="六面">六面</option>
                <option value="七面">七面</option>
                <option value="八面">八面</option>
                <option value="九面">九面</option>
                <option value="十面">十面</option>
            </select>
        </div>
        <div class="col-md-5">
            <input type="datetime-local" class="form-control" name="interviewTime">
        </div>
        <div class="col-md-2">
            <button type="button" class="btn btn-outline-danger btn-sm" onclick="removeInterviewRecord(this)">
                <i class="bi bi-trash"></i>
            </button>
        </div>
    `;
    container.appendChild(div);
}

// 删除面试记录
function removeInterviewRecord(button) {
    button.closest('.row').remove();
}

// 处理最终结果变化
function handleFinalResultChange() {
    const finalResult = document.getElementById('finalResult').value;
    const salarySection = document.getElementById('salarySection');

    if (finalResult === 'OC') {
        salarySection.style.display = 'block';
    } else {
        salarySection.style.display = 'none';
    }
}

// 处理薪资类型变化
function handleSalaryTypeChange() {
    const salaryType = document.getElementById('expectedSalaryType').value;
    const salaryValueContainer = document.getElementById('salaryValueContainer');
    const monthlySalaryContainer = document.getElementById('monthlySalaryContainer');
    const expectedSalaryValue = document.getElementById('expectedSalaryValue');
    const monthlySalary = document.getElementById('monthlySalary');
    const monthlyCount = document.getElementById('monthlyCount');

    if (salaryType === '月薪') {
        // 显示月薪和月数输入框
        salaryValueContainer.style.display = 'none';
        monthlySalaryContainer.style.display = 'block';
        expectedSalaryValue.value = '';
    } else if (salaryType === '待商议') {
        // 隐藏所有输入框
        salaryValueContainer.style.display = 'none';
        monthlySalaryContainer.style.display = 'none';
        expectedSalaryValue.value = '';
        monthlySalary.value = '';
        monthlyCount.value = '12';
    } else {
        // 总包：显示单个输入框
        salaryValueContainer.style.display = 'block';
        monthlySalaryContainer.style.display = 'none';
        monthlySalary.value = '';
        monthlyCount.value = '12';
    }
}

// 处理当前状态变化
function handleCurrentStatusChange() {
    const currentStatus = document.getElementById('currentStatus').value;
    const currentStatusDateSection = document.getElementById('currentStatusDateSection');
    const clearCurrentStatusBtn = document.getElementById('clearCurrentStatusBtn');

    if (currentStatus) {
        currentStatusDateSection.style.display = 'block';
        clearCurrentStatusBtn.style.display = 'block';
    } else {
        currentStatusDateSection.style.display = 'none';
        clearCurrentStatusBtn.style.display = 'none';
        document.getElementById('currentStatusDate').value = '';
    }
}

// 删除当前状态
function clearCurrentStatus() {
    // 清空表单字段
    document.getElementById('currentStatus').value = '';
    document.getElementById('currentStatusDate').value = '';
    document.getElementById('currentStatusDateSection').style.display = 'none';
    document.getElementById('clearCurrentStatusBtn').style.display = 'none';

    // 如果是在编辑模式下，需要保存到数据库
    const recordId = document.getElementById('recordId').value;
    if (recordId) {
        // 获取当前记录数据
        const record = currentRecords.find(r => r.id == recordId);
        if (record) {
            // 更新记录数据
            record.currentStatus = null;
            record.currentStatusDate = null;
            
            // 保存到数据库
            const recordData = {
                id: record.id,
                companyName: record.companyName,
                position: record.position,
                baseLocation: record.baseLocation,
                companyUrl: record.companyUrl,
                applyTime: record.applyTime,
                testTime: record.testTime,
                writtenExamTime: record.writtenExamTime,
                currentStatus: null,
                currentStatusDate: null,
                finalResult: record.finalResult,
                expectedSalaryType: record.expectedSalaryType,
                expectedSalaryValue: record.expectedSalaryValue,
                remarks: record.remarks,
                interviews: record.interviews || []
            };
            
            // 发送PUT请求更新记录
            const url = `${API_BASE}/records/${record.id}`;
            fetch(url, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${localStorage.getItem('token')}`
                },
                body: JSON.stringify(recordData)
            })
            .then(response => {
                if (!response.ok) {
                    throw new Error(`HTTP ${response.status}: ${response.statusText}`);
                }
                return response.json();
            })
            .then(data => {
                console.log('当前状态已清空');
                // 重新加载记录列表以更新UI
                loadRecords();
            })
            .catch(error => {
                console.error('清空当前状态失败:', error);
                alert('清空当前状态失败: ' + error.message);
            });
        }
    }
}

// 保存记录
function saveRecord() {
    const isEdit = document.getElementById('recordId').value;

    if (isEdit) {
        // 编辑模式：只保存当前选中的岗位
        saveEditRecord();
    } else {
        // 新增模式：保存所有岗位
        saveNewRecord();
    }
}

// 保存编辑的记录
function saveEditRecord() {
    const recordId = document.getElementById('recordId').value;
    const companyName = document.getElementById('companyName').value;
    const baseLocation = document.getElementById('baseLocation').value;
    const companyUrl = document.getElementById('companyUrl').value;
    const applyTime = document.getElementById('applyTime').value;
    const testTime = document.getElementById('testTime').value || null;
    const writtenExamTime = document.getElementById('writtenExamTime').value || null;
    const currentStatus = document.getElementById('currentStatus').value || null;
    const currentStatusDate = document.getElementById('currentStatusDate').value || null;
    const finalResult = document.getElementById('finalResult').value;
    const expectedSalaryType = document.getElementById('expectedSalaryType').value || null;
    const expectedSalaryValue = getSalaryValue();
    const remarks = document.getElementById('remarks').value;

    // 收集面试记录
    const interviews = [];
    const interviewElements = document.querySelectorAll('#interviewRecords .row');
    interviewElements.forEach(row => {
        const type = row.querySelector('select[name="interviewType"]').value;
        const time = row.querySelector('input[name="interviewTime"]').value;
        if (time) {
            interviews.push({
                interviewType: type,
                interviewTime: time
            });
        }
    });

    // 检查是否有岗位变更
    const positionList = document.getElementById('positionList');
    if (positionList) {
        // 多岗位编辑模式，需要处理岗位变更
        const positionItems = positionList.querySelectorAll('.position-item');
        const positions = Array.from(positionItems).map(item => {
            const input = item.querySelector('.position-input');
            const positionId = item.dataset.positionId;
            return {
                id: positionId,
                position: input.value.trim()
            };
        });

        // 检查是否有新增的临时岗位
        const tempPositions = positions.filter(p => p.id.startsWith('temp_'));
        if (tempPositions.length > 0) {
            // 有新增岗位，需要批量保存
            saveMultiplePositions(positions, companyName, baseLocation, companyUrl, applyTime, testTime, writtenExamTime, currentStatus, currentStatusDate, finalResult, expectedSalaryType, expectedSalaryValue, remarks, interviews);
            return;
        }
    }

    // 单岗位编辑模式或没有新增岗位
    const positionInput = document.querySelector('#positionContainer .position-input');
    const position = positionInput ? positionInput.value : '';

    if (!position) {
        alert('请选择或输入岗位名称');
        return;
    }

    const recordData = {
        id: recordId,
        companyName: companyName,
        position: position,
        baseLocation: baseLocation,
        companyUrl: companyUrl,
        applyTime: applyTime,
        testTime: testTime,
        writtenExamTime: writtenExamTime,
        currentStatus: currentStatus,
        currentStatusDate: currentStatusDate,
        finalResult: finalResult,
        expectedSalaryType: expectedSalaryType,
        expectedSalaryValue: expectedSalaryValue,
        remarks: remarks,
        interviews: interviews
    };

    const token = localStorage.getItem('token');

    fetch(`${API_BASE}/records/${recordId}`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(recordData)
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                bootstrap.Modal.getInstance(document.getElementById('recordModal')).hide();
                loadRecords();
            } else {
                alert('保存失败: ' + data.message);
            }
        })
        .catch(error => {
            console.error('保存错误:', error);
            alert('保存失败，请重试');
        });
}

// 保存多个岗位（编辑模式下的批量保存）
function saveMultiplePositions(positions, companyName, baseLocation, companyUrl, applyTime, testTime, writtenExamTime, currentStatus, currentStatusDate, finalResult, expectedSalaryType, expectedSalaryValue, remarks, interviews) {
    const token = localStorage.getItem('token');
    
    // 分离现有岗位和新增岗位
    const existingPositions = positions.filter(p => !p.id.startsWith('temp_'));
    const newPositions = positions.filter(p => p.id.startsWith('temp_'));
    
    // 先保存现有岗位的变更
    const existingPromises = existingPositions.map(position => {
        const recordData = {
            id: position.id,
            companyName: companyName,
            position: position.position,
            baseLocation: baseLocation,
            companyUrl: companyUrl,
            applyTime: applyTime,
            testTime: testTime,
            writtenExamTime: writtenExamTime,
            currentStatus: currentStatus,
            currentStatusDate: currentStatusDate,
            finalResult: finalResult,
            expectedSalaryType: expectedSalaryType,
            expectedSalaryValue: expectedSalaryValue,
            remarks: remarks,
            interviews: interviews
        };
        
        return fetch(`${API_BASE}/records/${position.id}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(recordData)
        });
    });
    
    // 再添加新岗位
    const newPositionPromises = newPositions.map(position => {
        const recordData = {
            companyName: companyName,
            position: position.position,
            baseLocation: baseLocation,
            companyUrl: companyUrl,
            applyTime: applyTime,
            testTime: testTime,
            writtenExamTime: writtenExamTime,
            currentStatus: currentStatus,
            currentStatusDate: currentStatusDate,
            finalResult: finalResult,
            expectedSalaryType: expectedSalaryType,
            expectedSalaryValue: expectedSalaryValue,
            remarks: remarks,
            interviews: interviews
        };
        
        return fetch(`${API_BASE}/records`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(recordData)
        });
    });
    
    // 等待所有操作完成
    Promise.all([...existingPromises, ...newPositionPromises])
        .then(responses => {
            const allSuccess = responses.every(response => response.ok);
            if (allSuccess) {
                bootstrap.Modal.getInstance(document.getElementById('recordModal')).hide();
                loadRecords();
            } else {
                alert('部分岗位保存失败，请检查');
            }
        })
        .catch(error => {
            console.error('批量保存错误:', error);
            alert('保存失败，请重试');
        });
}

// 保存新记录
function saveNewRecord() {
    const positions = getPositions();
    if (positions.length === 0) {
        alert('请至少输入一个岗位');
        return;
    }

    const companyName = document.getElementById('companyName').value;
    const baseLocation = document.getElementById('baseLocation').value;
    const companyUrl = document.getElementById('companyUrl').value;
    const applyTime = document.getElementById('applyTime').value;
    const testTime = document.getElementById('testTime').value || null;
    const writtenExamTime = document.getElementById('writtenExamTime').value || null;
    const currentStatus = document.getElementById('currentStatus').value || null;
    const currentStatusDate = document.getElementById('currentStatusDate').value || null;
    const finalResult = document.getElementById('finalResult').value;
    const expectedSalaryType = document.getElementById('expectedSalaryType').value || null;
    const expectedSalaryValue = getSalaryValue();
    const remarks = document.getElementById('remarks').value;

    // 收集面试记录
    const interviews = [];
    const interviewElements = document.querySelectorAll('#interviewRecords .row');
    interviewElements.forEach(row => {
        const type = row.querySelector('select[name="interviewType"]').value;
        const time = row.querySelector('input[name="interviewTime"]').value;
        if (time) {
            interviews.push({
                interviewType: type,
                interviewTime: time
            });
        }
    });

    // 为每个岗位创建记录
    const recordsToSave = positions.map((position, index) => ({
        id: document.getElementById('recordId').value || null,
        companyName: companyName,
        position: position,
        baseLocation: baseLocation,
        companyUrl: companyUrl,
        applyTime: applyTime,
        testTime: testTime,
        writtenExamTime: writtenExamTime,
        currentStatus: currentStatus,
        currentStatusDate: currentStatusDate,
        finalResult: finalResult,
        expectedSalaryType: expectedSalaryType,
        expectedSalaryValue: expectedSalaryValue,
        remarks: remarks,
        interviews: interviews,
        isPrimary: index === 0 // 第一个岗位作为主要岗位
    }));

    const token = localStorage.getItem('token');
    createMultipleRecords(recordsToSave, token);
}

// 创建多个记录
function createMultipleRecords(records, token) {
    // 使用批量添加API，确保同一公司的多个岗位使用相同的company_group_id
    fetch(`${API_BASE}/records/batch`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(records)
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            bootstrap.Modal.getInstance(document.getElementById('recordModal')).hide();
            loadRecords();
        } else {
            alert('保存失败: ' + data.message);
        }
    })
    .catch(error => {
        console.error('批量保存错误:', error);
        alert('保存失败，请重试');
    });
}


// 删除记录
function deleteRecord(id) {
    if (confirm('确定要删除这条记录吗？')) {
        const token = localStorage.getItem('token');

        fetch(`${API_BASE}/records/${id}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    loadRecords();
                } else {
                    alert('删除失败: ' + data.message);
                }
            })
            .catch(error => {
                console.error('删除错误:', error);
                alert('删除失败，请重试');
            });
    }
}

// 导出数据
function exportData() {
    const token = localStorage.getItem('token');
    
    // 根据当前筛选状态决定导出内容
    let exportUrl = `${API_BASE}/records/export`;
    let filename = '投递记录.xlsx';
    
    // 如果有筛选条件，使用搜索接口导出
    if (isFiltered && filteredRecords.length > 0) {
        const keywords = document.getElementById('searchInput').value.trim();
        const finalResult = document.getElementById('finalResultFilter').value;
        const currentStatus = document.getElementById('currentStatusFilter').value;
        const minSalary = document.getElementById('minSalaryFilter').value ? parseFloat(document.getElementById('minSalaryFilter').value) : null;
        
        // 构建查询参数
        const params = new URLSearchParams();
        if (keywords) params.append('keywords', keywords);
        if (finalResult) params.append('finalResult', finalResult);
        if (currentStatus) params.append('currentStatus', currentStatus);
        if (minSalary !== null && minSalary > 0) params.append('minSalary', minSalary);
        
        exportUrl = `${API_BASE}/records/export/search?${params.toString()}`;
        filename = `投递记录_筛选结果_${new Date().toISOString().slice(0, 10)}.xlsx`;
    }

    fetch(exportUrl, {
        headers: {
            'Authorization': `Bearer ${token}`
        }
    })
        .then(response => {
            if (response.ok) {
                return response.blob();
            } else {
                throw new Error('导出失败');
            }
        })
        .then(blob => {
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = filename;
            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(url);
            document.body.removeChild(a);
            
            // 显示导出成功提示
            if (isFiltered) {
                alert(`已导出筛选结果，共 ${filteredRecords.length} 条记录`);
            } else {
                alert(`已导出所有记录，共 ${currentRecords.length} 条记录`);
            }
        })
        .catch(error => {
            console.error('导出错误:', error);
            alert('导出失败，请重试');
        });
}

// 生成邀请码
function generateInviteCode() {
    const token = localStorage.getItem('token');

    fetch(`${API_BASE}/auth/generate-invite-code`, {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${token}`
        }
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                document.getElementById('generatedInviteCode').value = data.inviteCode;
                const modal = new bootstrap.Modal(document.getElementById('inviteCodeModal'));
                modal.show();
            } else {
                alert('生成邀请码失败: ' + data.message);
            }
        })
        .catch(error => {
            console.error('生成邀请码错误:', error);
            alert('生成邀请码失败，请重试');
        });
}

// 复制邀请码
function copyInviteCode() {
    const input = document.getElementById('generatedInviteCode');
    input.select();
    document.execCommand('copy');
    alert('邀请码已复制到剪贴板');
}

// 显示导入模态框
function showImportModal() {
    // 重置表单
    document.getElementById('importFile').value = '';
    document.getElementById('importModeAdd').checked = true;
    document.getElementById('importPreview').style.display = 'none';
    document.getElementById('previewBtn').disabled = false;
    document.getElementById('confirmImportBtn').disabled = true;
    
    const modal = new bootstrap.Modal(document.getElementById('importModal'));
    modal.show();
}

// 预览导入数据
function previewImportData() {
    const fileInput = document.getElementById('importFile');
    const file = fileInput.files[0];
    
    if (!file) {
        alert('请先选择Excel文件');
        return;
    }
    
    if (!file.name.match(/\.(xlsx|xls)$/i)) {
        alert('请选择Excel文件（.xlsx或.xls格式）');
        return;
    }
    
    const formData = new FormData();
    formData.append('file', file);
    formData.append('mode', 'preview'); // 预览模式
    
    const token = localStorage.getItem('token');
    
    fetch(`${API_BASE}/records/import`, {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${token}`
        },
        body: formData
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            displayImportPreview(data.data);
            document.getElementById('previewBtn').disabled = true;
            document.getElementById('confirmImportBtn').disabled = false;
        } else {
            alert('预览失败: ' + data.message);
        }
    })
    .catch(error => {
        console.error('预览错误:', error);
        alert('预览失败，请重试');
    });
}

// 显示导入预览
function displayImportPreview(records) {
    const previewBody = document.getElementById('importPreviewBody');
    previewBody.innerHTML = '';
    
    records.slice(0, 10).forEach(record => { // 只显示前10条
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${record.companyName || '-'}</td>
            <td>${record.position || '-'}</td>
            <td>${record.baseLocation || '-'}</td>
            <td>${record.applyTime || '-'}</td>
            <td>${record.finalResult || '-'}</td>
        `;
        previewBody.appendChild(row);
    });
    
    if (records.length > 10) {
        const row = document.createElement('tr');
        row.innerHTML = `<td colspan="5" class="text-center text-muted">... 还有 ${records.length - 10} 条记录</td>`;
        previewBody.appendChild(row);
    }
    
    document.getElementById('importPreview').style.display = 'block';
}

// 确认导入
function confirmImport() {
    const fileInput = document.getElementById('importFile');
    const file = fileInput.files[0];
    const importMode = document.querySelector('input[name="importMode"]:checked').value;
    
    if (!file) {
        alert('请先选择Excel文件');
        return;
    }
    
    if (!confirm(`确定要${getImportModeText(importMode)}吗？`)) {
        return;
    }
    
    const formData = new FormData();
    formData.append('file', file);
    formData.append('mode', importMode);
    
    const token = localStorage.getItem('token');
    
    // 显示加载状态
    const confirmBtn = document.getElementById('confirmImportBtn');
    const originalText = confirmBtn.innerHTML;
    confirmBtn.innerHTML = '<i class="bi bi-hourglass-split"></i> 导入中...';
    confirmBtn.disabled = true;
    
    fetch(`${API_BASE}/records/import`, {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${token}`
        },
        body: formData
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            alert(`导入成功！共处理 ${data.processedCount} 条记录，成功 ${data.successCount} 条，跳过 ${data.skippedCount} 条`);
            bootstrap.Modal.getInstance(document.getElementById('importModal')).hide();
            loadRecords(); // 重新加载记录
        } else {
            alert('导入失败: ' + data.message);
        }
    })
    .catch(error => {
        console.error('导入错误:', error);
        alert('导入失败，请重试');
    })
    .finally(() => {
        // 恢复按钮状态
        confirmBtn.innerHTML = originalText;
        confirmBtn.disabled = false;
    });
}

// 获取导入模式文本
function getImportModeText(mode) {
    switch(mode) {
        case 'add': return '新增导入';
        case 'replace': return '替换导入（将删除所有现有数据）';
        case 'skip': return '跳过重复导入';
        default: return '导入';
    }
}

// 格式化日期时间
function formatDateTime(dateTimeStr) {
    if (!dateTimeStr) return '-';
    const date = new Date(dateTimeStr);
    return date.toLocaleString('zh-CN');
}

// 格式化日期时间为input格式
function formatDateTimeForInput(dateTimeStr) {
    if (!dateTimeStr) return '';
    const date = new Date(dateTimeStr);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day}T${hours}:${minutes}`;
}

// 多岗位相关函数
function addPosition() {
    const container = document.getElementById('positionContainer');
    const inputGroup = document.createElement('div');
    inputGroup.className = 'input-group mb-2';
    inputGroup.innerHTML = `
        <input type="text" class="form-control position-input" placeholder="请输入岗位名称" required>
        <button class="btn btn-outline-danger" type="button" onclick="removePosition(this)">
            <i class="bi bi-dash"></i>
        </button>
    `;
    container.appendChild(inputGroup);
}

function removePosition(button) {
    const container = document.getElementById('positionContainer');
    if (container.children.length > 1) {
        button.closest('.input-group').remove();
    }
}

function getPositions() {
    const inputs = document.querySelectorAll('#positionContainer .position-input');
    return Array.from(inputs)
        .map(input => input.value.trim())
        .filter(value => value !== '');
}

function setPositions(positions) {
    const container = document.getElementById('positionContainer');
    container.innerHTML = '';

    positions.forEach((position, index) => {
        const inputGroup = document.createElement('div');
        inputGroup.className = 'input-group mb-2';
        inputGroup.innerHTML = `
            <input type="text" class="form-control position-input" placeholder="请输入岗位名称" value="${position}" required>
            <button class="btn btn-outline-${index === 0 ? 'success' : 'danger'}" type="button" onclick="${index === 0 ? 'addPosition()' : 'removePosition(this)'}">
                <i class="bi bi-${index === 0 ? 'plus' : 'dash'}"></i>
            </button>
        `;
        container.appendChild(inputGroup);
    });
}

// 加载用户岗位偏好
function loadUserPositionPreferences() {
    const saved = localStorage.getItem('positionPreferences');
    if (saved) {
        try {
            userPositionPreferences = JSON.parse(saved);
        } catch (e) {
            console.error('加载岗位偏好失败:', e);
        }
    }
}

// 保存用户岗位偏好
function saveUserPositionPreferences() {
    localStorage.setItem('positionPreferences', JSON.stringify(userPositionPreferences));
}

// 切换岗位显示
function switchPosition(companyGroupId, positionId) {
    console.log('切换岗位:', companyGroupId, positionId);

    // 确保positionId是数字类型
    const numericPositionId = parseInt(positionId);
    userPositionPreferences[companyGroupId] = numericPositionId;
    saveUserPositionPreferences();

    console.log('用户偏好已更新:', userPositionPreferences);

    // 找到对应的记录并更新当前岗位
    const record = currentRecords.find(r => r.companyGroupId === companyGroupId);
    if (record && record.positions) {
        const selectedPosition = record.positions.find(p => p.id == numericPositionId);
        if (selectedPosition) {
            record.currentPosition = selectedPosition;
            // 更新记录的所有信息为选中岗位的信息
            record.position = selectedPosition.position;
            record.finalResult = selectedPosition.finalResult;
            record.currentStatus = selectedPosition.currentStatus;
            record.currentStatusDate = selectedPosition.currentStatusDate;
            record.expectedSalaryType = selectedPosition.expectedSalaryType;
            record.expectedSalaryValue = selectedPosition.expectedSalaryValue;
            record.remarks = selectedPosition.remarks;
            record.poolDays = selectedPosition.poolDays;
            
            // 更新流程时间信息（从选中岗位获取）
            record.applyTime = selectedPosition.applyTime;
            record.testTime = selectedPosition.testTime;
            record.writtenExamTime = selectedPosition.writtenExamTime;
            record.interviews = selectedPosition.interviews;
            
            console.log('切换后的记录信息:', {
                position: record.position,
                finalResult: record.finalResult,
                currentStatus: record.currentStatus,
                interviews: record.interviews?.length || 0,
                applyTime: record.applyTime,
                testTime: record.testTime,
                writtenExamTime: record.writtenExamTime,
                companyGroupId: record.companyGroupId
            });
        }
    }

    // 重新渲染记录
    renderRecords();
}

// 显示岗位选择下拉框（编辑模式）
function showPositionSelector(positions, currentPositionId) {
    const container = document.getElementById('positionContainer');
    container.innerHTML = `
        <div class="mb-3">
            <label class="form-label">岗位管理</label>
            <div id="positionList">
                ${positions.map((pos, index) => `
                    <div class="input-group mb-2 position-item" data-position-id="${pos.id}">
                        <input type="text" class="form-control position-input" 
                               value="${pos.position}" 
                               placeholder="请输入岗位名称" 
                               onchange="updatePositionName(${pos.id}, this.value)"
                               ${pos.id == currentPositionId ? 'data-current="true"' : ''}>
                        <div class="input-group-text">
                            <div class="btn-group btn-group-sm" role="group">
                                <button type="button" class="btn btn-outline-primary btn-sm" 
                                        onclick="selectPosition(${pos.id})" 
                                        title="选择此岗位进行编辑"
                                        ${pos.id == currentPositionId ? 'disabled' : ''}>
                                    <i class="bi bi-check"></i>
                                </button>
                                <button type="button" class="btn btn-outline-danger btn-sm" 
                                        onclick="removePositionFromEdit(${pos.id})" 
                                        title="删除此岗位"
                                        ${positions.length <= 1 ? 'disabled' : ''}>
                                    <i class="bi bi-trash"></i>
                                </button>
                            </div>
                        </div>
                    </div>
                `).join('')}
            </div>
            <div class="d-flex justify-content-between align-items-center">
                <button type="button" class="btn btn-outline-success btn-sm" onclick="addNewPositionToEdit()">
                    <i class="bi bi-plus"></i> 新增岗位
                </button>
                <small class="text-muted">编辑岗位名称，点击✓选择要编辑的岗位</small>
            </div>
        </div>
    `;
}

// 显示单岗位输入框（编辑模式）
function showSinglePositionInput(position) {
    const container = document.getElementById('positionContainer');
    container.innerHTML = `
        <div class="input-group mb-2">
            <input type="text" class="form-control position-input" placeholder="请输入岗位名称" value="${position}" required>
            <button class="btn btn-outline-success" type="button" onclick="addPosition()">
                <i class="bi bi-plus"></i>
            </button>
        </div>
    `;
}

// 切换编辑的岗位
function switchEditPosition(positionId) {
    // 找到当前编辑的公司记录
    const recordId = document.getElementById('recordId').value;
    const record = currentRecords.find(r => {
        if (r.positions && r.positions.length > 0) {
            return r.positions.some(p => p.id == recordId);
        }
        return r.id == recordId;
    });

    if (!record || !record.positions) return;

    // 找到选中的岗位
    const selectedPosition = record.positions.find(p => p.id == positionId);
    if (!selectedPosition) return;

    // 更新表单数据
    document.getElementById('recordId').value = selectedPosition.id;
    document.getElementById('finalResult').value = selectedPosition.finalResult;
    document.getElementById('currentStatus').value = selectedPosition.currentStatus || '';
    document.getElementById('currentStatusDate').value = selectedPosition.currentStatusDate ? formatDateTimeForInput(selectedPosition.currentStatusDate) : '';
    document.getElementById('expectedSalaryType').value = selectedPosition.expectedSalaryType || '';

    // 更新流程时间信息
    document.getElementById('applyTime').value = selectedPosition.applyTime ? formatDateTimeForInput(selectedPosition.applyTime) : '';
    document.getElementById('testTime').value = selectedPosition.testTime ? formatDateTimeForInput(selectedPosition.testTime) : '';
    document.getElementById('writtenExamTime').value = selectedPosition.writtenExamTime ? formatDateTimeForInput(selectedPosition.writtenExamTime) : '';

    // 处理薪资值显示
    if (selectedPosition.expectedSalaryType === '月薪' && selectedPosition.expectedSalaryValue) {
        const match = selectedPosition.expectedSalaryValue.match(/(\d+(?:\.\d+)?)k×(\d+)/);
        if (match) {
            document.getElementById('monthlySalary').value = match[1];
            document.getElementById('monthlyCount').value = match[2];
        }
        document.getElementById('expectedSalaryValue').value = '';
    } else {
        document.getElementById('expectedSalaryValue').value = selectedPosition.expectedSalaryValue || '';
        document.getElementById('monthlySalary').value = '';
        document.getElementById('monthlyCount').value = '12';
    }

    document.getElementById('remarks').value = selectedPosition.remarks || '';

    // 渲染面试记录 - 使用选中岗位的面试记录
    renderInterviewRecords(selectedPosition.interviews || []);
    
    console.log('编辑模式切换岗位后的信息:', {
        position: selectedPosition.position,
        finalResult: selectedPosition.finalResult,
        currentStatus: selectedPosition.currentStatus,
        applyTime: selectedPosition.applyTime,
        testTime: selectedPosition.testTime,
        writtenExamTime: selectedPosition.writtenExamTime,
        interviews: selectedPosition.interviews?.length || 0
    });

    // 处理各种状态显示
    handleFinalResultChange();
    handleSalaryTypeChange();
    handleCurrentStatusChange();

    // 确保删除按钮状态正确
    const currentStatus = document.getElementById('currentStatus').value;
    const clearCurrentStatusBtn = document.getElementById('clearCurrentStatusBtn');
    if (currentStatus) {
        clearCurrentStatusBtn.style.display = 'block';
    } else {
        clearCurrentStatusBtn.style.display = 'none';
    }
}

// 选择岗位进行编辑
function selectPosition(positionId) {
    // 更新UI状态
    document.querySelectorAll('.position-item').forEach(item => {
        const input = item.querySelector('.position-input');
        const button = item.querySelector('button[onclick^="selectPosition"]');
        
        if (item.dataset.positionId == positionId) {
            input.setAttribute('data-current', 'true');
            button.disabled = true;
            button.innerHTML = '<i class="bi bi-check-circle-fill"></i>';
        } else {
            input.removeAttribute('data-current');
            button.disabled = false;
            button.innerHTML = '<i class="bi bi-check"></i>';
        }
    });
    
    // 切换岗位数据
    switchEditPosition(positionId);
}

// 更新岗位名称
function updatePositionName(positionId, newName) {
    // 找到当前编辑的公司记录
    const recordId = document.getElementById('recordId').value;
    const record = currentRecords.find(r => {
        if (r.positions && r.positions.length > 0) {
            return r.positions.some(p => p.id == recordId);
        }
        return r.id == recordId;
    });

    if (!record || !record.positions) return;

    // 更新岗位名称
    const position = record.positions.find(p => p.id == positionId);
    if (position) {
        position.position = newName;
    }
}

// 从编辑模式中删除岗位
function removePositionFromEdit(positionId) {
    // 找到当前编辑的公司记录
    const recordId = document.getElementById('recordId').value;
    const record = currentRecords.find(r => {
        if (r.positions && r.positions.length > 0) {
            return r.positions.some(p => p.id == recordId);
        }
        return r.id == recordId;
    });

    if (!record || !record.positions || record.positions.length <= 1) {
        alert('至少需要保留一个岗位');
        return;
    }

    if (!confirm('确定要删除此岗位吗？')) {
        return;
    }

    // 从记录中移除岗位
    record.positions = record.positions.filter(p => p.id != positionId);
    
    // 如果删除的是当前编辑的岗位，切换到第一个岗位
    if (positionId == recordId) {
        const firstPosition = record.positions[0];
        document.getElementById('recordId').value = firstPosition.id;
        switchEditPosition(firstPosition.id);
    }
    
    // 重新渲染岗位列表
    showPositionSelector(record.positions, document.getElementById('recordId').value);
}

// 在编辑模式中新增岗位
function addNewPositionToEdit() {
    const newPositionName = prompt('请输入新岗位名称：');
    if (!newPositionName || newPositionName.trim() === '') {
        return;
    }

    // 找到当前编辑的公司记录
    const recordId = document.getElementById('recordId').value;
    const record = currentRecords.find(r => {
        if (r.positions && r.positions.length > 0) {
            return r.positions.some(p => p.id == recordId);
        }
        return r.id == recordId;
    });

    if (!record || !record.positions) return;

    // 创建新岗位对象（临时，不保存到数据库）
    const newPosition = {
        id: 'temp_' + Date.now(), // 临时ID
        position: newPositionName.trim(),
        finalResult: 'PENDING',
        currentStatus: null,
        currentStatusDate: null,
        expectedSalaryType: null,
        expectedSalaryValue: null,
        remarks: '',
        applyTime: record.applyTime,
        testTime: record.testTime,
        writtenExamTime: record.writtenExamTime,
        interviews: []
    };

    // 添加到岗位列表
    record.positions.push(newPosition);
    
    // 重新渲染岗位列表
    showPositionSelector(record.positions, document.getElementById('recordId').value);
}

// 新增岗位到当前公司
function addNewPositionToCompany() {
    const newPositionName = prompt('请输入新岗位名称：');
    if (!newPositionName || newPositionName.trim() === '') {
        return;
    }

    // 获取当前公司信息
    const companyName = document.getElementById('companyName').value;
    const baseLocation = document.getElementById('baseLocation').value;
    const companyUrl = document.getElementById('companyUrl').value;
    const applyTime = document.getElementById('applyTime').value;
    const testTime = document.getElementById('testTime').value || null;
    const writtenExamTime = document.getElementById('writtenExamTime').value || null;
    const currentStatus = document.getElementById('currentStatus').value || null;
    const currentStatusDate = document.getElementById('currentStatusDate').value || null;
    const finalResult = document.getElementById('finalResult').value;
    const expectedSalaryType = document.getElementById('expectedSalaryType').value || null;
    const expectedSalaryValue = getSalaryValue();
    const remarks = document.getElementById('remarks').value;

    // 收集面试记录
    const interviews = [];
    const interviewElements = document.querySelectorAll('#interviewRecords .row');
    interviewElements.forEach(row => {
        const type = row.querySelector('select[name="interviewType"]').value;
        const time = row.querySelector('input[name="interviewTime"]').value;
        if (time) {
            interviews.push({
                interviewType: type,
                interviewTime: time
            });
        }
    });

    // 创建新岗位记录
    const newPositionData = {
        companyName: companyName,
        position: newPositionName.trim(),
        baseLocation: baseLocation,
        companyUrl: companyUrl,
        applyTime: applyTime,
        testTime: testTime,
        writtenExamTime: writtenExamTime,
        currentStatus: currentStatus,
        currentStatusDate: currentStatusDate,
        finalResult: finalResult,
        expectedSalaryType: expectedSalaryType,
        expectedSalaryValue: expectedSalaryValue,
        remarks: remarks,
        interviews: interviews
    };

    const token = localStorage.getItem('token');

    fetch(`${API_BASE}/records`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(newPositionData)
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                // 重新加载记录以更新UI
                loadRecords();
                // 关闭模态框
                bootstrap.Modal.getInstance(document.getElementById('recordModal')).hide();
            } else {
                alert('新增岗位失败: ' + data.message);
            }
        })
        .catch(error => {
            console.error('新增岗位错误:', error);
            alert('新增岗位失败，请重试');
        });
}

// 删除当前岗位
function deleteCurrentPosition() {
    const currentPositionId = document.getElementById('recordId').value;
    const positionSelector = document.getElementById('positionSelector');
    const currentPositionName = positionSelector.options[positionSelector.selectedIndex].text;

    if (!confirm(`确定要删除岗位"${currentPositionName}"吗？此操作不可撤销！`)) {
        return;
    }

    const token = localStorage.getItem('token');

    fetch(`${API_BASE}/records/${currentPositionId}`, {
        method: 'DELETE',
        headers: {
            'Authorization': `Bearer ${token}`
        }
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                // 重新加载记录以更新UI
                loadRecords();
                // 关闭模态框
                bootstrap.Modal.getInstance(document.getElementById('recordModal')).hide();
            } else {
                alert('删除岗位失败: ' + data.message);
            }
        })
        .catch(error => {
            console.error('删除岗位错误:', error);
            alert('删除岗位失败，请重试');
        });
}

