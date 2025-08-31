<template>
  <div class="system-messages">
    <el-container>
      <!-- 顶部导航 -->
      <el-header class="header">
        <div class="header-content">
          <h1 class="page-title">系统消息</h1>
          <div class="header-actions">
            <el-badge :value="unreadCount" :hidden="unreadCount === 0">
              <el-button @click="loadMessages">刷新</el-button>
            </el-badge>
            <el-button @click="markAllAsRead" :disabled="unreadCount === 0">
              全部已读
            </el-button>
            <el-button @click="$router.push('/dashboard')">返回首页</el-button>
          </div>
        </div>
      </el-header>
      
      <!-- 主要内容 -->
      <el-main class="main-content">
        <!-- 消息筛选 -->
        <el-card class="filter-card">
          <el-row :gutter="20">
            <el-col :span="6">
              <el-select v-model="filters.type" placeholder="消息类型" @change="filterMessages">
                <el-option label="全部" value=""></el-option>
                <el-option label="学员申请" value="STUDENT_APPLY"></el-option>
                <el-option label="预约申请" value="BOOKING_REQUEST"></el-option>
                <el-option label="取消申请" value="CANCEL_REQUEST"></el-option>
                <el-option label="课程提醒" value="CLASS_REMINDER"></el-option>
                <el-option label="系统通知" value="SYSTEM_NOTICE"></el-option>
              </el-select>
            </el-col>
            <el-col :span="6">
              <el-select v-model="filters.status" placeholder="阅读状态" @change="filterMessages">
                <el-option label="全部" value=""></el-option>
                <el-option label="未读" value="UNREAD"></el-option>
                <el-option label="已读" value="READ"></el-option>
              </el-select>
            </el-col>
            <el-col :span="6">
              <el-date-picker
                v-model="filters.dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                @change="filterMessages"
              />
            </el-col>
            <el-col :span="6">
              <el-button @click="clearFilters">清除筛选</el-button>
            </el-col>
          </el-row>
        </el-card>

        <!-- 消息列表 -->
        <el-card class="messages-card">
          <div slot="header">
            <span>消息列表 ({{ filteredMessages.length }})</span>
            <div class="header-stats">
              <span>未读: {{ unreadCount }}</span>
              <span>已读: {{ readCount }}</span>
            </div>
          </div>
          
          <div v-if="loading" class="loading-container">
            <el-loading text="加载消息中..."></el-loading>
          </div>
          
          <div v-else-if="filteredMessages.length === 0" class="empty-container">
            <el-empty description="暂无消息"></el-empty>
          </div>
          
          <div v-else class="message-list">
            <el-card 
              v-for="message in paginatedMessages" 
              :key="message.id" 
              class="message-item"
              :class="{ 'unread': !message.isRead, 'urgent': message.priority === 'HIGH' }"
              shadow="hover"
              @click.native="handleMessageClick(message)"
            >
              <div class="message-content">
                <div class="message-header">
                  <div class="message-title">
                    <i :class="getMessageIcon(message.type)"></i>
                    <span>{{ getMessageTitle(message.type) }}</span>
                    <el-tag 
                      v-if="message.priority === 'HIGH'" 
                      type="danger" 
                      size="mini"
                      effect="dark"
                    >
                      紧急
                    </el-tag>
                    <el-tag 
                      v-if="!message.isRead" 
                      type="warning" 
                      size="mini"
                    >
                      未读
                    </el-tag>
                  </div>
                  <div class="message-time">
                    {{ formatDateTime(message.createdAt) }}
                  </div>
                </div>
                
                <div class="message-body">
                  <div class="message-text">{{ message.content }}</div>
                  <div v-if="message.relatedData" class="message-data">
                    <span v-if="message.relatedData.studentName">
                      学员: {{ message.relatedData.studentName }}
                    </span>
                    <span v-if="message.relatedData.bookingTime">
                      时间: {{ formatDateTime(message.relatedData.bookingTime) }}
                    </span>
                    <span v-if="message.relatedData.tableNumber">
                      球台: {{ message.relatedData.tableNumber }}
                    </span>
                  </div>
                </div>
                
                <div v-if="hasActions(message)" class="message-actions">
                  <el-button
                    v-if="message.type === 'STUDENT_APPLY'"
                    type="success"
                    size="small"
                    @click.stop="approveStudentApplication(message)"
                  >
                    同意
                  </el-button>
                  <el-button
                    v-if="message.type === 'STUDENT_APPLY'"
                    type="danger"
                    size="small"
                    @click.stop="rejectStudentApplication(message)"
                  >
                    拒绝
                  </el-button>
                  <el-button
                    v-if="message.type === 'BOOKING_REQUEST'"
                    type="success"
                    size="small"
                    @click.stop="approveBooking(message)"
                  >
                    确认预约
                  </el-button>
                  <el-button
                    v-if="message.type === 'BOOKING_REQUEST'"
                    type="danger"
                    size="small"
                    @click.stop="rejectBooking(message)"
                  >
                    拒绝预约
                  </el-button>
                  <el-button
                    v-if="message.type === 'CANCEL_REQUEST'"
                    type="success"
                    size="small"
                    @click.stop="approveCancellation(message)"
                  >
                    同意取消
                  </el-button>
                  <el-button
                    v-if="message.type === 'CANCEL_REQUEST'"
                    type="danger"
                    size="small"
                    @click.stop="rejectCancellation(message)"
                  >
                    拒绝取消
                  </el-button>
                  <el-button
                    size="small"
                    @click.stop="markAsRead(message)"
                    v-if="!message.isRead"
                  >
                    标记已读
                  </el-button>
                </div>
              </div>
            </el-card>
          </div>
          
          <!-- 分页 -->
          <div class="pagination-container">
            <el-pagination
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
              :current-page="currentPage"
              :page-sizes="[10, 20, 50]"
              :page-size="pageSize"
              layout="total, sizes, prev, pager, next, jumper"
              :total="filteredMessages.length"
            />
          </div>
        </el-card>
      </el-main>
    </el-container>
  </div>
</template>

<script>
import { formatDate } from '@/utils/dateFormatter'

export default {
  name: 'SystemMessages',
  data() {
    return {
      loading: false,
      allMessages: [],
      filteredMessages: [],
      currentPage: 1,
      pageSize: 10,
      filters: {
        type: '',
        status: '',
        dateRange: []
      }
    }
  },
  
  computed: {
    unreadCount() {
      return this.allMessages.filter(msg => !msg.isRead).length
    },
    
    readCount() {
      return this.allMessages.filter(msg => msg.isRead).length
    },
    
    paginatedMessages() {
      const start = (this.currentPage - 1) * this.pageSize
      const end = start + this.pageSize
      return this.filteredMessages.slice(start, end)
    }
  },
  
  mounted() {
    this.loadMessages()
  },
  
  methods: {
    async loadMessages() {
      this.loading = true
      try {
        const response = await this.$http.get('/api/coach/messages')
        this.allMessages = response.data.data || []
        this.filterMessages()
      } catch (error) {
        this.$message.error('加载消息失败')
      } finally {
        this.loading = false
      }
    },
    
    filterMessages() {
      let filtered = [...this.allMessages]
      
      // 类型筛选
      if (this.filters.type) {
        filtered = filtered.filter(msg => msg.type === this.filters.type)
      }
      
      // 状态筛选
      if (this.filters.status) {
        if (this.filters.status === 'UNREAD') {
          filtered = filtered.filter(msg => !msg.isRead)
        } else if (this.filters.status === 'READ') {
          filtered = filtered.filter(msg => msg.isRead)
        }
      }
      
      // 日期筛选
      if (this.filters.dateRange && this.filters.dateRange.length === 2) {
        const [startDate, endDate] = this.filters.dateRange
        filtered = filtered.filter(msg => {
          const msgDate = new Date(msg.createdAt)
          return msgDate >= startDate && msgDate <= endDate
        })
      }
      
      // 按时间倒序排列
      filtered.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
      
      this.filteredMessages = filtered
      this.currentPage = 1
    },
    
    clearFilters() {
      this.filters = {
        type: '',
        status: '',
        dateRange: []
      }
      this.filterMessages()
    },
    
    handleSizeChange(newSize) {
      this.pageSize = newSize
    },
    
    handleCurrentChange(newPage) {
      this.currentPage = newPage
    },
    
    handleMessageClick(message) {
      if (!message.isRead) {
        this.markAsRead(message)
      }
    },
    
    async markAsRead(message) {
      try {
        await this.$http.post(`/api/coach/messages/${message.id}/read`)
        message.isRead = true
        this.$message.success('已标记为已读')
      } catch (error) {
        this.$message.error('操作失败')
      }
    },
    
    async markAllAsRead() {
      try {
        await this.$confirm('确认将所有未读消息标记为已读吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await this.$http.post('/api/coach/messages/read-all')
        this.allMessages.forEach(msg => msg.isRead = true)
        this.$message.success('所有消息已标记为已读')
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error('操作失败')
        }
      }
    },
    
    hasActions(message) {
      return ['STUDENT_APPLY', 'BOOKING_REQUEST', 'CANCEL_REQUEST'].includes(message.type) && !message.isRead
    },
    
    async approveStudentApplication(message) {
      try {
        await this.$http.post(`/api/coach/approve-relation/${message.relatedData.relationId}`)
        this.$message.success('已同意学员申请')
        this.markAsRead(message)
      } catch (error) {
        this.$message.error('操作失败')
      }
    },
    
    async rejectStudentApplication(message) {
      try {
        await this.$http.post(`/api/coach/reject-relation/${message.relatedData.relationId}`)
        this.$message.success('已拒绝学员申请')
        this.markAsRead(message)
      } catch (error) {
        this.$message.error('操作失败')
      }
    },
    
    async approveBooking(message) {
      try {
        await this.$http.post(`/api/coach/approve-booking/${message.relatedData.bookingId}`)
        this.$message.success('已确认预约')
        this.markAsRead(message)
      } catch (error) {
        this.$message.error('操作失败')
      }
    },
    
    async rejectBooking(message) {
      try {
        const { value: reason } = await this.$prompt('请输入拒绝原因', '拒绝预约', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          inputPattern: /.+/,
          inputErrorMessage: '请输入拒绝原因'
        })
        await this.$http.post(`/api/coach/reject-booking/${message.relatedData.bookingId}`, null, {
          params: { reason }
        })
        this.$message.success('已拒绝预约')
        this.markAsRead(message)
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error('操作失败')
        }
      }
    },
    
    async approveCancellation(message) {
      try {
        await this.$http.post(`/api/coach/confirm-cancel-booking/${message.relatedData.bookingId}`)
        this.$message.success('已同意取消')
        this.markAsRead(message)
      } catch (error) {
        this.$message.error('操作失败')
      }
    },
    
    async rejectCancellation(message) {
      try {
        const { value: reason } = await this.$prompt('请输入拒绝原因', '拒绝取消', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          inputPattern: /.+/,
          inputErrorMessage: '请输入拒绝原因'
        })
        await this.$http.post(`/api/coach/reject-cancel-booking/${message.relatedData.bookingId}`, null, {
          params: { reason }
        })
        this.$message.success('已拒绝取消申请')
        this.markAsRead(message)
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error('操作失败')
        }
      }
    },
    
    getMessageIcon(type) {
      const iconMap = {
        'STUDENT_APPLY': 'el-icon-user-solid',
        'BOOKING_REQUEST': 'el-icon-date',
        'CANCEL_REQUEST': 'el-icon-close',
        'CLASS_REMINDER': 'el-icon-bell',
        'SYSTEM_NOTICE': 'el-icon-info'
      }
      return iconMap[type] || 'el-icon-message'
    },
    
    getMessageTitle(type) {
      const titleMap = {
        'STUDENT_APPLY': '学员申请',
        'BOOKING_REQUEST': '预约申请',
        'CANCEL_REQUEST': '取消申请',
        'CLASS_REMINDER': '课程提醒',
        'SYSTEM_NOTICE': '系统通知'
      }
      return titleMap[type] || '消息'
    },
    
    formatDateTime(dateTime) {
      return formatDate(dateTime)
    }
  }
}
</script>

<style scoped>
.system-messages {
  padding: 20px;
  background: #f5f5f5;
  min-height: 100vh;
}

.header {
  background: white;
  border-bottom: 1px solid #ebeef5;
  padding: 0 20px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
}

.page-title {
  margin: 0;
  color: #303133;
  font-size: 24px;
  font-weight: 500;
}

.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.header-stats {
  float: right;
  color: #909399;
  font-size: 14px;
}

.header-stats span {
  margin-left: 15px;
}

.main-content {
  padding: 20px;
}

.filter-card, .messages-card {
  margin-bottom: 20px;
}

.message-list {
  margin-bottom: 20px;
}

.message-item {
  margin-bottom: 15px;
  cursor: pointer;
  transition: all 0.3s ease;
  border-left: 4px solid transparent;
}

.message-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.message-item.unread {
  border-left-color: #409eff;
  background: #f0f9ff;
}

.message-item.urgent {
  border-left-color: #f56c6c;
}

.message-content {
  padding: 5px 0;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.message-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  color: #303133;
}

.message-title i {
  font-size: 16px;
  color: #409eff;
}

.message-time {
  color: #909399;
  font-size: 13px;
}

.message-body {
  margin-bottom: 15px;
}

.message-text {
  color: #606266;
  line-height: 1.6;
  margin-bottom: 8px;
}

.message-data {
  display: flex;
  gap: 15px;
  color: #909399;
  font-size: 13px;
}

.message-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.loading-container, .empty-container {
  text-align: center;
  padding: 40px 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .system-messages {
    padding: 10px;
  }
  
  .main-content {
    padding: 10px;
  }
  
  .message-actions {
    flex-direction: column;
  }
  
  .message-actions .el-button {
    width: 100%;
  }
  
  .message-data {
    flex-direction: column;
    gap: 5px;
  }
}
</style> 