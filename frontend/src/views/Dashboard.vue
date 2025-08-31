<template>
  <div class="dashboard">
    <el-container>
      <!-- 顶部导航 -->
      <el-header class="header">
        <div class="header-left">
          <h1 class="system-title">乒乓球培训管理系统</h1>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <i class="el-icon-user"></i>
              {{ userName }}
              <i class="el-icon-arrow-down el-icon--right"></i>
            </span>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item command="profile">个人信息</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </div>
      </el-header>
      
      <!-- 主要内容 -->
      <el-main class="main-content">
        <!-- 欢迎信息 -->
        <div class="welcome-section">
          <h2>欢迎回来，{{ userName }}!</h2>
          <p class="user-details">
            {{ getRoleText(userRole) }} · {{ userCampus }}
            <span v-if="userRole === 'STUDENT'" class="balance-info">
              · 账户余额: ¥{{ accountBalance }}
            </span>
          </p>
        </div>
        
        <!-- 快速统计 -->
        <div class="stats-section" v-if="userRole === 'STUDENT' || userRole === 'COACH'">
          <el-row :gutter="20">
            <el-col :span="6" v-if="userRole === 'STUDENT'">
              <el-card class="stat-card">
                <div class="stat-content">
                  <div class="stat-number">{{ myCoachesCount }}</div>
                  <div class="stat-label">我的教练</div>
                </div>
                <i class="el-icon-user stat-icon"></i>
              </el-card>
            </el-col>
            
            <el-col :span="6" v-if="userRole === 'COACH'">
              <el-card class="stat-card coach-card" @click.native="$router.push('/coach/students')">
                <div class="stat-content">
                  <div class="stat-icon">
                    <i class="el-icon-user-solid"></i>
                  </div>
                  <div class="stat-info">
                    <div class="stat-value">{{ myStudentsCount }}</div>
                    <div class="stat-label">我的学员</div>
                  </div>
                </div>
              </el-card>
            </el-col>
            
            <el-col :span="6">
              <el-card class="stat-card schedule-card" @click.native="$router.push('/coach/schedule')">
                <div class="stat-content">
                  <div class="stat-icon">
                    <i class="el-icon-date"></i>
                  </div>
                  <div class="stat-info">
                    <div class="stat-value">{{ todayBookingsCount }}</div>
                    <div class="stat-label">今日课程</div>
                  </div>
                </div>
              </el-card>
            </el-col>
            
            <el-col :span="6" v-if="userRole === 'STUDENT'">
              <el-card class="stat-card">
                <div class="stat-content">
                  <div class="stat-number">{{ monthBookingsCount }}</div>
                  <div class="stat-label">本月课程</div>
                </div>
                <i class="el-icon-calendar stat-icon"></i>
              </el-card>
            </el-col>
            
            <el-col :span="6" v-if="userRole === 'COACH'">
              <el-card class="stat-card message-card" @click.native="$router.push('/messages')">
                <div class="stat-content">
                  <div class="stat-icon">
                    <i class="el-icon-message"></i>
                  </div>
                  <div class="stat-info">
                    <div class="stat-value">{{ pendingApprovalsCount }}</div>
                    <div class="stat-label">待处理消息</div>
                  </div>
                </div>
              </el-card>
            </el-col>
            
            <el-col :span="6">
              <el-card class="stat-card booking-card" @click.native="$router.push('/course/list')">
                <div class="stat-content">
                  <div class="stat-icon">
                    <i class="el-icon-tickets"></i>
                  </div>
                  <div class="stat-info">
                    <div class="stat-value">{{ pendingBookingsCount || 0 }}</div>
                    <div class="stat-label">待确认预约</div>
                  </div>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </div>
        
        <!-- 功能卡片 -->
        <div class="function-section">
          <h3 class="section-title">主要功能</h3>
          
          <!-- 学员功能 -->
          <div v-if="userRole === 'STUDENT'" class="function-cards">
            <el-row :gutter="20">
              <el-col :span="6">
                <el-card class="function-card" @click.native="$router.push('/coach/search')">
                  <div class="card-content">
                    <i class="el-icon-search card-icon"></i>
                    <h4>查找教练</h4>
                    <p>搜索和选择适合的教练</p>
                  </div>
                </el-card>
              </el-col>
              
              <el-col :span="6">
                <el-card class="function-card" @click.native="$router.push('/course/booking')">
                  <div class="card-content">
                    <i class="el-icon-date card-icon"></i>
                    <h4>课程预约</h4>
                    <p>预约教练课程时间</p>
                  </div>
                </el-card>
              </el-col>
              
              <el-col :span="6">
                <el-card class="function-card" @click.native="$router.push('/payment')">
                  <div class="card-content">
                    <i class="el-icon-money card-icon"></i>
                    <h4>充值管理</h4>
                    <p>账户充值和消费记录</p>
                  </div>
                </el-card>
              </el-col>
              
              <el-col :span="6">
                <el-card class="function-card" @click.native="$router.push('/evaluation')">
                  <div class="card-content">
                    <i class="el-icon-edit-outline card-icon"></i>
                    <h4>课程评价</h4>
                    <p>课后评价和查看评价记录</p>
                  </div>
                </el-card>
              </el-col>
            </el-row>
            
            <el-row :gutter="20" style="margin-top: 20px;">
              <el-col :span="6">
                <el-card class="function-card" @click.native="$router.push('/competition')">
                  <div class="card-content">
                    <i class="el-icon-trophy card-icon"></i>
                    <h4>月赛报名</h4>
                    <p>参加校区月度比赛</p>
                  </div>
                </el-card>
              </el-col>
              
              <el-col :span="6">
                <el-card class="function-card" @click.native="$router.push('/profile')">
                  <div class="card-content">
                    <i class="el-icon-user card-icon"></i>
                    <h4>个人中心</h4>
                    <p>查看和修改个人信息</p>
                  </div>
                </el-card>
              </el-col>
            </el-row>
          </div>
          
          <!-- 教练功能 -->
          <div v-if="userRole === 'COACH'" class="function-cards">
            <el-row :gutter="20">
              <el-col :span="6">
                <el-card class="function-card" @click.native="$router.push('/course/booking')">
                  <div class="card-content">
                    <i class="el-icon-date card-icon"></i>
                    <h4>课程管理</h4>
                    <p>查看和管理课程预约</p>
                  </div>
                </el-card>
              </el-col>
              
              <el-col :span="6">
                <el-card class="function-card" @click.native="showStudentRequests">
                  <div class="card-content">
                    <i class="el-icon-user-solid card-icon"></i>
                    <h4>学员申请</h4>
                    <p>处理学员的双选申请</p>
                    <el-badge v-if="pendingApprovalsCount > 0" :value="pendingApprovalsCount" class="badge"></el-badge>
                  </div>
                </el-card>
              </el-col>
              
              <el-col :span="6">
                <el-card class="function-card" @click.native="$router.push('/coach/students')">
                  <div class="card-content">
                    <i class="el-icon-s-custom card-icon"></i>
                    <h4>我的学员</h4>
                    <p>查看和管理我的学员</p>
                  </div>
                </el-card>
              </el-col>
              
              <el-col :span="6">
                <el-card class="function-card" @click.native="$router.push('/evaluation')">
                  <div class="card-content">
                    <i class="el-icon-edit-outline card-icon"></i>
                    <h4>课程评价</h4>
                    <p>评价学员课程表现</p>
                  </div>
                </el-card>
              </el-col>
            </el-row>
            
            <el-row :gutter="20" style="margin-top: 20px;">
              <el-col :span="6">
                <el-card class="function-card" @click.native="$router.push('/coach/working-time')">
                  <div class="card-content">
                    <i class="el-icon-time card-icon"></i>
                    <h4>工作时间</h4>
                    <p>设置和管理可预约时间</p>
                  </div>
                </el-card>
              </el-col>
              
              <el-col :span="6">
                <el-card class="function-card" @click.native="$router.push('/profile')">
                  <div class="card-content">
                    <i class="el-icon-user card-icon"></i>
                    <h4>个人中心</h4>
                    <p>查看和修改个人信息</p>
                  </div>
                </el-card>
              </el-col>
            </el-row>
          </div>
          
          <!-- 管理员功能 -->
          <div v-if="userRole === 'CAMPUS_ADMIN' || userRole === 'SUPER_ADMIN'" class="function-cards">
            <el-row :gutter="20">
              <el-col :span="6">
                <el-card class="function-card" @click.native="$router.push('/admin')">
                  <div class="card-content">
                    <i class="el-icon-s-tools card-icon"></i>
                    <h4>管理面板</h4>
                    <p>教练审核和系统管理</p>
                  </div>
                </el-card>
              </el-col>
              
              <el-col :span="6" v-if="userRole === 'SUPER_ADMIN'">
                <el-card class="function-card" @click.native="$router.push('/admin')">
                  <div class="card-content">
                    <i class="el-icon-office-building card-icon"></i>
                    <h4>校区管理</h4>
                    <p>管理校区信息和设置</p>
                  </div>
                </el-card>
              </el-col>
              
              <el-col :span="6">
                <el-card class="function-card" @click.native="$router.push('/admin')">
                  <div class="card-content">
                    <i class="el-icon-document card-icon"></i>
                    <h4>系统日志</h4>
                    <p>查看系统操作记录</p>
                  </div>
                </el-card>
              </el-col>
              
              <el-col :span="6">
                <el-card class="function-card" @click.native="$router.push('/profile')">
                  <div class="card-content">
                    <i class="el-icon-user card-icon"></i>
                    <h4>个人中心</h4>
                    <p>查看和修改个人信息</p>
                  </div>
                </el-card>
              </el-col>
            </el-row>
          </div>
        </div>
        
        <!-- 最近活动 -->
        <div class="activity-section" v-if="recentActivities.length > 0">
          <h3 class="section-title">最近活动</h3>
          <el-card>
            <el-timeline>
              <el-timeline-item 
                v-for="activity in recentActivities" 
                :key="activity.id"
                :timestamp="formatDate(activity.createdAt)"
              >
                {{ activity.description }}
              </el-timeline-item>
            </el-timeline>
          </el-card>
        </div>
      </el-main>
    </el-container>
    
    <!-- 学员申请弹窗（教练用） -->
    <el-dialog
      title="学员申请列表"
      :visible.sync="studentRequestsVisible"
      width="800px"
    >
      <el-table :data="pendingRelations" stripe>
        <el-table-column prop="student.realName" label="学员姓名" width="120" />
        <el-table-column prop="student.gender" label="性别" width="80">
          <template slot-scope="scope">
            {{ scope.row.student.gender === 'MALE' ? '男' : '女' }}
          </template>
        </el-table-column>
        <el-table-column prop="student.age" label="年龄" width="80" />
        <el-table-column prop="student.phone" label="联系电话" width="120" />
        <el-table-column prop="createdAt" label="申请时间" width="150">
          <template slot-scope="scope">
            {{ formatDate(scope.row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template slot-scope="scope">
            <el-button 
              type="success" 
              size="mini"
              @click="approveStudent(scope.row)"
            >
              同意
            </el-button>
            <el-button 
              type="danger" 
              size="mini"
              @click="rejectStudent(scope.row)"
            >
              拒绝
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script>
import { formatDate } from '@/utils/dateFormatter'

export default {
  name: 'Dashboard',
  
  data() {
    return {
      accountBalance: 0,
      myCoachesCount: 0,
      myStudentsCount: 0,
      todayBookingsCount: 0,
      monthBookingsCount: 0,
      pendingApprovalsCount: 0,
      recentActivities: [],
      pendingRelations: [],
      studentRequestsVisible: false,
      pendingBookingsCount: 0 // Added for new stat
    }
  },
  
  computed: {
    userName() {
      return this.$store.getters.userName
    },
    
    userRole() {
      return this.$store.getters.userRole
    },
    
    userCampus() {
      return this.$store.getters.userCampus
    }
  },
  
  created() {
    this.loadDashboardData()
  },
  
  methods: {
    async loadDashboardData() {
      try {
        // 根据用户角色加载不同的数据
        if (this.userRole === 'STUDENT') {
          await this.loadStudentData()
        } else if (this.userRole === 'COACH') {
          await this.loadCoachData()
        } else if (this.userRole === 'CAMPUS_ADMIN' || this.userRole === 'SUPER_ADMIN') {
          await this.loadAdminData()
        }
        
        // 加载最近活动（如果有相关API）
        // this.loadRecentActivities()
      } catch (error) {
        console.error('加载仪表板数据失败:', error)
      }
    },
    
    async loadStudentData() {
      try {
        // 加载账户余额
        const balanceResponse = await this.$http.get('/api/payment/balance')
        this.accountBalance = balanceResponse.data.data || 0
        
        // 加载我的教练
        const coachesResponse = await this.$http.get('/api/user/my-coaches')
        const coachesData = coachesResponse.data.data || []
        this.myCoachesCount = coachesData.filter(r => r.status === 'APPROVED').length
        
        // 加载我的预约 - 学员使用course端点
        const bookingsResponse = await this.$http.get('/api/course/my-bookings')
        const bookings = bookingsResponse.data.data || []
        
        // 统计今日课程
        const today = new Date().toDateString()
        this.todayBookingsCount = bookings.filter(booking => {
          const bookingDate = new Date(booking.startTime).toDateString()
          return bookingDate === today && booking.status === 'CONFIRMED'
        }).length
        
        // 统计本月课程
        const currentMonth = new Date().getMonth()
        const currentYear = new Date().getFullYear()
        this.monthBookingsCount = bookings.filter(booking => {
          const bookingDate = new Date(booking.startTime)
          return bookingDate.getMonth() === currentMonth && 
                 bookingDate.getFullYear() === currentYear &&
                 booking.status === 'CONFIRMED'
        }).length
      } catch (error) {
        console.error('加载学员数据失败:', error)
      }
    },
    
    async loadCoachData() {
      try {
        // 加载我的学员
        const studentsResponse = await this.$http.get('/api/coach/my-students')
        this.myStudentsCount = (studentsResponse.data.data || []).length
        
        // 加载待处理的双选申请
        const pendingResponse = await this.$http.get('/api/coach/pending-relations')
        const pendingData = pendingResponse.data.data || []
        this.pendingApprovalsCount = pendingData.length
        this.pendingRelations = pendingData
        
        // 加载我的课程 - 教练使用coach端点
        const bookingsResponse = await this.$http.get('/api/coach/my-bookings')
        const bookings = bookingsResponse.data.data || []
        
        console.log('Dashboard - 获取到的所有课程:', bookings)
        
        // 统计今日课程
        const today = new Date().toDateString()
        const todayBookings = bookings.filter(booking => {
          const bookingDate = new Date(booking.startTime).toDateString()
          const isToday = bookingDate === today
          const isActive = booking.status === 'PENDING' || booking.status === 'CONFIRMED'
          
          console.log('课程检查:', {
            id: booking.id,
            startTime: booking.startTime,
            bookingDate,
            today,
            isToday,
            status: booking.status,
            isActive,
            shouldCount: isToday && isActive
          })
          
          return isToday && isActive
        })
        
        console.log('今日课程列表:', todayBookings)
        this.todayBookingsCount = todayBookings.length

        // 统计待确认预约数量
        this.pendingBookingsCount = bookings.filter(booking => booking.status === 'PENDING').length
      } catch (error) {
        console.error('加载教练数据失败:', error)
      }
    },
    
    async loadAdminData() {
      try {
        // 管理员不需要特定的统计数据，这里可以留空
        // 或者加载一些系统级别的统计信息
        console.log('管理员数据加载完成')
      } catch (error) {
        console.error('加载管理员数据失败:', error)
      }
    },
    
    showStudentRequests() {
      this.studentRequestsVisible = true
    },
    
    async approveStudent(relation) {
      try {
        const response = await this.$http.post(`/api/coach/approve-relation/${relation.id}`)
        this.$message.success(response.data.message)
        this.loadCoachData() // 刷新数据
      } catch (error) {
        this.$message.error(error.response?.data?.message || '操作失败')
      }
    },
    
    async rejectStudent(relation) {
      try {
        const response = await this.$http.post(`/api/coach/reject-relation/${relation.id}`)
        this.$message.success(response.data.message)
        this.loadCoachData() // 刷新数据
      } catch (error) {
        this.$message.error(error.response?.data?.message || '操作失败')
      }
    },
    
    handleCommand(command) {
      if (command === 'profile') {
        this.$router.push('/profile')
      } else if (command === 'logout') {
        this.$store.dispatch('logout')
        this.$router.push('/login')
      }
    },
    
    getRoleText(role) {
      const roleMap = {
        'STUDENT': '学员',
        'COACH': '教练员',
        'CAMPUS_ADMIN': '校区管理员',
        'SUPER_ADMIN': '超级管理员'
      }
      return roleMap[role] || '用户'
    },
    
    formatDate(dateTime) {
      return formatDate(dateTime)
    }
  }
}
</script>

<style scoped>
.dashboard {
  height: 100vh;
  background: #f5f5f5;
}

.header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 0 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.system-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.user-info {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 5px;
}

.main-content {
  padding: 20px;
}

.welcome-section {
  margin-bottom: 30px;
}

.welcome-section h2 {
  margin: 0 0 10px 0;
  color: #333;
  font-size: 24px;
}

.user-details {
  color: #666;
  margin: 0;
  font-size: 14px;
}

.balance-info {
  color: #67C23A;
  font-weight: 600;
}

.stats-section {
  margin-bottom: 30px;
}

.stat-card {
  text-align: center;
  cursor: default;
}

.stat-content {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-number {
  font-size: 32px;
  font-weight: 600;
  color: #409EFF;
  margin-bottom: 5px;
}

.stat-label {
  color: #666;
  font-size: 14px;
}

.stat-icon {
  position: absolute;
  top: 20px;
  right: 20px;
  font-size: 40px;
  color: #ddd;
}

.function-section, .activity-section {
  margin-bottom: 30px;
}

.section-title {
  margin: 0 0 20px 0;
  color: #333;
  font-size: 18px;
  font-weight: 600;
}

.function-cards {
  margin-bottom: 20px;
}

.function-card {
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
}

.function-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.card-content {
  text-align: center;
  padding: 20px;
}

.card-icon {
  font-size: 48px;
  color: #409EFF;
  margin-bottom: 15px;
}

.card-content h4 {
  margin: 0 0 10px 0;
  color: #333;
  font-size: 16px;
}

.card-content p {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.badge {
  position: absolute;
  top: 10px;
  right: 10px;
}

/* New styles for coach-specific stats */
.coach-card .stat-content {
  flex-direction: row;
  align-items: center;
  gap: 10px;
}

.coach-card .stat-icon {
  position: static;
  top: auto;
  right: auto;
  font-size: 30px;
  color: #409EFF;
}

.coach-card .stat-info {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.coach-card .stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #409EFF;
}

.coach-card .stat-label {
  color: #666;
  font-size: 12px;
}

.schedule-card .stat-content {
  flex-direction: row;
  align-items: center;
  gap: 10px;
}

.schedule-card .stat-icon {
  position: static;
  top: auto;
  right: auto;
  font-size: 30px;
  color: #409EFF;
}

.schedule-card .stat-info {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.schedule-card .stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #409EFF;
}

.schedule-card .stat-label {
  color: #666;
  font-size: 12px;
}

.message-card .stat-content {
  flex-direction: row;
  align-items: center;
  gap: 10px;
}

.message-card .stat-icon {
  position: static;
  top: auto;
  right: auto;
  font-size: 30px;
  color: #409EFF;
}

.message-card .stat-info {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.message-card .stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #409EFF;
}

.message-card .stat-label {
  color: #666;
  font-size: 12px;
}

.booking-card .stat-content {
  flex-direction: row;
  align-items: center;
  gap: 10px;
}

.booking-card .stat-icon {
  position: static;
  top: auto;
  right: auto;
  font-size: 30px;
  color: #409EFF;
}

.booking-card .stat-info {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.booking-card .stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #409EFF;
}

.booking-card .stat-label {
  color: #666;
  font-size: 12px;
}
</style> 