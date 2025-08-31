<template>
  <div class="course-list">
    <el-container>
      <!-- 顶部导航 -->
      <el-header class="header">
        <div class="header-content">
          <h1 class="page-title">{{ userRole === 'COACH' ? '我的课程' : '我的预约' }}</h1>
          <div class="header-actions">
            <el-button @click="loadBookings">刷新</el-button>
            <el-button @click="$router.push('/dashboard')">返回首页</el-button>
          </div>
        </div>
      </el-header>
      
      <!-- 主要内容 -->
      <el-main class="main-content">
        <!-- 筛选和搜索 -->
        <el-card class="filter-card">
          <el-row :gutter="20">
            <el-col :span="6">
              <el-select v-model="filters.status" placeholder="状态筛选" @change="filterBookings">
                <el-option label="全部" value=""></el-option>
                <el-option label="待确认" value="PENDING"></el-option>
                <el-option label="已确认" value="CONFIRMED"></el-option>
                <el-option label="已完成" value="COMPLETED"></el-option>
                <el-option label="已取消" value="CANCELLED"></el-option>
                <el-option label="已拒绝" value="REJECTED"></el-option>
                <el-option label="待取消确认" value="PENDING_CANCELLATION"></el-option>
              </el-select>
            </el-col>
            <el-col :span="6">
              <el-date-picker
                v-model="filters.dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                @change="filterBookings"
              />
            </el-col>
            <el-col :span="6" v-if="userRole === 'COACH'">
              <el-input
                v-model="filters.studentName"
                placeholder="搜索学员姓名"
                prefix-icon="el-icon-search"
                @input="filterBookings"
              />
            </el-col>
            <el-col :span="6" v-else>
              <el-input
                v-model="filters.coachName"
                placeholder="搜索教练姓名"
                prefix-icon="el-icon-search"
                @input="filterBookings"
              />
            </el-col>
          </el-row>
        </el-card>

        <!-- 课程列表 -->
        <el-card class="list-card">
          <div slot="header">
            <span>{{ userRole === 'COACH' ? '课程列表' : '预约列表' }} ({{ filteredBookings.length }})</span>
          </div>
          
          <div v-if="loading" class="loading-container">
            <el-loading></el-loading>
          </div>
          
          <div v-else-if="filteredBookings.length === 0" class="empty-container">
            <el-empty :description="`暂无${userRole === 'COACH' ? '课程' : '预约'}记录`"></el-empty>
          </div>
          
          <div v-else class="booking-list">
            <el-card 
              v-for="booking in paginatedBookings" 
              :key="booking.id" 
              class="booking-item"
              shadow="hover"
            >
              <div class="booking-content">
                <div class="booking-header">
                  <div class="participant-info">
                    <h4 v-if="userRole === 'COACH'">{{ booking.student.realName }}</h4>
                    <h4 v-else>{{ booking.coach.realName }}</h4>
                    <el-tag :type="getStatusTagType(booking.status)" size="small">
                      {{ getStatusText(booking.status) }}
                    </el-tag>
                  </div>
                  <div class="booking-time">
                    <i class="el-icon-time"></i>
                    {{ formatDateTime(booking.startTime) }}
                  </div>
                </div>
                
                <div class="booking-details">
                  <div class="detail-item">
                    <i class="el-icon-location"></i>
                    <span>球台 {{ booking.tableNumber }}</span>
                  </div>
                  <div class="detail-item">
                    <i class="el-icon-money"></i>
                    <span>¥{{ booking.cost }}</span>
                  </div>
                  <div class="detail-item">
                    <i class="el-icon-clock"></i>
                    <span>{{ getDuration(booking.startTime, booking.endTime) }}小时</span>
                  </div>
                </div>
                
                <div v-if="booking.remarks" class="booking-remarks">
                  <i class="el-icon-document"></i>
                  <span>{{ booking.remarks }}</span>
                </div>
                
                <div class="booking-actions">
                  <!-- 教练操作 -->
                  <template v-if="userRole === 'COACH'">
                    <el-button
                      v-if="booking.status === 'PENDING'"
                      type="success"
                      size="small"
                      @click="approveBooking(booking.id)"
                    >
                      确认预约
                    </el-button>
                    <el-button
                      v-if="booking.status === 'PENDING'"
                      type="danger"
                      size="small"
                      @click="showRejectDialog(booking)"
                    >
                      拒绝预约
                    </el-button>
                    <el-button
                      v-if="booking.status === 'CONFIRMED'"
                      type="primary"
                      size="small"
                      @click="completeBooking(booking.id)"
                    >
                      标记完成
                    </el-button>
                    <el-button
                      v-if="canCancelBooking(booking)"
                      type="warning"
                      size="small"
                      @click="cancelBooking(booking.id)"
                    >
                      申请取消
                    </el-button>
                  </template>
                  
                  <!-- 学员操作 -->
                  <template v-else>
                    <el-button
                      v-if="canCancelBooking(booking)"
                      type="danger"
                      size="small"
                      @click="cancelBooking(booking.id)"
                    >
                      申请取消
                    </el-button>
                  </template>
                  
                  <!-- 通用操作 -->
                  <el-button
                    v-if="booking.status === 'COMPLETED'"
                    type="primary"
                    size="small"
                    @click="$router.push('/evaluation')"
                  >
                    查看评价
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
              :page-sizes="[10, 20, 50, 100]"
              :page-size="pageSize"
              layout="total, sizes, prev, pager, next, jumper"
              :total="filteredBookings.length"
            />
          </div>
        </el-card>
        
        <!-- 拒绝预约对话框 -->
        <el-dialog
          title="拒绝预约"
          :visible.sync="rejectDialogVisible"
          width="500px"
        >
          <div v-if="selectedBooking">
            <p><strong>学员：</strong>{{ selectedBooking.student.realName }}</p>
            <p><strong>时间：</strong>{{ formatDateTime(selectedBooking.startTime) }} - {{ formatTime(selectedBooking.endTime) }}</p>
            <p><strong>球台：</strong>{{ selectedBooking.tableNumber }}</p>
          </div>
          <el-form style="margin-top: 20px;">
            <el-form-item label="拒绝原因" required>
              <el-input
                v-model="rejectReason"
                type="textarea"
                rows="3"
                placeholder="请输入拒绝原因"
              />
            </el-form-item>
          </el-form>
          <span slot="footer">
            <el-button @click="rejectDialogVisible = false">取消</el-button>
            <el-button type="danger" @click="rejectBooking">确认拒绝</el-button>
          </span>
        </el-dialog>
      </el-main>
    </el-container>
  </div>
</template>

<script>
import { formatDate } from '@/utils/dateFormatter'

export default {
  name: 'CourseList',
  data() {
    return {
      loading: false,
      allBookings: [], // 所有预约数据
      filteredBookings: [], // 筛选后的数据
      currentPage: 1,
      pageSize: 10,
      filters: {
        status: '',
        dateRange: [],
        studentName: '',
        coachName: ''
      },
      rejectDialogVisible: false,
      selectedBooking: null,
      rejectReason: ''
    }
  },
  
  computed: {
    userRole() {
      return this.$store.getters.userRole
    },
    
    paginatedBookings() {
      const start = (this.currentPage - 1) * this.pageSize
      const end = start + this.pageSize
      return this.filteredBookings.slice(start, end)
    }
  },
  
  mounted() {
    this.loadBookings()
  },
  
  methods: {
    async loadBookings() {
      this.loading = true
      try {
        const endpoint = this.userRole === 'COACH' ? '/api/coach/my-bookings' : '/api/course/my-bookings'
        const response = await this.$http.get(endpoint)
        this.allBookings = response.data.data || []
        this.filterBookings() // 初始化筛选
      } catch (error) {
        this.$message.error('加载数据失败')
      } finally {
        this.loading = false
      }
    },
    
    filterBookings() {
      let filtered = [...this.allBookings]
      
      // 状态筛选
      if (this.filters.status) {
        filtered = filtered.filter(booking => booking.status === this.filters.status)
      }
      
      // 日期筛选
      if (this.filters.dateRange && this.filters.dateRange.length === 2) {
        const [startDate, endDate] = this.filters.dateRange
        filtered = filtered.filter(booking => {
          const bookingDate = new Date(booking.startTime)
          return bookingDate >= startDate && bookingDate <= endDate
        })
      }
      
      // 姓名搜索
      if (this.userRole === 'COACH' && this.filters.studentName) {
        filtered = filtered.filter(booking => 
          booking.student.realName.includes(this.filters.studentName)
        )
      } else if (this.userRole === 'STUDENT' && this.filters.coachName) {
        filtered = filtered.filter(booking => 
          booking.coach.realName.includes(this.filters.coachName)
        )
      }
      
      this.filteredBookings = filtered
      this.currentPage = 1 // 重置到第一页
    },
    
    handleSizeChange(newSize) {
      this.pageSize = newSize
    },
    
    handleCurrentChange(newPage) {
      this.currentPage = newPage
    },
    
    async approveBooking(bookingId) {
      try {
        await this.$confirm('确认此预约吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        const response = await this.$http.post(`/api/coach/approve-booking/${bookingId}`)
        this.$message.success(response.data.message)
        this.loadBookings()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(error.response?.data?.message || '确认失败')
        }
      }
    },
    
    showRejectDialog(booking) {
      this.selectedBooking = booking
      this.rejectReason = ''
      this.rejectDialogVisible = true
    },
    
    async rejectBooking() {
      if (!this.rejectReason.trim()) {
        this.$message.warning('请输入拒绝原因')
        return
      }
      try {
        const response = await this.$http.post(`/api/coach/reject-booking/${this.selectedBooking.id}`, null, {
          params: { reason: this.rejectReason }
        })
        this.$message.success(response.data.message)
        this.rejectDialogVisible = false
        this.loadBookings()
      } catch (error) {
        this.$message.error(error.response?.data?.message || '拒绝失败')
      }
    },
    
    async completeBooking(bookingId) {
      try {
        await this.$confirm('确认标记此课程为已完成吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        const response = await this.$http.post(`/api/coach/complete-booking/${bookingId}`)
        this.$message.success(response.data.message)
        this.loadBookings()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(error.response?.data?.message || '操作失败')
        }
      }
    },
    
    async cancelBooking(bookingId) {
      try {
        await this.$confirm('确认申请取消此预约吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        const endpoint = this.userRole === 'COACH' 
          ? `/api/coach/cancel-booking/${bookingId}` 
          : `/api/student/cancel-booking/${bookingId}`
        const response = await this.$http.post(endpoint)
        this.$message.success(response.data.message)
        this.loadBookings()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(error.response?.data?.message || '取消失败')
        }
      }
    },
    
    canCancelBooking(booking) {
      const startTime = new Date(booking.startTime)
      const now = new Date()
      const hoursDiff = (startTime.getTime() - now.getTime()) / (1000 * 60 * 60)
      
      return (booking.status === 'PENDING' || booking.status === 'CONFIRMED') && hoursDiff >= 24
    },
    
    getDuration(startTime, endTime) {
      const start = new Date(startTime)
      const end = new Date(endTime)
      const hours = (end.getTime() - start.getTime()) / (1000 * 60 * 60)
      return hours.toFixed(1)
    },
    
    getStatusText(status) {
      const statusMap = {
        'PENDING': '待确认',
        'CONFIRMED': '已确认',
        'COMPLETED': '已完成',
        'CANCELLED': '已取消',
        'REJECTED': '已拒绝',
        'PENDING_CANCELLATION': '待取消确认'
      }
      return statusMap[status] || status
    },
    
    getStatusTagType(status) {
      const typeMap = {
        'PENDING': 'warning',
        'CONFIRMED': 'success',
        'COMPLETED': 'info',
        'CANCELLED': 'danger',
        'REJECTED': 'danger',
        'PENDING_CANCELLATION': 'warning'
      }
      return typeMap[status] || 'info'
    },
    
    formatDateTime(dateTime) {
      return formatDate(dateTime)
    },
    
    formatTime(dateTime) {
      try {
        const date = new Date(dateTime)
        return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
      } catch (error) {
        return '时间解析失败'
      }
    }
  }
}
</script>

<style scoped>
.course-list {
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
}

.main-content {
  padding: 20px;
}

.filter-card, .list-card {
  margin-bottom: 20px;
}

.booking-list {
  margin-bottom: 20px;
}

.booking-item {
  margin-bottom: 15px;
  transition: all 0.3s ease;
}

.booking-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.booking-content {
  padding: 10px 0;
}

.booking-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.participant-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.participant-info h4 {
  margin: 0;
  color: #303133;
  font-size: 16px;
}

.booking-time {
  color: #606266;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 5px;
}

.booking-details {
  display: flex;
  gap: 20px;
  margin-bottom: 10px;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 5px;
  color: #606266;
  font-size: 14px;
}

.booking-remarks {
  display: flex;
  align-items: flex-start;
  gap: 5px;
  color: #909399;
  font-size: 13px;
  margin-bottom: 15px;
  padding: 8px;
  background: #f8f9fa;
  border-radius: 4px;
}

.booking-actions {
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
  .course-list {
    padding: 10px;
  }
  
  .main-content {
    padding: 10px;
  }
  
  .booking-details {
    flex-direction: column;
    gap: 5px;
  }
  
  .booking-actions {
    flex-direction: column;
  }
  
  .booking-actions .el-button {
    width: 100%;
  }
}
</style> 