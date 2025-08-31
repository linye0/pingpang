<template>
  <div class="coach-schedule">
    <el-container>
      <!-- 顶部导航 -->
      <el-header class="header">
        <div class="header-content">
          <h1 class="page-title">我的课表</h1>
          <div class="header-actions">
            <el-button @click="loadSchedule">刷新</el-button>
            <el-button @click="$router.push('/dashboard')">返回首页</el-button>
          </div>
        </div>
      </el-header>
      
      <!-- 主要内容 -->
      <el-main class="main-content">
        <!-- 日期选择器 -->
        <el-card class="date-selector-card">
          <el-row :gutter="20" type="flex" align="middle">
            <el-col :span="8">
              <el-date-picker
                v-model="selectedWeek"
                type="week"
                format="第 WW 周"
                placeholder="选择周"
                @change="loadSchedule"
                style="width: 100%"
              />
            </el-col>
            <el-col :span="8">
              <div class="week-info">
                <span>{{ getWeekRange() }}</span>
              </div>
            </el-col>
            <el-col :span="8">
              <el-button-group>
                <el-button @click="previousWeek">上一周</el-button>
                <el-button @click="currentWeek">本周</el-button>
                <el-button @click="nextWeek">下一周</el-button>
              </el-button-group>
            </el-col>
          </el-row>
        </el-card>

        <!-- 课表视图 -->
        <el-card class="schedule-card">
          <div slot="header">
            <span>课表安排</span>
            <span class="schedule-stats">
              共 {{ totalBookings }} 节课，{{ confirmedBookings }} 节已确认
            </span>
          </div>
          
          <div 
            class="schedule-table"
            v-loading="loading"
            element-loading-text="加载课表中..."
          >
            <!-- 时间轴表头 -->
            <div class="schedule-header">
              <div class="schedule-info">
                <span class="date-range">{{ getWeekRange() }}</span>
                <span class="course-count">共 {{ weekBookings.length }} 节课</span>
              </div>
              <div class="time-column">时间</div>
              <div 
                v-for="day in weekDays" 
                :key="day.date.getTime()"
                class="day-column"
                :class="{ 'today': isToday(day.date) }"
              >
                <div class="day-name">{{ day.name }}</div>
                <div class="day-date">{{ formatDate(day.date) }}</div>
              </div>
            </div>
            
            <!-- 时间段行 -->
            <div class="schedule-body">
              <div 
                v-for="slot in timeSlots" 
                :key="slot.value"
                class="time-row"
              >
                <div class="time-cell">
                  {{ slot.label }}
                </div>
                <div 
                  v-for="day in weekDays" 
                  :key="`${day.date.getTime()}-${slot.value}`"
                  class="booking-cell"
                  :class="{ 'has-booking': getBookingForSlot(day.date, slot) }"
                >
                  <div 
                    v-if="getBookingForSlot(day.date, slot)"
                    class="booking-item"
                    :class="`status-${getBookingForSlot(day.date, slot).status.toLowerCase()}`"
                    @click="showBookingDetails(getBookingForSlot(day.date, slot))"
                  >
                    <div class="student-name">
                      {{ getBookingForSlot(day.date, slot).student.realName }}
                    </div>
                    <div class="booking-time">
                      {{ formatTime(getBookingForSlot(day.date, slot).startTime) }} - {{ formatTime(getBookingForSlot(day.date, slot).endTime) }}
                    </div>
                    <div class="booking-info">
                      <span class="table-number">{{ getBookingForSlot(day.date, slot).tableNumber }}</span>
                      <span class="booking-status">
                        {{ getStatusText(getBookingForSlot(day.date, slot).status) }}
                      </span>
                    </div>
                  </div>
                  <div v-else class="empty-slot">
                    <span>空闲</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-card>

        <!-- 课程详情对话框 -->
        <el-dialog
          title="课程详情"
          :visible.sync="detailDialogVisible"
          width="600px"
        >
          <div v-if="selectedBooking" class="booking-details">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="学员姓名">
                {{ selectedBooking.student.realName }}
              </el-descriptions-item>
              <el-descriptions-item label="学员电话">
                {{ selectedBooking.student.phone }}
              </el-descriptions-item>
              <el-descriptions-item label="上课时间">
                {{ formatDateTime(selectedBooking.startTime) }} - {{ formatTime(selectedBooking.endTime) }}
              </el-descriptions-item>
              <el-descriptions-item label="球台">
                {{ selectedBooking.tableNumber }}
              </el-descriptions-item>
              <el-descriptions-item label="课程费用">
                ¥{{ selectedBooking.cost }}
              </el-descriptions-item>
              <el-descriptions-item label="预约状态">
                <el-tag :type="getStatusTagType(selectedBooking.status)">
                  {{ getStatusText(selectedBooking.status) }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="备注" :span="2">
                {{ selectedBooking.remarks || '无' }}
              </el-descriptions-item>
              <el-descriptions-item label="预约时间" :span="2">
                {{ formatDateTime(selectedBooking.createdAt) }}
              </el-descriptions-item>
            </el-descriptions>
            
            <!-- 操作按钮 -->
            <div class="dialog-actions">
              <el-button
                v-if="selectedBooking.status === 'PENDING'"
                type="success"
                @click="approveBooking(selectedBooking.id)"
              >
                确认预约
              </el-button>
              <el-button
                v-if="selectedBooking.status === 'PENDING'"
                type="danger"
                @click="showRejectDialog(selectedBooking.id)"
              >
                拒绝预约
              </el-button>
              <el-button
                v-if="selectedBooking.status === 'CONFIRMED' && isPastTime(selectedBooking.endTime)"
                type="primary"
                @click="completeLesson(selectedBooking.id)"
              >
                完成课程
              </el-button>
              <el-button
                v-if="selectedBooking.status === 'COMPLETED'"
                type="info"
                disabled
              >
                课程已完成
              </el-button>
            </div>
          </div>
        </el-dialog>
        
        <!-- 拒绝预约对话框 -->
        <el-dialog
          title="拒绝预约"
          :visible.sync="rejectDialogVisible"
          width="500px"
        >
          <el-form>
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
  name: 'CoachSchedule',
  data() {
    return {
      loading: false,
      selectedWeek: new Date(),
      weekBookings: [],
      detailDialogVisible: false,
      selectedBooking: null,
      rejectDialogVisible: false,
      rejectReason: '',
      timeSlots: [
        { label: '09:00-10:00', value: '09:00-10:00', start: 9, end: 10 },
        { label: '10:00-11:00', value: '10:00-11:00', start: 10, end: 11 },
        { label: '11:00-12:00', value: '11:00-12:00', start: 11, end: 12 },
        { label: '14:00-15:00', value: '14:00-15:00', start: 14, end: 15 },
        { label: '15:00-16:00', value: '15:00-16:00', start: 15, end: 16 },
        { label: '16:00-17:00', value: '16:00-17:00', start: 16, end: 17 },
        { label: '17:00-18:00', value: '17:00-18:00', start: 17, end: 18 },
        { label: '19:00-20:00', value: '19:00-20:00', start: 19, end: 20 },
        { label: '20:00-21:00', value: '20:00-21:00', start: 20, end: 21 }
      ]
    }
  },
  
  computed: {
    weekDays() {
      const startOfWeek = this.getStartOfWeek(this.selectedWeek)
      const days = []
      const dayNames = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
      
      for (let i = 0; i < 7; i++) {
        const date = new Date(startOfWeek)
        date.setDate(startOfWeek.getDate() + i)
        days.push({
          date: new Date(date),
          name: dayNames[i]
        })
      }
      return days
    },
    
    totalBookings() {
      return this.weekBookings.length
    },
    
    confirmedBookings() {
      return this.weekBookings.filter(booking => booking.status === 'CONFIRMED').length
    },

    weekNumber() {
      const startOfWeek = this.getStartOfWeek(this.selectedWeek)
      const d = new Date(startOfWeek)
      const weekNumber = Math.ceil((d.getMonth() + 1) / 4) // 假设每个月有4周
      return weekNumber
    }
  },
  
  mounted() {
    this.loadSchedule()
  },
  
  methods: {
    async loadSchedule() {
      this.loading = true
      try {
        const startOfWeek = this.getStartOfWeek(this.selectedWeek)
        const endOfWeek = new Date(startOfWeek)
        endOfWeek.setDate(startOfWeek.getDate() + 6)
        endOfWeek.setHours(23, 59, 59, 999)
        
        console.log('课表查询时间范围:', {
          start: startOfWeek.toISOString(),
          end: endOfWeek.toISOString(),
          startLocal: startOfWeek.toLocaleString(),
          endLocal: endOfWeek.toLocaleString()
        })
        
        // 使用与Dashboard相同的API端点
        const response = await this.$http.get('/api/coach/my-bookings')
        const allBookings = response.data.data || []
        
        console.log('获取到的所有课程数据:', allBookings)
        
        // 在前端过滤本周的课程
        this.weekBookings = allBookings.filter(booking => {
          const bookingTime = new Date(booking.startTime)
          return bookingTime >= startOfWeek && bookingTime <= endOfWeek
        })
        
        console.log('过滤后的本周课程:', this.weekBookings)
      } catch (error) {
        console.error('加载课表失败:', error)
        this.$message.error('加载课表失败')
      } finally {
        this.loading = false
      }
    },
    
    getStartOfWeek(date) {
      const d = new Date(date)
      const day = d.getDay()
      const diff = d.getDate() - day + (day === 0 ? -6 : 1) // 周一为一周开始
      const monday = new Date(d.setDate(diff))
      monday.setHours(0, 0, 0, 0)
      return monday
    },
    
    getWeekRange() {
      const startOfWeek = this.getStartOfWeek(this.selectedWeek)
      const endOfWeek = new Date(startOfWeek)
      endOfWeek.setDate(startOfWeek.getDate() + 6)
      
      return `${this.formatShortDate(startOfWeek)} - ${this.formatShortDate(endOfWeek)}`
    },
    
    getBookingForSlot(date, slot) {
      const booking = this.weekBookings.find(booking => {
        const bookingDate = new Date(booking.startTime)
        const bookingHour = bookingDate.getHours()
        const sameDay = this.isSameDay(bookingDate, date)
        const sameTimeSlot = bookingHour === slot.start
        
        // 调试信息
        if (sameDay) {
          console.log('课程匹配检查:', {
            bookingId: booking.id,
            bookingDate: bookingDate.toLocaleString(),
            slotDate: date.toLocaleString(),
            bookingHour,
            slotHour: slot.start,
            sameDay,
            sameTimeSlot,
            matched: sameDay && sameTimeSlot
          })
        }
        
        return sameDay && sameTimeSlot
      })
      
      return booking
    },
    
    isSameDay(date1, date2) {
      return date1.getFullYear() === date2.getFullYear() &&
             date1.getMonth() === date2.getMonth() &&
             date1.getDate() === date2.getDate()
    },
    
    isToday(date) {
      return this.isSameDay(date, new Date())
    },
    
    previousWeek() {
      const newDate = new Date(this.selectedWeek)
      newDate.setDate(newDate.getDate() - 7)
      this.selectedWeek = newDate
      this.loadSchedule()
    },
    
    currentWeek() {
      this.selectedWeek = new Date()
      this.loadSchedule()
    },
    
    nextWeek() {
      const newDate = new Date(this.selectedWeek)
      newDate.setDate(newDate.getDate() + 7)
      this.selectedWeek = newDate
      this.loadSchedule()
    },
    
    showBookingDetails(booking) {
      this.selectedBooking = booking
      this.detailDialogVisible = true
    },
    
    async approveBooking(bookingId) {
      try {
        const response = await this.$http.post(`/api/coach/approve-booking/${bookingId}`)
        this.$message.success(response.data.message)
        this.detailDialogVisible = false
        this.loadSchedule()
      } catch (error) {
        this.$message.error(error.response?.data?.message || '确认失败')
      }
    },
    
    showRejectDialog(booking) {
      this.selectedBooking = booking
      this.rejectReason = ''
      this.detailDialogVisible = false
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
        this.loadSchedule()
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
        this.detailDialogVisible = false
        this.loadSchedule()
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
        const response = await this.$http.post(`/api/coach/cancel-booking/${bookingId}`)
        this.$message.success(response.data.message)
        this.detailDialogVisible = false
        this.loadSchedule()
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
    
    getStatusText(status) {
      const statusMap = {
        'PENDING': '待确认',
        'CONFIRMED': '已确认',
        'COMPLETED': '已完成',
        'CANCELLED': '已取消',
        'REJECTED': '已拒绝'
      }
      return statusMap[status] || status
    },
    
    getStatusTagType(status) {
      const typeMap = {
        'PENDING': 'warning',
        'CONFIRMED': 'success',
        'COMPLETED': 'info',
        'CANCELLED': 'danger',
        'REJECTED': 'danger'
      }
      return typeMap[status] || 'info'
    },
    
    formatDate(date) {
      return formatDate(date, 'MM-DD')
    },
    
    formatShortDate(date) {
      return formatDate(date, 'YYYY-MM-DD')
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
    },

    isPastTime(dateTime) {
      const bookingEndTime = new Date(dateTime);
      const now = new Date();
      return bookingEndTime < now;
    },

         async completeLesson(bookingId) {
       try {
         await this.$confirm('确认标记此课程为已完成吗？', '提示', {
           confirmButtonText: '确定',
           cancelButtonText: '取消',
           type: 'warning'
         });
         const response = await this.$http.post(`/api/coach/complete-booking/${bookingId}`);
         this.$message.success(response.data.message || '课程已标记为完成');
         this.detailDialogVisible = false;
         this.loadSchedule();
             } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(error.response?.data?.message || '操作失败');
        }
      }
    },
    
    async createTestData() {
      this.creatingTestData = true
      try {
        const response = await this.$http.post('/api/coach/debug/create-test-data')
        this.$message.success(response.data.message || '测试数据创建成功')
        // 重新加载课表
        await this.loadSchedule()
      } catch (error) {
        console.error('创建测试数据失败:', error)
        this.$message.error(error.response?.data?.message || '创建测试数据失败')
      } finally {
        this.creatingTestData = false
      }
    },
    
    async showDebugInfo() {
      try {
        const response = await this.$http.get('/api/coach/debug')
        const debugData = response.data.data
        
        let message = `调试信息:
教练ID: ${debugData.coachId}
课程总数: ${debugData.totalBookings}
本周课程: ${this.weekBookings.length}
今日课程: ${debugData.todayBookings}
待确认: ${debugData.pendingBookings}`
        
        this.$alert(message, '调试信息', {
          confirmButtonText: '确定',
          type: 'info'
        })
      } catch (error) {
        console.error('获取调试信息失败:', error)
        this.$message.error('获取调试信息失败')
      }
    }
  }
}
</script>

<style scoped>
.coach-schedule {
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

.date-selector-card, .schedule-card {
  margin-bottom: 20px;
}

.week-info {
  text-align: center;
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}

.schedule-stats {
  float: right;
  color: #909399;
  font-size: 14px;
}

.schedule-table {
  overflow-x: auto;
}

.schedule-header {
  display: flex;
  background: #f8f9fa;
  border-radius: 4px 4px 0 0;
  border: 1px solid #ebeef5;
}

.schedule-info {
  display: flex;
  align-items: center;
  margin-right: 20px;
  font-size: 14px;
  color: #606266;
}

.current-week {
  font-weight: 500;
  color: #303133;
  margin-right: 10px;
}

.date-range {
  margin-right: 10px;
}

.course-count {
  font-weight: 500;
  color: #409eff;
}

.time-column {
  width: 120px;
  padding: 15px 10px;
  text-align: center;
  font-weight: 500;
  color: #303133;
  border-right: 1px solid #ebeef5;
}

.day-column {
  flex: 1;
  min-width: 140px;
  padding: 15px 10px;
  text-align: center;
  border-right: 1px solid #ebeef5;
  background: white;
  color: #333;
  transition: all 0.3s ease;
}

.day-column:last-child {
  border-right: none;
}

.day-column.today {
  background: #ecf5ff;
  color: #409eff;
  box-shadow: 0 1px 3px rgba(64, 158, 255, 0.2);
}

.day-name {
  font-weight: 600;
  margin-bottom: 5px;
  font-size: 16px;
  color: #333;
}

.day-column.today .day-name {
  color: #409eff;
}

.day-date {
  font-size: 14px;
  color: #666;
  font-weight: 500;
}

.day-column.today .day-date {
  color: #409eff;
}

.schedule-body {
  border: 1px solid #ebeef5;
  border-top: none;
}

.time-row {
  display: flex;
  border-bottom: 1px solid #ebeef5;
}

.time-row:last-child {
  border-bottom: none;
}

.time-cell {
  width: 120px;
  padding: 15px 10px;
  text-align: center;
  background: #fafafa;
  border-right: 1px solid #ebeef5;
  font-size: 14px;
  color: #606266;
}

.booking-cell {
  flex: 1;
  min-width: 140px;
  min-height: 80px;
  border-right: 1px solid #ebeef5;
  position: relative;
  cursor: pointer;
  transition: all 0.3s ease;
  padding: 4px;
}

.booking-cell:last-child {
  border-right: none;
}

.booking-cell:hover {
  background: #f5f7fa;
}

.booking-cell.has-booking:hover .booking-item {
  transform: scale(1.02);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.booking-item {
  width: calc(100% - 8px);
  height: calc(100% - 8px);
  padding: 10px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  text-align: center;
  border-radius: 8px;
  margin: 4px;
  color: white;
  font-size: 13px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  cursor: pointer;
}

.booking-item.status-pending {
  background: linear-gradient(135deg, #f39c12, #e67e22);
}

.booking-item.status-confirmed {
  background: linear-gradient(135deg, #27ae60, #2ecc71);
}

.booking-item.status-completed {
  background: linear-gradient(135deg, #7f8c8d, #95a5a6);
}

.booking-item.status-cancelled {
  background: linear-gradient(135deg, #e74c3c, #c0392b);
}

.booking-item.status-rejected {
  background: linear-gradient(135deg, #e74c3c, #c0392b);
  opacity: 0.8;
}

.student-name {
  font-weight: 600;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 14px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}

.booking-time {
  font-size: 11px;
  margin-bottom: 4px;
  opacity: 0.85;
  font-weight: 500;
}

.booking-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 11px;
  opacity: 0.9;
}

.booking-status {
  background: rgba(255, 255, 255, 0.3);
  padding: 2px 6px;
  border-radius: 12px;
  font-size: 10px;
  font-weight: 500;
}

.empty-slot {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  font-size: 12px;
  border: 2px dashed #e4e7ed;
  border-radius: 8px;
  margin: 4px;
  background: #fafafa;
  transition: all 0.3s ease;
}

.empty-slot:hover {
  border-color: #409eff;
  color: #409eff;
  background: #f0f9ff;
}

.dialog-actions {
  margin-top: 20px;
  text-align: center;
}

.dialog-actions .el-button {
  margin: 0 5px;
}

.schedule-header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.header-actions .el-button {
  border-radius: 6px;
}

.loading-container {
  text-align: center;
  padding: 40px 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .coach-schedule {
    padding: 10px;
  }
  
  .main-content {
    padding: 10px;
  }
  
  .time-column, .day-column, .time-cell, .booking-cell {
    min-width: 80px;
  }
  
  .booking-item {
    font-size: 10px;
  }
}
</style> 