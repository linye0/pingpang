<template>
  <div class="course-booking">
    <el-container>
      <!-- 顶部导航 -->
      <el-header class="header">
        <div class="header-content">
          <h1 class="page-title">{{ userRole === 'COACH' ? '课程安排' : '课程预约' }}</h1>
          <el-button @click="$router.push('/dashboard')">返回首页</el-button>
        </div>
      </el-header>
      
      <!-- 主要内容 -->
      <el-main class="main-content">
        <el-row :gutter="20">
          <!-- 左侧：学员预约表单 / 教练待处理预约 -->
          <el-col :span="16">
            <!-- 学员预约表单 -->
            <el-card v-if="userRole === 'STUDENT'" class="booking-form-card">
              <div slot="header">
                <span>新建预约</span>
              </div>
              
              <el-form :model="bookingForm" :rules="rules" ref="bookingForm" label-width="120px">
                <!-- 学员选择教练 -->
                <el-form-item label="选择教练" prop="coachId">
                  <el-select 
                    v-model="bookingForm.coachId" 
                    placeholder="请选择教练"
                    style="width: 100%"
                    @change="loadCoachSchedule"
                  >
                    <el-option 
                      v-for="relation in myCoaches" 
                      :key="relation.coach.id"
                      :label="`${relation.coach.realName} (${getLevelText(relation.coach.level)})`"
                      :value="relation.coach.id"
                      :disabled="relation.status !== 'APPROVED'"
                    />
                  </el-select>
                </el-form-item>

                <el-form-item label="预约日期" prop="date">
                  <el-date-picker
                    v-model="bookingForm.date"
                    type="date"
                    placeholder="选择日期"
                    style="width: 100%"
                    :picker-options="datePickerOptions"
                    @change="loadCoachSchedule"
                  />
                </el-form-item>
                
                <el-form-item label="时间段" prop="timeSlot">
                  <el-select 
                    v-model="bookingForm.timeSlot" 
                    placeholder="请选择时间段"
                    style="width: 100%"
                    clearable
                    filterable
                    @change="onTimeSlotChange"
                    @clear="onTimeSlotClear"
                    @visible-change="onTimeSlotDropdownChange"
                  >
                    <el-option 
                      v-for="slot in availableTimeSlots" 
                      :key="slot.value"
                      :label="slot.label"
                      :value="slot.value"
                    />
                  </el-select>
                  <div style="font-size: 12px; color: #999; margin-top: 4px;">
                    当前值: {{ bookingForm.timeSlot || '未选择' }} | 
                    可选项: {{ availableTimeSlots.length }}个 |
                    日期: {{ bookingForm.date || '未选择' }} |
                    教练: {{ bookingForm.coachId || '未选择' }}
                  </div>
                  <div v-if="availableTimeSlots.length === 0" style="font-size: 12px; color: #f56c6c; margin-top: 4px;">
                    ⚠️ 没有可用时间段 - 请先选择教练和日期
                  </div>
                </el-form-item>
                
                <el-form-item label="球台" prop="tableNumber">
                  <el-select 
                    v-model="bookingForm.tableNumber" 
                    placeholder="系统自动分配或手动选择"
                    style="width: 100%"
                    clearable
                  >
                    <el-option 
                      v-for="table in availableTables" 
                      :key="table"
                      :label="`球台 ${table}`"
                      :value="table"
                    />
                  </el-select>
                </el-form-item>
                
                <el-form-item label="备注" prop="remarks">
                  <el-input 
                    v-model="bookingForm.remarks"
                    type="textarea"
                    placeholder="课程备注（可选）"
                    rows="3"
                  />
                </el-form-item>
                
                <el-form-item>
                  <el-button 
                    type="primary" 
                    @click="submitBooking"
                    :loading="submitting"
                    :disabled="!canSubmitBooking"
                  >
                    提交预约
                  </el-button>
                  <el-button @click="resetForm">重置</el-button>
                </el-form-item>
              </el-form>
            </el-card>

            <!-- 教练待处理预约 -->
            <el-card v-if="userRole === 'COACH'" class="pending-bookings-card">
              <div slot="header">
                <span>待处理预约申请</span>
                <el-button 
                  style="float: right; padding: 3px 0" 
                  type="text" 
                  @click="loadPendingBookings"
                >
                  刷新
                </el-button>
              </div>
              
              <div v-if="pendingBookings.length === 0" class="empty-container">
                <el-empty description="暂无待处理预约申请"></el-empty>
              </div>
              
              <div v-else>
                <el-card 
                  v-for="booking in pendingBookings" 
                  :key="booking.id" 
                  class="booking-card"
                  style="margin-bottom: 10px;"
                >
                  <div class="booking-info">
                    <div class="booking-header">
                      <h4>{{ booking.student.realName }} 的预约申请</h4>
                      <el-tag type="warning">待确认</el-tag>
                    </div>
                    <div class="booking-details">
                      <div class="booking-time">
                        <i class="el-icon-time"></i>
                        {{ formatBookingTime(booking.startTime, booking.endTime) }}
                      </div>
                      <p>球台 {{ booking.tableNumber }}</p>
                    </div>
                    <div class="booking-actions">
                      <el-button 
                        type="success" 
                        size="small" 
                        @click="approveBooking(booking.id)"
                      >
                        确认预约
                      </el-button>
                      <el-button 
                        type="danger" 
                        size="small" 
                        @click="showRejectDialog(booking)"
                      >
                        拒绝预约
                      </el-button>
                    </div>
                  </div>
                </el-card>
              </div>
            </el-card>
          </el-col>
          
          <!-- 右侧：信息面板 -->
          <el-col :span="8">
            <!-- 账户余额（仅学员显示） -->
            <el-card v-if="userRole === 'STUDENT'" class="balance-card">
              <div slot="header">
                <span>账户余额</span>
              </div>
              <div class="balance-info">
                <span class="balance-amount">¥{{ accountBalance.toFixed(2) }}</span>
                <el-button type="text" @click="$router.push('/payment')">充值</el-button>
              </div>
            </el-card>

            <!-- 我的预约 -->
            <el-card class="my-bookings-card">
              <div slot="header">
                <span>{{ userRole === 'COACH' ? '我的课程' : '我的预约' }}</span>
                <el-button 
                  style="float: right; padding: 3px 0" 
                  type="text" 
                  @click="loadMyBookings"
                >
                  刷新
                </el-button>
              </div>
              
              <div v-if="myBookings.length === 0" class="empty-container">
                <el-empty :description="`暂无${userRole === 'COACH' ? '课程' : '预约'}记录`"></el-empty>
              </div>
              
              <div v-else class="booking-list">
                <el-card 
                  v-for="booking in myBookings.slice(0, 5)" 
                  :key="booking.id" 
                  class="booking-item"
                  style="margin-bottom: 10px;"
                >
                  <div class="booking-info">
                    <div class="booking-header">
                      <span class="student-name" v-if="userRole === 'COACH'">{{ booking.student.realName }}</span>
                      <span class="coach-name" v-else>{{ booking.coach.realName }}</span>
                      <el-tag :type="getStatusTagType(booking.status)" size="small">
                        {{ getStatusText(booking.status) }}
                      </el-tag>
                    </div>
                    <div class="booking-details">
                      <p>{{ formatBookingDateTime(booking.startTime) }}</p>
                      <p>球台 {{ booking.tableNumber }}</p>
                    </div>
                  </div>
                </el-card>
                
                <el-button type="text" @click="$router.push('/course/list')">
                  查看更多
                </el-button>
              </div>
            </el-card>
          </el-col>
        </el-row>
        
        <!-- 拒绝预约对话框 -->
        <el-dialog
          title="拒绝预约"
          :visible.sync="rejectDialogVisible"
          width="500px"
          @close="rejectDialogVisible = false"
        >
          <div v-if="selectedPendingBooking">
            <p><strong>学员：</strong>{{ selectedPendingBooking.student.realName }}</p>
            <p><strong>时间：</strong>{{ formatBookingDateTime(selectedPendingBooking.startTime) }} - {{ formatBookingTimeShort(selectedPendingBooking.endTime) }}</p>
            <p><strong>球台：</strong>{{ selectedPendingBooking.tableNumber || '系统自动分配' }}</p>
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
import { formatDate, formatBookingTime, formatDateTime, formatDateTimeShort } from '@/utils/dateFormatter'

export default {
  name: 'CourseBooking',
  
  data() {
    return {
      bookingForm: {
        coachId: null,
        date: null,
        timeSlot: null,
        tableNumber: null,
        remarks: ''
      },
      rules: {
        coachId: [
          { required: true, message: '请选择教练', trigger: 'change' }
        ],
        date: [
          { required: true, message: '请选择日期', trigger: 'change' }
        ],
        timeSlot: [
          { required: true, message: '请选择时间段', trigger: 'change' }
        ]
      },
      myCoaches: [],
      myBookings: [],
      coachSchedule: [],
      availableTables: [],
      accountBalance: 0,
      submitting: false,
      currentCoachProfile: null, // 当前教练的完整资料
      datePickerOptions: {
        disabledDate(time) {
          return time.getTime() < Date.now() - 8.64e7 // 不能选择过去的日期
        }
      },
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
      ],
      pendingBookings: [], // 新增：待处理预约申请列表
      rejectDialogVisible: false, // 新增：拒绝预约对话框可见性
      rejectReason: '', // 新增：拒绝预约原因
      selectedPendingBooking: null // 新增：当前选中的待处理预约
    }
  },
  
  computed: {
    userRole() {
      return this.$store.getters.userRole
    },
    
    currentUser() {
      return this.$store.state.user
    },
    
    availableTimeSlots() {
      console.log('=== 计算可用时间段（新版本） ===')
      console.log('当前日期:', this.bookingForm.date)
      console.log('当前教练ID:', this.bookingForm.coachId)
      
      // 检查教练课表数据
      if (!this.coachSchedule || !this.coachSchedule.availableSlots) {
        console.log('没有教练课表数据，返回空数组')
        return []
      }
      
      const availableSlots = this.coachSchedule.availableSlots || []
      console.log('从API获取的可预约时间段数量:', availableSlots.length)
      console.log('原始时间段数据:', availableSlots)
      
      // 转换为前端需要的格式
      const convertedSlots = availableSlots.map(slot => {
        try {
          // 更安全的时间解析
          let startTime, endTime
          
          if (typeof slot.startTime === 'string') {
            // 如果是字符串，尝试解析
            startTime = new Date(slot.startTime.replace('Z', ''))
          } else if (slot.startTime instanceof Date) {
            startTime = slot.startTime
          } else {
            console.error('无效的开始时间格式:', slot.startTime)
            return null
          }
          
          if (typeof slot.endTime === 'string') {
            endTime = new Date(slot.endTime.replace('Z', ''))
          } else if (slot.endTime instanceof Date) {
            endTime = slot.endTime
          } else {
            console.error('无效的结束时间格式:', slot.endTime)
            return null
          }
          
          // 验证 Date 对象是否有效
          if (isNaN(startTime.getTime()) || isNaN(endTime.getTime())) {
            console.error('时间解析失败:', { 
              startTime: slot.startTime, 
              endTime: slot.endTime,
              parsedStart: startTime,
              parsedEnd: endTime
            })
            return null
          }
          
          return {
            value: `${startTime.getHours().toString().padStart(2, '0')}:00-${endTime.getHours().toString().padStart(2, '0')}:00`,
            label: `${this.formatTime(startTime)} - ${this.formatTime(endTime)}`,
            start: startTime.getHours(),
            end: endTime.getHours(),
            startTime: slot.startTime,
            endTime: slot.endTime,
            workingTimeId: slot.workingTimeId,
            remarks: slot.remarks
          }
        } catch (error) {
          console.error('时间段转换失败:', error, slot)
          return null
        }
      }).filter(slot => slot !== null) // 过滤掉转换失败的
        .sort((a, b) => a.start - b.start) // 按时间排序
      
      console.log('转换后的可预约时间段:', convertedSlots.map(slot => slot.label))
      console.log('最终可用时间段数量:', convertedSlots.length)
      
      return convertedSlots
    },
    
    selectedCoach() {
      if (this.userRole === 'COACH') {
        return this.currentCoachProfile || this.currentUser
      }
      return this.myCoaches.find(relation => relation.coach.id === this.bookingForm.coachId)?.coach
    },
    
    estimatedCost() {
      if (!this.selectedCoach || !this.bookingForm.timeSlot) {
        return '0.00'
      }
      const rate = this.getLevelRate(this.selectedCoach.level)
      return (rate * this.duration).toFixed(2)
    },
    
    duration() {
      return 1 // 固定1小时
    },
    
    canSubmitBooking() {
      const baseCondition = this.bookingForm.coachId && this.bookingForm.date && this.bookingForm.timeSlot
      
      if (this.userRole === 'STUDENT') {
        // 学员需要检查账户余额
        return baseCondition && parseFloat(this.estimatedCost) <= this.accountBalance
      } else {
        // 教练不需要检查余额
        return baseCondition
      }
    }
  },
  
  watch: {
    availableTimeSlots: {
      handler(newSlots) {
        console.log('availableTimeSlots变化:', newSlots.length, '个时间段')
        newSlots.forEach(slot => {
          console.log('- 时间段:', slot.label, '值:', slot.value)
        })
      },
      immediate: true
    },
    
    'bookingForm.timeSlot': {
      handler(newValue, oldValue) {
        console.log('bookingForm.timeSlot监听到变化:', oldValue, '->', newValue)
      }
    },
    
    'bookingForm.date': {
      handler(newValue, oldValue) {
        console.log('日期变化:', oldValue, '->', newValue)
        // 只有当日期真正改变时才重置
        if (newValue !== oldValue) {
          // 重置时间段选择
          this.bookingForm.timeSlot = null
          this.availableTables = []
          // 如果已选择教练，重新加载教练日程
          if (this.bookingForm.coachId) {
            this.loadCoachSchedule()
          }
        }
      }
    },
    
    'bookingForm.coachId': {
      handler(newValue, oldValue) {
        console.log('教练选择变化:', oldValue, '->', newValue)
        // 只有当教练真正改变时才重置
        if (newValue !== oldValue) {
          // 重置时间段和球台选择
          this.bookingForm.timeSlot = null
          this.bookingForm.tableNumber = null
          this.availableTables = []
          this.coachSchedule = []
          
          // 如果已选择日期，加载教练日程
          if (this.bookingForm.date) {
            this.loadCoachSchedule()
          }
        }
      }
    }
  },
  
  created() {
    // 根据用户角色加载不同数据
    if (this.userRole === 'COACH') {
      // 教练加载待处理预约和课程列表
      this.loadPendingBookings()
    } else if (this.userRole === 'STUDENT') {
      // 学员加载教练列表和账户余额
      this.loadMyCoaches()
      this.loadAccountBalance()
    }
    // 都需要加载自己的预约/课程
    this.loadMyBookings()
  },
  
  methods: {
    formatTime(date) {
      try {
        // 确保传入的是有效的 Date 对象
        if (!date || !(date instanceof Date) || isNaN(date.getTime())) {
          console.error('formatTime: 无效的日期对象', date)
          return '无效时间'
        }
        
        return date.toLocaleTimeString('zh-CN', { 
          hour: '2-digit', 
          minute: '2-digit',
          hour12: false 
        })
      } catch (error) {
        console.error('formatTime 错误:', error, date)
        return '格式化失败'
      }
    },
    
    async loadCurrentCoachProfile() {
      // 教练不需要此方法，因为不再创建预约
    },
    
    async loadMyCoaches() {
      try {
        // 根据需求文档，只能预约已建立双选关系的教练
        const response = await this.$http.get('/api/user/my-coaches')
        this.myCoaches = response.data.data || []
      } catch (error) {
        this.$message.error('加载我的教练失败')
      }
    },
    
    async loadMyBookings() {
      try {
        // 教练和学员使用不同的API端点
        const endpoint = this.userRole === 'COACH' ? '/api/coach/my-bookings' : '/api/course/my-bookings'
        const response = await this.$http.get(endpoint)
        this.myBookings = response.data.data || []
      } catch (error) {
        this.$message.error('加载我的预约失败')
      }
    },
    
    async loadCoachSchedule() {
      console.log('=== loadCoachSchedule被调用（新版本） ===')
      console.log('教练ID:', this.bookingForm.coachId)
      console.log('日期:', this.bookingForm.date)
      
      if (!this.bookingForm.coachId || !this.bookingForm.date) {
        console.log('缺少教练ID或日期，清空课表数据')
        this.coachSchedule = null
        return
      }
      
      try {
        const date = new Date(this.bookingForm.date)
        const startTime = new Date(date.getFullYear(), date.getMonth(), date.getDate()).toISOString()
        const endTime = new Date(date.getFullYear(), date.getMonth(), date.getDate() + 1).toISOString()
        
        console.log('加载教练可预约时间段参数:', { 
          coachId: this.bookingForm.coachId, 
          startTime, 
          endTime 
        })
        
        const response = await this.$http.get(`/api/student/coaches/${this.bookingForm.coachId}/schedule`, {
          params: { startTime, endTime }
        })
        
        this.coachSchedule = response.data.data || {}
        console.log('教练课表数据加载完成:', this.coachSchedule)
        console.log('可预约时间段数量:', this.coachSchedule.availableSlots ? this.coachSchedule.availableSlots.length : 0)
        console.log('已有预约数量:', this.coachSchedule.existingBookings ? this.coachSchedule.existingBookings.length : 0)
        
      } catch (error) {
        console.error('加载教练课表失败:', error)
        console.error('错误详情:', {
          status: error.response?.status,
          data: error.response?.data,
          message: error.message
        })
        this.coachSchedule = null
        this.$message.error('加载教练课表失败: ' + (error.response?.data?.message || error.message))
      }
    },
    
    async loadAvailableTables() {
      if (!this.bookingForm.date || !this.bookingForm.timeSlot) {
        console.log('缺少日期或时间段，无法加载球台')
        return
      }
      
      try {
        const date = new Date(this.bookingForm.date)
        
        // 优先从availableTimeSlots中查找，fallback到静态timeSlots
        let timeSlot = this.availableTimeSlots.find(slot => slot.value === this.bookingForm.timeSlot)
        
        if (!timeSlot) {
          // fallback到静态timeSlots
          timeSlot = this.timeSlots.find(slot => slot.value === this.bookingForm.timeSlot)
        }
        
        if (!timeSlot) {
          console.error('未找到时间段:', this.bookingForm.timeSlot)
          console.log('可用时间段:', this.availableTimeSlots.map(s => s.value))
          console.log('静态时间段:', this.timeSlots.map(s => s.value))
          return
        }
        
        const startTime = new Date(date.getFullYear(), date.getMonth(), date.getDate(), timeSlot.start).toISOString()
        const endTime = new Date(date.getFullYear(), date.getMonth(), date.getDate(), timeSlot.end).toISOString()
        
        console.log('加载球台参数:', { startTime, endTime })
        
        const response = await this.$http.get('/api/student/available-tables', {
          params: { startTime, endTime }
        })
        
        this.availableTables = response.data.data || []
        console.log('可用球台:', this.availableTables)
        
        if (this.availableTables.length === 0) {
          this.$message.warning('该时间段没有可用球台')
        }
      } catch (error) {
        console.error('加载可用球台失败:', error)
        this.$message.error('加载球台失败: ' + (error.response?.data?.message || error.message))
      }
    },
    
    async loadAccountBalance() {
      try {
        const response = await this.$http.get('/api/payment/balance')
        this.accountBalance = response.data.data || 0
      } catch (error) {
        console.error('加载账户余额失败:', error)
      }
    },
    
    async loadPendingBookings() {
      try {
        const response = await this.$http.get('/api/coach/pending-bookings')
        this.pendingBookings = response.data.data || []
      } catch (error) {
        this.$message.error('加载待处理预约失败')
      }
    },

    async approveBooking(bookingId) {
      try {
        await this.$confirm('确认确认此预约吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        const response = await this.$http.post(`/api/coach/approve-booking/${bookingId}`)
        this.$message.success(response.data.message)
        this.loadPendingBookings()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(error.response?.data?.message || '确认失败')
        }
      }
    },

    showRejectDialog(booking) {
      this.selectedPendingBooking = booking
      this.rejectReason = ''
      this.rejectDialogVisible = true
    },

    async rejectBooking() {
      if (!this.rejectReason.trim()) {
        this.$message.warning('请输入拒绝原因')
        return
      }
      try {
        await this.$confirm('确认拒绝此预约吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        // 使用URL参数传递拒绝原因
        const response = await this.$http.post(`/api/coach/reject-booking/${this.selectedPendingBooking.id}`, null, {
          params: { reason: this.rejectReason }
        })
        this.$message.success(response.data.message)
        this.rejectDialogVisible = false
        this.loadPendingBookings()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(error.response?.data?.message || '拒绝失败')
        }
      }
    },
    
    calculateCost() {
      // 这个方法可以用来计算成本，但现在我们直接在onTimeSlotChange中处理
      console.log('calculateCost被调用')
    },
    
    getLevelRate(level) {
      // 根据教练等级返回小时费率
      switch (level) {
        case 'SENIOR':
          return 200
        case 'INTERMEDIATE':
          return 150
        case 'JUNIOR':
          return 80
        default:
          return 80
      }
    },
    
    async submitBooking() {
      // 只有学员可以创建预约
      if (this.userRole !== 'STUDENT') {
        this.$message.error('只有学员可以创建预约')
        return
      }
      
      try {
        await this.$refs.bookingForm.validate()
        this.submitting = true
        
        const date = new Date(this.bookingForm.date)
        const timeSlot = this.timeSlots.find(slot => slot.value === this.bookingForm.timeSlot)
        
        const request = {
          coachId: this.bookingForm.coachId,
          startTime: new Date(date.getFullYear(), date.getMonth(), date.getDate(), timeSlot.start).toISOString(),
          endTime: new Date(date.getFullYear(), date.getMonth(), date.getDate(), timeSlot.end).toISOString(),
          tableNumber: this.bookingForm.tableNumber,
          remarks: this.bookingForm.remarks
        }
        
        // 学员使用student端点创建预约
        const response = await this.$http.post('/api/student/book-course', request)
        this.$message.success(response.data.message)
        
        // 重置表单并刷新数据
        this.resetForm()
        this.loadMyBookings()
        this.loadAccountBalance()
      } catch (error) {
        this.$message.error(error.response?.data?.message || '预约失败')
      } finally {
        this.submitting = false
      }
    },
    
    resetForm() {
      this.bookingForm = {
        coachId: null,
        date: null,
        timeSlot: null,
        tableNumber: null,
        remarks: ''
      }
      this.availableTables = []
    },
    
    getLevelText(level) {
      const levelMap = {
        'JUNIOR': '初级教练',
        'INTERMEDIATE': '中级教练', 
        'SENIOR': '高级教练',
        'MASTER': '专业教练'
      }
      return levelMap[level] || level
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
    
    formatBookingDateTime(dateTime) {
      return formatDateTime(dateTime)
    },
    
    formatBookingTimeShort(dateTime) {
      return formatDateTimeShort(dateTime)
    },
    
    formatBookingTime(startTime, endTime) {
      return formatBookingTime(startTime, endTime)
    },

    onTimeSlotChange(value) {
      console.log('=== 时间段选择变化 ===')
      console.log('选择的值:', value)
      
      // 确保值正确设置
      this.bookingForm.timeSlot = value
      
      console.log('设置后的bookingForm.timeSlot:', this.bookingForm.timeSlot)
      console.log('当前bookingForm.date:', this.bookingForm.date)
      console.log('当前bookingForm.coachId:', this.bookingForm.coachId)
      console.log('可用时间段数量:', this.availableTimeSlots.length)
      
      // 使用nextTick确保DOM更新完成
      this.$nextTick(() => {
        console.log('nextTick后，bookingForm.timeSlot:', this.bookingForm.timeSlot)
        if (this.bookingForm.timeSlot && this.bookingForm.date) {
          console.log('调用loadAvailableTables...')
          this.loadAvailableTables()
        } else {
          console.log('缺少必要条件，不调用loadAvailableTables')
          console.log('timeSlot:', this.bookingForm.timeSlot)
          console.log('date:', this.bookingForm.date)
        }
      })
    },

    onTimeSlotClear() {
      console.log('时间段选择器被清除')
      this.bookingForm.timeSlot = null
      this.availableTables = []
    },

    onTimeSlotDropdownChange(visible) {
      console.log('时间段选择器下拉框状态变化:', visible)
    }
  }
}
</script>

<style scoped>
.course-booking {
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

.main-content {
  padding: 20px;
}

.booking-form-card, .pending-bookings-card, .my-bookings-card, .balance-card {
  margin-bottom: 20px;
}

.booking-card, .booking-item {
  transition: all 0.3s ease;
}

.booking-card:hover, .booking-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.booking-info {
  padding: 10px 0;
}

.booking-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.booking-header h4 {
  margin: 0;
  color: #303133;
  font-size: 16px;
}

.booking-details p {
  margin: 5px 0;
  color: #606266;
  font-size: 14px;
}

.booking-actions {
  display: flex;
  gap: 10px;
  margin-top: 15px;
}

.balance-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.balance-amount {
  font-size: 24px;
  font-weight: bold;
  color: #67C23A;
}

.booking-list {
  max-height: 400px;
  overflow-y: auto;
}

.empty-container {
  text-align: center;
  padding: 40px 0;
}

.student-name, .coach-name {
  font-weight: 500;
  color: #303133;
}

/* 表单样式 */
.el-form-item {
  margin-bottom: 20px;
}

.cost-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.cost-amount {
  font-size: 18px;
  font-weight: bold;
  color: #E6A23C;
}

.cost-detail {
  font-size: 12px;
  color: #909399;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .course-booking {
    padding: 10px;
  }
  
  .main-content {
    padding: 10px;
  }
  
  .booking-actions {
    flex-direction: column;
  }
  
  .booking-actions .el-button {
    width: 100%;
  }
}
</style> 