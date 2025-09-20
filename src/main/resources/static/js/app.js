// 全局变量
let currentUser = null;
let currentRecords = [];
let allRecords = []; // 保存所有原始记录
let filteredRecords = [];
let isFiltered = false;
let currentCompanyGroupMap = new Map(); // 公司组映射：key=companyGroupId, value=record数组
let currentViewMode = 'card'; // 当前显示模式：'card' 或 'list'

// 模态框相关变量
let modalRecords = []; // 模态框中当前编辑的所有record
let currentModalRecordIndex = 0; // 当前选中的record索引
let isModalEditMode = false; // 是否为编辑模式

// 排序相关变量
let currentSortType = 'poolDays'; // 当前排序类型，默认为泡池时间

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

    // 用户岗位偏好已移除，现在使用公司组映射管理
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
                allRecords = data.data; // 保存所有原始记录
                currentRecords = data.data;
                buildCompanyGroupMap();
                renderRecords();
            } else {
                console.error('加载记录失败:', data.message);
            }
        })
        .catch(error => {
            console.error('加载记录错误:', error);
        });
}

// 构建公司组映射
function buildCompanyGroupMap() {
    currentCompanyGroupMap.clear();

    currentRecords.forEach(record => {
        const companyGroupId = record.companyGroupId;
        console.log(record.companyName, record.position, "isPrimary: ", record.isPrimary);
        if (!currentCompanyGroupMap.has(companyGroupId)) {
            currentCompanyGroupMap.set(companyGroupId, []);
        }


        if(record.isPrimary){
            currentCompanyGroupMap.get(companyGroupId).splice(0, 0, record);
        }
        else{
            currentCompanyGroupMap.get(companyGroupId).push(record);
        }

    });

    // 对公司组进行排序
    sortRecords();

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
    // 如果传入了records参数，重新构建映射
    if (records) {
        currentRecords = records;
        buildCompanyGroupMap();
    }

    if (currentViewMode === 'card') {
        renderCardView();
    } else {
        renderListView();
    }
}

// 渲染卡片视图
function renderCardView() {
    const container = document.getElementById('processGrid');
    container.innerHTML = '';

    if (currentCompanyGroupMap.size === 0) {
        container.innerHTML = '<div class="col-12 text-center text-muted py-5">暂无记录</div>';
        return;
    }

    // 遍历公司组映射，渲染每个公司组的第一个record
    currentCompanyGroupMap.forEach((records, companyGroupId) => {
        if (records.length === 0) return;

        // 获取第一个record（最新的）
        const record = records[0];

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

        if (records.length > 1) {
            positionSelector = `
                <div class="process-info-item">
                    <span class="process-info-label">岗位</span>
                    <select class="form-select form-select-sm position-dropdown" 
                            onchange="switchPosition('${companyGroupId}', this.value)"
                            style="display: inline-block; width: auto; min-width: 120px;">
                        ${records.map(r => `
                            <option value="${r.id}" ${r.id == record.id ? 'selected' : ''}>
                                ${r.position}
                            </option>
                        `).join('')}
                    </select>
                </div>
            `;
        } else {
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
                <button class="btn btn-outline-primary btn-sm" onclick="editCompanyGroup('${companyGroupId}')">
                    <i class="bi bi-pencil"></i> 编辑
                </button>
                <button class="btn btn-outline-danger btn-sm" onclick="deleteCompanyGroup('${companyGroupId}')">
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
function renderListView() {
    const tbody = document.getElementById('recordsTableBody');
    tbody.innerHTML = '';

    if (currentCompanyGroupMap.size === 0) {
        tbody.innerHTML = '<tr><td colspan="11" class="text-center text-muted py-4">暂无记录</td></tr>';
        return;
    }

    // 遍历公司组映射，渲染每个公司组的第一个record
    currentCompanyGroupMap.forEach((records, companyGroupId) => {
        if (records.length === 0) return;

        // 获取第一个record（最新的）
        const record = records[0];

        // 生成公司名称（如果有URL则作为超链接）
        const companyNameHtml = record.companyUrl
            ? `<a href="${record.companyUrl}" target="_blank" class="company-link">${record.companyName}</a>`
            : record.companyName;

        // 生成record选择器
        let positionCell = '';
        if (records.length > 1) {
            positionCell = `
                <select class="form-select form-select-sm position-selector" 
                        onchange="switchPosition('${companyGroupId}', this.value)">
                    ${records.map(r => `
                        <option value="${r.id}" ${r.id == record.id ? 'selected' : ''}>
                            ${r.position}
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
                    <button class="btn btn-outline-primary btn-sm" onclick="editCompanyGroup('${companyGroupId}')" title="编辑">
                        <i class="bi bi-pencil"></i>
                    </button>
                    <button class="btn btn-outline-danger btn-sm" onclick="deleteCompanyGroup('${companyGroupId}')" title="删除">
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
        // 重新加载原始数据
        loadRecords();
        hideFilterResult();
        return;
    }

    // 在前端进行筛选
    filteredRecords = allRecords.filter(record => {
        // 关键词筛选
        if (keywords) {
            const keywordArray = keywords.split(/[,，]/).map(k => k.trim()).filter(k => k);
            const keywordMatch = keywordArray.some(keyword =>
                record.companyName.toLowerCase().includes(keyword.toLowerCase()) ||
                record.position.toLowerCase().includes(keyword.toLowerCase()) ||
                (record.baseLocation && record.baseLocation.toLowerCase().includes(keyword.toLowerCase()))
            );
            if (!keywordMatch) return false;
        }

        // 最终结果筛选
        if (finalResult) {
            if (finalResult === '已挂') {
                // 已挂：筛选所有包含"挂"的结果
                if (!record.finalResult || !record.finalResult.includes('挂')) {
                    return false;
                }
            } else if (finalResult === '待定') {
                // 待定：筛选PENDING状态
                if (record.finalResult !== 'PENDING') {
                    return false;
                }
            } else {
                // 其他状态：精确匹配
                if (record.finalResult !== finalResult) {
                    return false;
                }
            }
        }

        // 当前状态筛选
        if (currentStatus && record.currentStatus !== currentStatus) {
            return false;
        }

        // 最低薪资筛选
        if (minSalary !== null && minSalary > 0) {
            if (record.finalResult !== 'OC' || !record.expectedSalaryType || !record.expectedSalaryValue) {
                return false;
            }

            let salaryValue = 0;
            if (record.expectedSalaryType === '总包') {
                // 总包：提取数字部分
                const match = record.expectedSalaryValue.match(/(\d+(?:\.\d+)?)/);
                if (match) {
                    salaryValue = parseFloat(match[1]);
                }
            } else if (record.expectedSalaryType === '月薪') {
                // 月薪：解析 "15k×12" 格式
                const match = record.expectedSalaryValue.match(/(\d+(?:\.\d+)?)k×(\d+)/);
                if (match) {
                    const monthlySalary = parseFloat(match[1]);
                    const months = parseInt(match[2]);
                    salaryValue = monthlySalary * months / 12; // 转换为年总包
                }
            }

            if (salaryValue < minSalary) {
                return false;
            }
        }

        return true;
    });

    isFiltered = true;

    // 使用筛选后的数据重新构建公司组映射
    currentRecords = filteredRecords;
    buildCompanyGroupMap();
    renderRecords();

    // 显示筛选结果数量
    const resultCount = filteredRecords.length;
    const totalCount = allRecords.length;
    showFilterResult(resultCount, totalCount);
}

// 清空筛选条件
function clearFilters() {
    document.getElementById('searchInput').value = '';
    document.getElementById('finalResultFilter').value = '';
    document.getElementById('currentStatusFilter').value = '';
    document.getElementById('minSalaryFilter').value = '';

    isFiltered = false;
    filteredRecords = [];

    // 恢复原始数据
    currentRecords = [...allRecords];
    buildCompanyGroupMap();
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
    document.getElementById('companyGroupId').value = '';
    document.getElementById('interviewRecords').innerHTML = '';
    document.getElementById('salarySection').style.display = 'none';
    document.getElementById('currentStatusDateSection').style.display = 'none';
    document.getElementById('clearCurrentStatusBtn').style.display = 'none';
    document.getElementById('monthlySalaryContainer').style.display = 'none';
    document.getElementById('deleteRecordBtn').style.display = 'none';

    // 初始化模态框状态
    isModalEditMode = false;
    modalRecords = [];
    currentModalRecordIndex = 0;

    // 创建第一个空的record
    const newRecord = createEmptyRecord();
    modalRecords.push(newRecord);

    // 渲染标签页
    renderRecordTabs();

    // 渲染表单内容
    renderRecordForm();

    // 处理各种状态显示
    handleFinalResultChange();
    handleSalaryTypeChange();
    handleCurrentStatusChange();

    const modal = new bootstrap.Modal(document.getElementById('recordModal'));
    modal.show();
}

// 创建空的record对象
function createEmptyRecord() {
    return {
        id: null,
        position: '',
        baseLocation: '',
        companyUrl: '',
        applyTime: '',
        testTime: '',
        writtenExamTime: '',
        currentStatus: '',
        currentStatusDate: '',
        finalResult: 'PENDING',
        expectedSalaryType: '',
        expectedSalaryValue: '',
        remarks: '',
        interviews: []
    };
}

// 渲染标签页
function renderRecordTabs() {
    const container = document.getElementById('recordTabsContainer');
    container.innerHTML = '';

    modalRecords.forEach((record, index) => {
        const tab = document.createElement('div');
        tab.className = `record-tab ${index === currentModalRecordIndex ? 'active' : ''}`;
        tab.onclick = () => switchToRecord(index);

        tab.innerHTML = `
            <span class="record-tab-text">${record.position || '新记录'}</span>
        `;

        container.appendChild(tab);
    });
}

// 渲染表单内容
function renderRecordForm() {
    const currentRecord = modalRecords[currentModalRecordIndex];
    if (!currentRecord) return;

    // 更新表单字段
    document.getElementById('position').value = currentRecord.position || '';
    document.getElementById('baseLocation').value = currentRecord.baseLocation || '';
    document.getElementById('companyUrl').value = currentRecord.companyUrl || '';
    document.getElementById('applyTime').value = currentRecord.applyTime || '';
    document.getElementById('testTime').value = currentRecord.testTime || '';
    document.getElementById('writtenExamTime').value = currentRecord.writtenExamTime || '';
    document.getElementById('currentStatus').value = currentRecord.currentStatus || '';
    document.getElementById('currentStatusDate').value = currentRecord.currentStatusDate || '';
    document.getElementById('finalResult').value = currentRecord.finalResult || 'PENDING';
    document.getElementById('expectedSalaryType').value = currentRecord.expectedSalaryType || '';
    document.getElementById('expectedSalaryValue').value = currentRecord.expectedSalaryValue || '';
    document.getElementById('remarks').value = currentRecord.remarks || '';

    // 处理薪资值显示
    if (currentRecord.expectedSalaryType === '月薪' && currentRecord.expectedSalaryValue) {
        const match = currentRecord.expectedSalaryValue.match(/(\d+(?:\.\d+)?)k×(\d+)/);
        if (match) {
            document.getElementById('monthlySalary').value = match[1];
            document.getElementById('monthlyCount').value = match[2];
        }
        document.getElementById('expectedSalaryValue').value = '';
    } else {
        document.getElementById('expectedSalaryValue').value = currentRecord.expectedSalaryValue || '';
        document.getElementById('monthlySalary').value = '';
        document.getElementById('monthlyCount').value = '12';
    }

    // 渲染面试记录
    renderInterviewRecords(currentRecord.interviews || []);

    // 更新recordId
    document.getElementById('recordId').value = currentRecord.id || '';

    // 显示/隐藏删除按钮
    document.getElementById('deleteRecordBtn').style.display = modalRecords.length > 1 ? 'block' : 'none';
}

// 切换到指定record
function switchToRecord(index) {
    if (index === currentModalRecordIndex) return;

    // 保存当前record的数据
    saveCurrentRecordData();

    // 执行淡出动画
    const formContent = document.getElementById('recordFormContent');
    formContent.classList.add('fade-out');

    setTimeout(() => {
        // 更新索引
        currentModalRecordIndex = index;

        // 重新渲染标签页和表单
        renderRecordTabs();
        renderRecordForm();

        // 执行淡入动画
        formContent.classList.remove('fade-out');
        formContent.classList.add('fade-in');

        setTimeout(() => {
            formContent.classList.remove('fade-in');
        }, 200);
    }, 150);
}

// 添加新的record标签页
function addRecordTab() {
    // 保存当前record的数据
    saveCurrentRecordData();

    // 创建新record（复制第一个record的除position外的所有字段）
    const newRecord = createEmptyRecord();
    if (modalRecords.length > 0) {
        const firstRecord = modalRecords[0];
        Object.keys(newRecord).forEach(key => {
            if (key !== 'position' && key !== 'id') {
                newRecord[key] = firstRecord[key] || '';
            }
        });
    }

    modalRecords.push(newRecord);
    currentModalRecordIndex = modalRecords.length - 1;

    // 重新渲染
    renderRecordTabs();
    renderRecordForm();
}



// 保存当前record的数据到modalRecords
function saveCurrentRecordData() {
    if (currentModalRecordIndex >= modalRecords.length) return;

    const currentRecord = modalRecords[currentModalRecordIndex];
    currentRecord.position = document.getElementById('position').value.trim();
    currentRecord.baseLocation = document.getElementById('baseLocation').value.trim();
    currentRecord.companyUrl = document.getElementById('companyUrl').value.trim();
    currentRecord.applyTime = document.getElementById('applyTime').value;
    currentRecord.testTime = document.getElementById('testTime').value;
    currentRecord.writtenExamTime = document.getElementById('writtenExamTime').value;
    currentRecord.currentStatus = document.getElementById('currentStatus').value;
    currentRecord.currentStatusDate = document.getElementById('currentStatusDate').value;
    currentRecord.finalResult = document.getElementById('finalResult').value;
    currentRecord.expectedSalaryType = document.getElementById('expectedSalaryType').value;
    currentRecord.expectedSalaryValue = getSalaryValue();
    currentRecord.remarks = document.getElementById('remarks').value.trim();

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
    currentRecord.interviews = interviews;
}

// 编辑公司组
function editCompanyGroup(companyGroupId) {

    // 获取公司组的record数组
    const records = currentCompanyGroupMap.get(companyGroupId);
    if (!records || records.length === 0) {
        console.error('未找到公司组记录:', companyGroupId);
        return;
    }

    // 设置模态框标题
    document.getElementById('recordModalTitle').textContent = '编辑投递记录';

    // 初始化模态框状态
    isModalEditMode = true;
    modalRecords = [...records]; // 复制record数组
    currentModalRecordIndex = 0;

    // 设置公司名称和companyGroupId
    document.getElementById('companyName').value = records[0].companyName || '';
    document.getElementById('companyGroupId').value = companyGroupId;

    // 渲染标签页和表单
    renderRecordTabs();
    renderRecordForm();

    // 处理各种状态显示
    handleFinalResultChange();
    handleSalaryTypeChange();
    handleCurrentStatusChange();

    // 显示删除按钮
    document.getElementById('deleteRecordBtn').style.display = 'block';

    const modal = new bootstrap.Modal(document.getElementById('recordModal'));
    modal.show();
}

// 编辑记录（内部函数）
function editRecordWithCompanyGroup(companyGroupId, record) {

    document.getElementById('recordModalTitle').textContent = '编辑投递记录';
    document.getElementById('recordId').value = record.id;
    // 添加隐藏字段存储公司组ID
    let companyGroupIdField = document.getElementById('companyGroupId');
    if (!companyGroupIdField) {
        companyGroupIdField = document.createElement('input');
        companyGroupIdField.type = 'hidden';
        companyGroupIdField.id = 'companyGroupId';
        document.getElementById('recordForm').appendChild(companyGroupIdField);
    }
    companyGroupIdField.value = companyGroupId;
    document.getElementById('companyName').value = record.companyName;
    document.getElementById('baseLocation').value = record.baseLocation || '';
    document.getElementById('companyUrl').value = record.companyUrl || '';
    document.getElementById('applyTime').value = formatDateTimeForInput(record.applyTime);
    document.getElementById('testTime').value = record.testTime ? formatDateTimeForInput(record.testTime) : '';
    document.getElementById('writtenExamTime').value = record.writtenExamTime ? formatDateTimeForInput(record.writtenExamTime) : '';
    document.getElementById('finalResult').value = record.finalResult;

    // 显示岗位输入框（如果有同公司其他岗位，显示选择器）
    showPositionSelectorForEdit(companyGroupId, record);

    // 使用record的数据
    document.getElementById('currentStatus').value = record.currentStatus || '';
    document.getElementById('currentStatusDate').value = record.currentStatusDate ? formatDateTimeForInput(record.currentStatusDate) : '';
    document.getElementById('expectedSalaryType').value = record.expectedSalaryType || '';

    // 处理薪资值显示
    if (record.expectedSalaryType === '月薪' && record.expectedSalaryValue) {
        // 解析月薪格式 "15k×12"
        const match = record.expectedSalaryValue.match(/(\d+(?:\.\d+)?)k×(\d+)/);
        if (match) {
            document.getElementById('monthlySalary').value = match[1];
            document.getElementById('monthlyCount').value = match[2];
        }
        document.getElementById('expectedSalaryValue').value = '';
    } else {
        document.getElementById('expectedSalaryValue').value = record.expectedSalaryValue || '';
        document.getElementById('monthlySalary').value = '';
        document.getElementById('monthlyCount').value = '12';
    }

    document.getElementById('remarks').value = record.remarks || '';

    // 渲染面试记录
    renderInterviewRecords(record.interviews || []);

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
    // 保存当前record的数据
    saveCurrentRecordData();

    // 验证必填字段
    const companyName = document.getElementById('companyName').value.trim();
    if (!companyName) {
        alert('请输入公司名称');
        return;
    }

    // 验证所有record的position
    for (let i = 0; i < modalRecords.length; i++) {
        if (!modalRecords[i].position.trim()) {
            alert(`请输入第${i + 1}个记录的岗位名称`);
            return;
        }
    }

    if (isModalEditMode) {
        // 编辑模式：批量更新
        saveModalRecords();
    } else {
        // 新增模式：批量创建
        createModalRecords();
    }
}

// 保存模态框中的records（编辑模式）
async function saveModalRecords() {
    const companyName = document.getElementById('companyName').value.trim();
    const companyGroupId = document.getElementById('companyGroupId').value;

    try {
        // 分离有ID的记录（更新）和没有ID的记录（创建）
        const updatePromises = [];
        const createPromises = [];

        modalRecords.forEach(record => {
            const recordData = {
                companyName: companyName,
                position: record.position,
                baseLocation: record.baseLocation || null,
                companyUrl: record.companyUrl || null,
                applyTime: record.applyTime,
                testTime: record.testTime || null,
                writtenExamTime: record.writtenExamTime || null,
                currentStatus: record.currentStatus || null,
                currentStatusDate: record.currentStatusDate || null,
                finalResult: record.finalResult,
                expectedSalaryType: record.expectedSalaryType || null,
                expectedSalaryValue: record.expectedSalaryValue || null,
                remarks: record.remarks || null,
                interviews: record.interviews || []
            };

            if (record.id) {
                // 有ID的记录，使用PUT更新
                recordData.id = record.id;
                updatePromises.push(
                    fetch(`/records/${record.id}`, {
                        method: 'PUT',
                        headers: {
                            'Content-Type': 'application/json',
                            'Authorization': `Bearer ${localStorage.getItem('token')}`
                        },
                        body: JSON.stringify(recordData)
                    })
                );
            } else {
                // 没有ID的记录，使用POST创建
                createPromises.push(
                    fetch('/records', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                            'Authorization': `Bearer ${localStorage.getItem('token')}`
                        },
                        body: JSON.stringify(recordData)
                    })
                );
            }
        });

        // 执行所有更新和创建操作
        const allPromises = [...updatePromises, ...createPromises];
        const responses = await Promise.all(allPromises);

        // 检查响应
        for (const response of responses) {
            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.message || '保存记录失败');
            }
        }

        // 关闭模态框并刷新数据
        bootstrap.Modal.getInstance(document.getElementById('recordModal')).hide();
        loadRecords();

    } catch (error) {
        console.error('保存失败:', error);
        alert('保存失败: ' + error.message);
    }
}

// 创建模态框中的records（新增模式）
async function createModalRecords() {
    const companyName = document.getElementById('companyName').value.trim();

    try {
        // 准备创建数据
        const createPromises = modalRecords.map(record => {
            const recordData = {
                companyName: companyName,
                position: record.position,
                baseLocation: record.baseLocation || null,
                companyUrl: record.companyUrl || null,
                applyTime: record.applyTime,
                testTime: record.testTime || null,
                writtenExamTime: record.writtenExamTime || null,
                currentStatus: record.currentStatus || null,
                currentStatusDate: record.currentStatusDate || null,
                finalResult: record.finalResult,
                expectedSalaryType: record.expectedSalaryType || null,
                expectedSalaryValue: record.expectedSalaryValue || null,
                remarks: record.remarks || null,
                interviews: record.interviews || []
            };

            return fetch('/records', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${localStorage.getItem('token')}`
                },
                body: JSON.stringify(recordData)
            });
        });

        // 执行所有创建
        const responses = await Promise.all(createPromises);

        // 检查响应
        for (const response of responses) {
            if (!response.ok) {
                throw new Error('创建记录失败');
            }
        }

        // 关闭模态框并刷新数据
        bootstrap.Modal.getInstance(document.getElementById('recordModal')).hide();
        loadRecords();

    } catch (error) {
        console.error('创建失败:', error);
        alert('创建失败，请重试');
    }
}

// 删除当前record
function deleteCurrentRecord() {
    if (modalRecords.length <= 1) {
        alert('至少需要保留一个记录');
        return;
    }

    if (confirm('确定要删除当前记录吗？')) {
        const recordToDelete = modalRecords[currentModalRecordIndex];

        if (recordToDelete.id) {
            // 如果record有ID，需要从后端删除
            deleteRecordFromBackend(recordToDelete.id);
        }

        // 从modalRecords中删除
        modalRecords.splice(currentModalRecordIndex, 1);

        // 调整当前索引
        if (currentModalRecordIndex >= modalRecords.length) {
            currentModalRecordIndex = modalRecords.length - 1;
        }

        // 重新渲染
        renderRecordTabs();
        renderRecordForm();
    }
}

// 从后端删除record
async function deleteRecordFromBackend(recordId) {
    try {
        const response = await fetch(`/records/${recordId}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${localStorage.getItem('token')}`
            }
        });

        if (!response.ok) {
            throw new Error('删除记录失败');
        }
    } catch (error) {
        console.error('删除记录失败:', error);
        alert('删除记录失败，请重试');
    }
}

// 保存公司组
function saveCompanyGroup(companyGroupId) {

    // 获取公司组的record数组
    const records = currentCompanyGroupMap.get(companyGroupId);
    if (!records || records.length === 0) {
        console.error('未找到公司组记录:', companyGroupId);
        return;
    }

    // 收集表单数据
    const companyName = document.getElementById('companyName').value;
    const baseLocation = document.getElementById('baseLocation').value || null;
    const companyUrl = document.getElementById('companyUrl').value || null;
    const applyTime = document.getElementById('applyTime').value;
    const testTime = document.getElementById('testTime').value || null;
    const writtenExamTime = document.getElementById('writtenExamTime').value || null;
    const currentStatus = document.getElementById('currentStatus').value || null;
    const currentStatusDate = document.getElementById('currentStatusDate').value || null;
    const finalResult = document.getElementById('finalResult').value;
    const expectedSalaryType = document.getElementById('expectedSalaryType').value || null;
    const expectedSalaryValue = getSalaryValue() || null;
    const remarks = document.getElementById('remarks').value || null;

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

    // 获取当前编辑的record
    const currentRecordId = document.getElementById('recordId').value;
    const currentRecord = records.find(r => r.id == currentRecordId);
    if (!currentRecord) {
        console.error('未找到当前编辑的record:', currentRecordId);
        return;
    }

    // 更新当前record的岗位名称
    const positionInput = document.querySelector('#positionContainer .position-input');
    const position = positionInput ? positionInput.value.trim() : '';
    if (!position) {
        alert('请输入岗位名称');
        return;
    }

    // 准备批量保存的数据
    const updatePromises = records.map(record => {
        const recordData = {
            id: record.id,
            companyName: companyName,
            position: record.id == currentRecordId ? position : record.position, // 只有当前编辑的record更新岗位名称
            baseLocation: record.id == currentRecordId ? baseLocation : (record.baseLocation || null),
            companyUrl: record.id == currentRecordId ? companyUrl : (record.companyUrl || null),
            applyTime: applyTime,
            testTime: record.id == currentRecordId ? testTime : (record.testTime || null),
            writtenExamTime: record.id == currentRecordId ? writtenExamTime : (record.writtenExamTime || null),
            currentStatus: record.id == currentRecordId ? currentStatus : (record.currentStatus || null), // 只有当前编辑的record更新状态
            currentStatusDate: record.id == currentRecordId ? currentStatusDate : (record.currentStatusDate || null),
            finalResult: record.id == currentRecordId ? finalResult : record.finalResult, // 只有当前编辑的record更新结果
            expectedSalaryType: record.id == currentRecordId ? expectedSalaryType : (record.expectedSalaryType || null),
            expectedSalaryValue: record.id == currentRecordId ? expectedSalaryValue : (record.expectedSalaryValue || null),
            remarks: record.id == currentRecordId ? remarks : (record.remarks || null),
            interviews: record.id == currentRecordId ? interviews : (record.interviews || [])
        };

        return fetch(`${API_BASE}/records/${record.id}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${localStorage.getItem('token')}`
            },
            body: JSON.stringify(recordData)
        });
    });

    // 执行批量更新
    Promise.all(updatePromises)
        .then(responses => {
            const allSuccess = responses.every(response => response.ok);
            if (allSuccess) {
                bootstrap.Modal.getInstance(document.getElementById('recordModal')).hide();
                loadRecords(); // 重新加载数据
            } else {
                alert('部分记录保存失败，请检查');
            }
        })
        .catch(error => {
            console.error('保存错误:', error);
            alert('保存失败，请重试');
        });
}

// 保存编辑的记录
function saveEditRecord() {
    const recordId = document.getElementById('recordId').value;
    const companyName = document.getElementById('companyName').value;
    const baseLocation = document.getElementById('baseLocation').value || null;
    const companyUrl = document.getElementById('companyUrl').value || null;
    const applyTime = document.getElementById('applyTime').value;
    const testTime = document.getElementById('testTime').value || null;
    const writtenExamTime = document.getElementById('writtenExamTime').value || null;
    const currentStatus = document.getElementById('currentStatus').value || null;
    const currentStatusDate = document.getElementById('currentStatusDate').value || null;
    const finalResult = document.getElementById('finalResult').value;
    const expectedSalaryType = document.getElementById('expectedSalaryType').value || null;
    const expectedSalaryValue = getSalaryValue() || null;
    const remarks = document.getElementById('remarks').value || null;

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

    // 获取岗位名称
    const positionInput = document.querySelector('#positionContainer .position-input');
    const position = positionInput ? positionInput.value.trim() : '';

    if (!position) {
        alert('请输入岗位名称');
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

// 旧的多岗位保存函数已移除，现在使用公司组批量保存

// 保存新记录
function saveNewRecord() {
    const positionInput = document.querySelector('#positionContainer .position-input');
    const position = positionInput ? positionInput.value.trim() : '';

    if (!position) {
        alert('请输入岗位名称');
        return;
    }

    const companyName = document.getElementById('companyName').value;
    const baseLocation = document.getElementById('baseLocation').value || null;
    const companyUrl = document.getElementById('companyUrl').value || null;
    const applyTime = document.getElementById('applyTime').value;
    const testTime = document.getElementById('testTime').value || null;
    const writtenExamTime = document.getElementById('writtenExamTime').value || null;
    const currentStatus = document.getElementById('currentStatus').value || null;
    const currentStatusDate = document.getElementById('currentStatusDate').value || null;
    const finalResult = document.getElementById('finalResult').value;
    const expectedSalaryType = document.getElementById('expectedSalaryType').value || null;
    const expectedSalaryValue = getSalaryValue() || null;
    const remarks = document.getElementById('remarks').value || null;

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

    // 创建单个记录
    const recordData = {
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
    createSingleRecord(recordData, token);
}

// 创建单个记录
function createSingleRecord(recordData, token) {
    fetch(`${API_BASE}/records`, {
        method: 'POST',
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

// 旧的批量创建函数已移除，现在使用公司组管理


// 删除公司组
function deleteCompanyGroup(companyGroupId) {
    // 获取公司组的record数组
    const records = currentCompanyGroupMap.get(companyGroupId);
    if (!records || records.length === 0) {
        console.error('未找到公司组记录:', companyGroupId);
        return;
    }

    // 获取第一个record（当前显示的）
    const currentRecord = records[0];
    const companyName = currentRecord.companyName;

    if (confirm(`确定要删除公司"${companyName}"的所有记录吗？此操作不可撤销！`)) {
        const token = localStorage.getItem('token');

        // 删除所有record
        const deletePromises = records.map(record =>
            fetch(`${API_BASE}/records/${record.id}`, {
                method: 'DELETE',
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            })
        );

        Promise.all(deletePromises)
            .then(responses => {
                const allSuccess = responses.every(response => response.ok);
                if (allSuccess) {
                    // 从映射中移除公司组
                    currentCompanyGroupMap.delete(companyGroupId);
                    // 重新渲染
                    renderRecords();
                } else {
                    alert('部分记录删除失败，请检查');
                }
            })
            .catch(error => {
                console.error('删除错误:', error);
                alert('删除失败，请重试');
            });
    }
}

// 删除记录（保留用于其他用途）
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
    let recordCount = currentRecords.length;

    // 如果有筛选条件，导出筛选结果
    if (isFiltered && filteredRecords.length > 0) {
        filename = `投递记录_筛选结果_${new Date().toISOString().slice(0, 10)}.xlsx`;
        recordCount = filteredRecords.length;
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
                alert(`已导出筛选结果，共 ${recordCount} 条记录`);
            } else {
                alert(`已导出所有记录，共 ${recordCount} 条记录`);
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

// 用户岗位偏好功能已移除，现在使用公司组映射管理record顺序

// 切换record显示
async function switchPosition(companyGroupId, recordId) {

    // 确保recordId是数字类型
    const numericRecordId = parseInt(recordId);

    // 获取公司组的record数组
    const records = currentCompanyGroupMap.get(companyGroupId);
    if (!records) {
        console.error('未找到公司组:', companyGroupId);
        return;
    }

    // 找到要切换的record
    const targetRecordIndex = records.findIndex(r => r.id === numericRecordId);

    if (targetRecordIndex === -1) {
        console.error('未找到record:', numericRecordId);
        return;
    }
    // 将选中record的isPrimary属性设置为true
    records.forEach(record => {
        record.isPrimary = targetRecordIndex === records.indexOf(record);
    })
    // 将选中的record移到数组最前面
    const targetRecordList = records.splice(targetRecordIndex, 1)[0];
    records.unshift(targetRecordList);


    try {
        let updatePromises = [];
        records.forEach(record => {
            const recordData = {
                companyName: record.companyName,
                position: record.position,
                baseLocation: record.baseLocation || null,
                companyUrl: record.companyUrl || null,
                applyTime: record.applyTime,
                testTime: record.testTime || null,
                writtenExamTime: record.writtenExamTime || null,
                currentStatus: record.currentStatus || null,
                currentStatusDate: record.currentStatusDate || null,
                finalResult: record.finalResult,
                expectedSalaryType: record.expectedSalaryType || null,
                expectedSalaryValue: record.expectedSalaryValue || null,
                remarks: record.remarks || null,
                interviews: record.interviews || [],
                isPrimary: record.isPrimary
            };

            // 有ID的记录，使用PUT更新
            recordData.id = record.id;
            updatePromises.push(
                fetch(`/records/${record.id}`, {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${localStorage.getItem('token')}`
                    },
                    body: JSON.stringify(recordData)
                })
            );
        });

        // 执行所有更新和创建操作
        const responses = await Promise.all(updatePromises);

        // 检查响应
        for (const response of responses) {
            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.message || '保存记录失败');
            }
        }
        // 重新渲染记录
        buildCompanyGroupMap();
        renderRecords();
    }catch (error) {
        console.error('保存失败:', error);
        alert('保存失败: ' + error.message);
    }

}

// 显示岗位选择下拉框（编辑模式）
function showPositionSelectorForEdit(companyGroupId, currentRecord) {
    const container = document.getElementById('positionContainer');

    // 获取公司组的所有record
    const records = currentCompanyGroupMap.get(companyGroupId);
    if (!records || records.length === 0) {
        console.error('未找到公司组记录:', companyGroupId);
        return;
    }

    // 显示当前岗位输入框
    container.innerHTML = `
        <div class="input-group mb-2">
            <input type="text" class="form-control position-input" 
                   value="${currentRecord.position}" 
                   placeholder="请输入岗位名称" 
                   required>
            <button class="btn btn-outline-success" type="button" onclick="addPosition()">
                <i class="bi bi-plus"></i>
            </button>
        </div>
    `;

    // 如果有多个record，显示选择器
    if (records.length > 1) {
        const selectorHtml = `
            <div class="mb-3">
                <label class="form-label">同公司其他岗位</label>
                <select class="form-select" id="otherPositionSelector" onchange="switchToOtherPosition(this.value)">
                    <option value="">选择其他岗位进行编辑</option>
                    ${records.map(record => `
                        <option value="${record.id}" ${record.id == currentRecord.id ? 'selected' : ''}>
                            ${record.position}
                        </option>
                    `).join('')}
                </select>
                <small class="text-muted">选择其他岗位将切换到该岗位的编辑界面</small>
            </div>
        `;
        container.innerHTML += selectorHtml;
    }
}

// 这些函数已移除，现在使用公司组映射直接管理同公司record

// 切换到其他岗位
function switchToOtherPosition(recordId) {
    if (!recordId) return;

    // 获取公司组ID
    const companyGroupIdElement = document.getElementById('companyGroupId');
    const companyGroupId = companyGroupIdElement ? companyGroupIdElement.value : null;
    if (!companyGroupId) {
        console.error('未找到公司组ID');
        return;
    }

    // 获取公司组的record数组
    const records = currentCompanyGroupMap.get(companyGroupId);
    if (!records) {
        console.error('未找到公司组记录:', companyGroupId);
        return;
    }

    // 找到要切换的record
    const targetRecord = records.find(r => r.id == recordId);
    if (!targetRecord) {
        console.error('未找到record:', recordId);
        return;
    }

    // 调整record顺序（将选中的record移到最前面）
    const targetIndex = records.findIndex(r => r.id == recordId);
    if (targetIndex > 0) {
        const targetRecord = records.splice(targetIndex, 1)[0];
        records.unshift(targetRecord);
    }

    // 重新加载编辑界面
    editRecordWithCompanyGroup(companyGroupId, targetRecord);
}

// 旧的多岗位管理函数已移除，现在使用公司组映射管理

// 排序相关函数
function applySorting() {
    const sortSelect = document.getElementById('sortSelect');
    currentSortType = sortSelect.value;

    // 重新构建公司组映射并排序
    buildCompanyGroupMap();
    renderRecords();
}

// 获取结果优先级（数字越小优先级越高）
function getResultPriority(result) {
    if (!result) return 999;
    switch (result) {
        case 'OC': return 1;
        case 'PENDING': return 2;
        case '简历挂':
        case '测评挂':
        case '笔试挂':
        case '面试挂': return 3;
        default: return 4;
    }
}

// 排序records数组
function sortRecords() {
    // 对 currentCompanyGroupMap 排序（原地更新，不返回）
    currentCompanyGroupMap = new Map(
        [...currentCompanyGroupMap.entries()].sort((a, b) => {
            const recordA = a[1][0];
            const recordB = b[1][0];

            // 所有排序方式都先按 OC、PENDING、其他排序
            const priorityA = getResultPriority(recordA.finalResult);
            const priorityB = getResultPriority(recordB.finalResult);

            if (priorityA !== priorityB) {
                return priorityA - priorityB;
            }

            // 相同优先级时按具体排序类型排序
            switch (currentSortType) {
                case 'poolDays':
                    return sortByPoolDays(recordA, recordB);
                case 'updateTime':
                    return sortByUpdateTime(recordA, recordB);
                case 'applyTime':
                    return sortByApplyTime(recordA, recordB);
                case 'createdTime':
                    return sortByCreatedTime(recordA, recordB);
                default:
                    return 0; // 默认不额外排序
            }
        })
    );

}

// 按泡池时间排序（升序）
function sortByPoolDays(a, b) {
    const poolDaysA = a.poolDays || 0;
    const poolDaysB = b.poolDays || 0;
    return poolDaysA - poolDaysB;
}

// 按更新时间排序（降序，最新的在前）
function sortByUpdateTime(a, b) {
    const timeA = new Date(a.updatedAt || 0);
    const timeB = new Date(b.updatedAt || 0);
    return timeB - timeA;
}

// 按投递时间排序（降序，最新的在前）
function sortByApplyTime(a, b) {
    const timeA = new Date(a.applyTime || 0);
    const timeB = new Date(b.applyTime || 0);
    return timeB - timeA;
}

function sortByCreatedTime(a, b) {
    const timeA = new Date(a.createdAt || 0);
    const timeB = new Date(b.createdAt || 0);
    return timeB - timeA;
}

function animateReorder(container, {
    keyOf,           // (el) => string  从子节点取 key（这里用 data-key）
    build,           // () => void      把“期望最终状态”的 DOM 放进 container（尽量复用旧节点）
    onEnter,         // (el) => void    新增节点入场初始化（可选）
    onLeave          // (el) => Promise 可返回一个 Promise 完成后再移除（可选）
}) {
    const oldChildren = Array.from(container.children);
    const oldRects = new Map();
    const oldMap = new Map();

    oldChildren.forEach(el => {
        const key = keyOf(el);
        if (!key) return;
        oldMap.set(key, el);
        oldRects.set(key, el.getBoundingClientRect());
    });

    // 标记老孩子都“未使用”
    const usedOld = new Set();

    // === 构建新状态（尽量复用旧节点） ===
    const frag = document.createDocumentFragment();
    build({ reuse: (key, createEl) => {
            let el = oldMap.get(key);
            if (el) { usedOld.add(key); }
            else { el = createEl(); if (onEnter) onEnter(el); }
            el.dataset.key = key;
            frag.appendChild(el);
            return el;
        }});

    // 找出要删除的旧节点
    const toRemove = oldChildren.filter(el => !usedOld.has(keyOf(el)));

    // 先把新 DOM 塞回容器（进入“Last”阶段）
    container.innerHTML = '';
    container.appendChild(frag);

    // 计算位移
    const newChildren = Array.from(container.children);
    const invertOps = [];
    newChildren.forEach(el => {
        const key = keyOf(el);
        const last = el.getBoundingClientRect();
        const first = oldRects.get(key);
        if (first) {
            const dx = first.left - last.left;
            const dy = first.top - last.top;
            if (dx || dy) {
                el.style.transform = `translate(${dx}px, ${dy}px)`;
            }
        }
    });

    // 强制一次 reflow
    container.offsetWidth; // eslint-disable-line no-unused-expressions

    // 播放
    newChildren.forEach(el => {
        el.style.transform = '';
        el.style.opacity = '';
    });

    // 删除的做淡出后移除
    toRemove.forEach(el => {
        el.classList.add('anim-leave');
        const removeNow = () => el.remove();
        if (onLeave) {
            onLeave(el)?.finally(removeNow);
        } else {
            setTimeout(removeNow, 220);
        }
    });
}
