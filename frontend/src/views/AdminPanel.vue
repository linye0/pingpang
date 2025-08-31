<template>
  <div class="admin-panel">
    <el-container>
      <!-- 顶部导航 -->
      <el-header class="header">
        <div class="header-content">
          <div>
            <h1 class="page-title">管理面板</h1>
            <span class="role-badge">
              <el-tag :type="isSuper ? 'danger' : 'warning'">
                {{ isSuper ? '超级管理员' : '校区管理员' }}
              </el-tag>
              <span v-if="isCampusAdmin" class="campus-info">
                - {{ $store.state.user?.campus?.name || '未知校区' }}
              </span>
            </span>
          </div>
          <el-button @click="$router.push('/dashboard')">返回首页</el-button>
        </div>
      </el-header>
      
      <!-- 主要内容 -->
      <el-main class="main-content">
        <el-tabs v-model="activeTab" type="card">
          <!-- 教练审核 -->
          <el-tab-pane label="教练审核" name="coach-approval" key="coach-approval-tab">
            <el-card>
              <div slot="header">
                <span>待审核教练</span>
                <el-button 
                  type="text" 
                  @click="loadPendingCoaches"
                  style="float: right; padding: 3px 0"
                >
                  刷新
                </el-button>
              </div>
              
              <div v-if="isCampusAdmin" class="scope-info">
                <el-alert
                  title="权限范围"
                  :description="`您当前只能审核 ${$store.state.user?.campus?.name || '您的校区'} 的教练申请`"
                  type="info"
                  :closable="false"
                  show-icon
                  style="margin-bottom: 15px;"
                />
              </div>
              
              <div v-if="!pendingCoaches || pendingCoaches.length === 0" class="empty-container">
                <el-empty description="暂无待审核教练" />
              </div>
              
              <div v-else>
                <el-table :data="pendingCoaches || []" stripe>
                  <el-table-column prop="realName" label="姓名" width="120" />
                  <el-table-column prop="gender" label="性别" width="80">
                    <template slot-scope="scope">
                      {{ scope.row.gender === 'MALE' ? '男' : '女' }}
                    </template>
                  </el-table-column>
                  <el-table-column prop="age" label="年龄" width="80" />
                  <el-table-column prop="phone" label="电话" width="120" />
                  <el-table-column prop="level" label="申请等级" width="120">
                    <template slot-scope="scope">
                      {{ getLevelText(scope.row.level) }}
                    </template>
                  </el-table-column>
                  <el-table-column prop="achievements" label="获奖经历" min-width="200">
                    <template slot-scope="scope">
                      <el-tooltip :content="scope.row.achievements" placement="top">
                        <span>{{ scope.row.achievements.substring(0, 50) }}...</span>
                      </el-tooltip>
                    </template>
                  </el-table-column>
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
                        @click="approveCoach(scope.row)"
                      >
                        通过
                      </el-button>
                      <el-button 
                        type="danger" 
                        size="mini"
                        @click="rejectCoach(scope.row)"
                      >
                        拒绝
                      </el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </el-card>
          </el-tab-pane>
          
          <!-- 校区管理 -->
          <el-tab-pane label="校区管理" name="campuses" key="campuses-tab" v-if="isSuper">
            <el-card>
              <div slot="header">
                <span>校区列表</span>
                <el-button 
                  type="primary" 
                  size="small"
                  @click="showCampusDialog(null)"
                  style="float: right"
                >
                  新增校区
                </el-button>
              </div>
              
              <el-table :data="campuses || []" stripe>
                <el-table-column prop="name" label="校区名称" width="150" />
                <el-table-column prop="address" label="地址" min-width="200" />
                <el-table-column prop="contactPerson" label="联系人" width="120" />
                <el-table-column prop="contactPhone" label="联系电话" width="120" />
                <el-table-column prop="contactEmail" label="邮箱" width="150" />
                <el-table-column label="是否主校区" width="100">
                  <template slot-scope="scope">
                    <el-tag :type="scope.row.isMainCampus ? 'success' : 'info'" size="small">
                      {{ scope.row.isMainCampus ? '主校区' : '分校区' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="150">
                  <template slot-scope="scope">
                    <el-button 
                      type="primary" 
                      size="mini"
                      @click="showCampusDialog(scope.row)"
                    >
                      编辑
                    </el-button>
                    <el-button 
                      type="danger" 
                      size="mini"
                      @click="deleteCampus(scope.row)"
                      :disabled="scope.row.isMainCampus"
                    >
                      删除
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-tab-pane>
          
          <!-- 用户管理 -->
          <el-tab-pane label="用户管理" name="users" key="users-tab">
            <el-card>
              <div slot="header">
                <span>用户列表</span>
                <el-button 
                  type="text" 
                  @click="loadUsers"
                  style="float: right; padding: 3px 0"
                >
                  刷新
                </el-button>
              </div>
              
              <div v-if="isCampusAdmin" class="scope-info">
                <el-alert
                  title="权限范围"
                  :description="`您当前只能管理 ${$store.state.user?.campus?.name || '您的校区'} 的用户`"
                  type="info"
                  :closable="false"
                  show-icon
                  style="margin-bottom: 15px;"
                />
              </div>
              
              <el-table :data="users || []" stripe>
                <el-table-column prop="username" label="用户名" width="120" />
                <el-table-column prop="realName" label="真实姓名" width="120" />
                <el-table-column prop="role" label="角色" width="120">
                  <template slot-scope="scope">
                    {{ getRoleText(scope.row.role) }}
                  </template>
                </el-table-column>
                <el-table-column prop="phone" label="电话" width="120" />
                <el-table-column prop="email" label="邮箱" width="180" />
                <el-table-column prop="active" label="状态" width="80">
                  <template slot-scope="scope">
                    <el-tag :type="scope.row.active ? 'success' : 'danger'" size="small">
                      {{ scope.row.active ? '活跃' : '禁用' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="200">
                  <template slot-scope="scope">
                    <el-button 
                      :type="scope.row.active ? 'warning' : 'success'" 
                      size="mini"
                      @click="toggleUserStatus(scope.row)"
                    >
                      {{ scope.row.active ? '禁用' : '启用' }}
                    </el-button>
                    <el-button 
                      type="primary" 
                      size="mini"
                      @click="showUserDetail(scope.row)"
                    >
                      详情
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-tab-pane>
          
          <!-- 教练管理 -->
          <el-tab-pane label="教练管理" name="coaches" key="coaches-tab">
            <el-card>
              <div slot="header">
                <span>教练列表</span>
                <el-button 
                  type="text" 
                  @click="loadCoaches"
                  style="float: right; padding: 3px 0"
                >
                  刷新
                </el-button>
              </div>
              
              <el-table :data="coaches || []" stripe>
                <el-table-column prop="realName" label="姓名" width="120" />
                <el-table-column prop="level" label="等级" width="120">
                  <template slot-scope="scope">
                    {{ getLevelText(scope.row.level) }}
                  </template>
                </el-table-column>
                <el-table-column prop="approvalStatus" label="审核状态" width="120">
                  <template slot-scope="scope">
                    <el-tag :type="getStatusType(scope.row.approvalStatus)" size="small">
                      {{ getApprovalText(scope.row.approvalStatus) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="phone" label="电话" width="120" />
                <el-table-column prop="active" label="状态" width="80">
                  <template slot-scope="scope">
                    <el-tag :type="scope.row.active ? 'success' : 'danger'" size="small">
                      {{ scope.row.active ? '活跃' : '禁用' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="200">
                  <template slot-scope="scope">
                    <el-button 
                      :type="scope.row.active ? 'warning' : 'success'" 
                      size="mini"
                      @click="toggleCoachStatus(scope.row)"
                      v-if="scope.row.approvalStatus === 'APPROVED'"
                    >
                      {{ scope.row.active ? '禁用' : '启用' }}
                    </el-button>
                    <el-button 
                      type="primary" 
                      size="mini"
                      @click="showCoachDetail(scope.row)"
                    >
                      详情
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-tab-pane>
          
          <!-- 学员管理 -->
          <el-tab-pane label="学员管理" name="students" key="students-tab">
            <el-card>
              <div slot="header">
                <span>学员列表</span>
                <el-button 
                  type="text" 
                  @click="loadStudents"
                  style="float: right; padding: 3px 0"
                >
                  刷新
                </el-button>
              </div>
              
              <el-table :data="students || []" stripe>
                <el-table-column prop="realName" label="姓名" width="120" />
                <el-table-column prop="gender" label="性别" width="80">
                  <template slot-scope="scope">
                    {{ scope.row.gender === 'MALE' ? '男' : '女' }}
                  </template>
                </el-table-column>
                <el-table-column prop="age" label="年龄" width="80" />
                <el-table-column prop="phone" label="电话" width="120" />
                <el-table-column prop="accountBalance" label="账户余额" width="120">
                  <template slot-scope="scope">
                    ¥{{ scope.row.accountBalance || 0 }}
                  </template>
                </el-table-column>
                <el-table-column prop="active" label="状态" width="80">
                  <template slot-scope="scope">
                    <el-tag :type="scope.row.active ? 'success' : 'danger'" size="small">
                      {{ scope.row.active ? '活跃' : '禁用' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="200">
                  <template slot-scope="scope">
                    <el-button 
                      :type="scope.row.active ? 'warning' : 'success'" 
                      size="mini"
                      @click="toggleStudentStatus(scope.row)"
                    >
                      {{ scope.row.active ? '禁用' : '启用' }}
                    </el-button>
                    <el-button 
                      type="primary" 
                      size="mini"
                      @click="showStudentDetail(scope.row)"
                    >
                      详情
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-tab-pane>
          
          <!-- 课程管理 -->
          <el-tab-pane label="课程管理" name="courses" key="courses-tab">
            <el-card>
              <div slot="header">
                <span>课程预约</span>
                <el-button 
                  type="text" 
                  @click="loadBookings"
                  style="float: right; padding: 3px 0"
                >
                  刷新
                </el-button>
              </div>
              
              <el-table :data="bookings || []" stripe>
                <el-table-column prop="student.realName" label="学员" width="120" />
                <el-table-column prop="coach.realName" label="教练" width="120" />
                <el-table-column prop="startTime" label="上课时间" width="180">
                  <template slot-scope="scope">
                    {{ formatDateTime(scope.row.startTime) }}
                  </template>
                </el-table-column>
                <el-table-column prop="duration" label="时长(分钟)" width="100" />
                <el-table-column prop="status" label="状态" width="120">
                  <template slot-scope="scope">
                    <el-tag :type="getBookingStatusType(scope.row.status)" size="small">
                      {{ getBookingStatusText(scope.row.status) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="createdAt" label="预约时间" width="150">
                  <template slot-scope="scope">
                    {{ formatDate(scope.row.createdAt) }}
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-tab-pane>
          
          <!-- 系统日志 -->
          <el-tab-pane label="系统日志" name="logs" key="logs-tab">
            <el-card>
              <div slot="header">
                <span>操作日志</span>
                <el-button 
                  type="text" 
                  @click="loadSystemLogs"
                  style="float: right; padding: 3px 0"
                >
                  刷新
                </el-button>
              </div>
              
              <el-table :data="systemLogs || []" stripe>
                <el-table-column prop="user.realName" label="操作人" width="120" />
                <el-table-column prop="operationType" label="操作类型" width="120" />
                <el-table-column prop="description" label="操作描述" min-width="300" />
                <el-table-column prop="createdAt" label="操作时间" width="150">
                  <template slot-scope="scope">
                    {{ formatDate(scope.row.createdAt) }}
                  </template>
                </el-table-column>
              </el-table>
              
              <!-- 分页 -->
              <div class="pagination-container">
                <el-pagination
                  @current-change="handleLogPageChange"
                  :current-page="logCurrentPage"
                  :page-size="logPageSize"
                  layout="total, prev, pager, next"
                  :total="logTotal"
                />
              </div>
            </el-card>
          </el-tab-pane>
        </el-tabs>
      </el-main>
    </el-container>
    
    <!-- 校区编辑弹窗 -->
    <el-dialog
      :title="campusDialogTitle"
      :visible.sync="campusDialogVisible"
      width="600px"
    >
      <el-form 
        :model="campusForm" 
        :rules="campusRules" 
        ref="campusForm"
        label-width="120px"
      >
        <el-form-item label="校区名称" prop="name">
          <el-input v-model="campusForm.name" placeholder="请输入校区名称" />
        </el-form-item>
        
        <el-form-item label="校区地址" prop="address">
          <el-input v-model="campusForm.address" placeholder="请输入校区地址" />
        </el-form-item>
        
        <el-form-item label="联系人" prop="contactPerson">
          <el-input v-model="campusForm.contactPerson" placeholder="请输入联系人姓名" />
        </el-form-item>
        
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="campusForm.contactPhone" placeholder="请输入联系电话" />
        </el-form-item>
        
        <el-form-item label="联系邮箱" prop="contactEmail">
          <el-input v-model="campusForm.contactEmail" placeholder="请输入联系邮箱" />
        </el-form-item>
        
        <el-form-item label="是否主校区">
          <el-switch 
            v-model="campusForm.isMainCampus"
            :disabled="editingCampus && editingCampus.isMainCampus"
          />
        </el-form-item>
      </el-form>
      
      <div slot="footer" class="dialog-footer">
        <el-button @click="campusDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCampus" :loading="savingCampus">
          确定
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { formatDate } from '@/utils/dateFormatter'

export default {
  name: 'AdminPanel',
  
  data() {
    return {
      activeTab: 'coach-approval',
      pendingCoaches: [],
      users: [],
      coaches: [],
      students: [],
      bookings: [],
      campuses: [],
      systemLogs: [],
      campusDialogVisible: false,
      campusForm: {
        name: '',
        address: '',
        contactPerson: '',
        contactPhone: '',
        contactEmail: '',
        isMainCampus: false
      },
      campusRules: {
        name: [
          { required: true, message: '请输入校区名称', trigger: 'blur' }
        ],
        address: [
          { required: true, message: '请输入校区地址', trigger: 'blur' }
        ],
        contactPerson: [
          { required: true, message: '请输入联系人', trigger: 'blur' }
        ],
        contactPhone: [
          { required: true, message: '请输入联系电话', trigger: 'blur' },
          { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
        ],
        contactEmail: [
          { required: true, message: '请输入联系邮箱', trigger: 'blur' },
          { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
        ]
      },
      editingCampus: null,
      savingCampus: false,
      logCurrentPage: 1,
      logPageSize: 20,
      logTotal: 0
    }
  },
  
  computed: {
    isSuper() {
      return this.$store.getters.userRole === 'SUPER_ADMIN'
    },
    
    isCampusAdmin() {
      return this.$store.getters.userRole === 'CAMPUS_ADMIN'
    },
    
    userCampusId() {
      return this.$store.state.user?.campus?.id
    },
    
    campusDialogTitle() {
      return this.editingCampus ? '编辑校区' : '新增校区'
    }
  },
  
  created() {
    this.loadPendingCoaches()
    this.loadUsers()
    this.loadCoaches()
    this.loadStudents()
    this.loadBookings()
    if (this.isSuper) {
      this.loadCampuses()
    }
    this.loadSystemLogs()
  },
  
  methods: {
    async loadPendingCoaches() {
      try {
        const response = await this.$http.get('/api/admin/pending-coaches')
        this.pendingCoaches = response.data.data || []
      } catch (error) {
        this.pendingCoaches = []
        this.$message.error('加载待审核教练失败')
      }
    },
    
    async loadCampuses() {
      try {
        const response = await this.$http.get('/api/admin/campus')
        this.campuses = response.data.data || []
      } catch (error) {
        this.campuses = []
        this.$message.error('加载校区列表失败')
      }
    },
    
    async loadSystemLogs() {
      try {
        const response = await this.$http.get('/api/admin/logs', {
          params: {
            page: this.logCurrentPage - 1,
            size: this.logPageSize
          }
        })
        this.systemLogs = response.data.data.content || response.data.data || []
        this.logTotal = response.data.data.totalElements || (response.data.data && response.data.data.length) || 0
      } catch (error) {
        this.systemLogs = []
        this.logTotal = 0
        this.$message.error('加载系统日志失败')
      }
    },
    
    async approveCoach(coach) {
      try {
        await this.$confirm(`确认通过教练 ${coach.realName} 的申请吗？`, '确认操作', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'success'
        })
        
        const response = await this.$http.post(`/api/admin/approve-coach/${coach.id}`)
        this.$message.success(response.data.message)
        this.loadPendingCoaches()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(error.response?.data?.message || '操作失败')
        }
      }
    },
    
    async rejectCoach(coach) {
      try {
        await this.$confirm(`确认拒绝教练 ${coach.realName} 的申请吗？`, '确认操作', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        const response = await this.$http.post(`/api/admin/reject-coach/${coach.id}`)
        this.$message.success(response.data.message)
        this.loadPendingCoaches()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(error.response?.data?.message || '操作失败')
        }
      }
    },
    
    showCampusDialog(campus) {
      if (campus) {
        this.editingCampus = campus
        this.campusForm = { ...campus }
      } else {
        this.editingCampus = null
        this.campusForm = {
          name: '',
          address: '',
          contactPerson: '',
          contactPhone: '',
          contactEmail: '',
          isMainCampus: false
        }
      }
      this.campusDialogVisible = true
    },
    
    async saveCampus() {
      try {
        await this.$refs.campusForm.validate()
        this.savingCampus = true
        
        let response
        if (this.editingCampus) {
          response = await this.$http.put(`/api/admin/campus/${this.editingCampus.id}`, this.campusForm)
        } else {
          response = await this.$http.post('/api/admin/campus', this.campusForm)
        }
        
        this.$message.success(response.data.message)
        this.campusDialogVisible = false
        this.loadCampuses()
      } catch (error) {
        this.$message.error(error.response?.data?.message || '保存失败')
      } finally {
        this.savingCampus = false
      }
    },
    
    async deleteCampus(campus) {
      try {
        await this.$confirm(`确认删除校区 ${campus.name} 吗？`, '确认删除', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'danger'
        })
        
        const response = await this.$http.delete(`/api/admin/campus/${campus.id}`)
        this.$message.success(response.data.message)
        this.loadCampuses()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(error.response?.data?.message || '删除失败')
        }
      }
    },
    
    handleLogPageChange(page) {
      this.logCurrentPage = page
      this.loadSystemLogs()
    },
    
    getLevelText(level) {
      const levelMap = {
        'SENIOR': '高级教练员',
        'INTERMEDIATE': '中级教练员',
        'JUNIOR': '初级教练员'
      }
      return levelMap[level] || '未知'
    },
    
    async loadUsers() {
      try {
        const response = await this.$http.get('/api/admin/users')
        this.users = response.data.data || []
      } catch (error) {
        this.users = []
        this.$message.error('加载用户列表失败')
      }
    },
    
    async loadCoaches() {
      try {
        const response = await this.$http.get('/api/admin/coaches')
        this.coaches = response.data.data || []
      } catch (error) {
        this.coaches = []
        this.$message.error('加载教练列表失败')
      }
    },
    
    async loadStudents() {
      try {
        const response = await this.$http.get('/api/admin/students')
        this.students = response.data.data || []
      } catch (error) {
        this.students = []
        this.$message.error('加载学员列表失败')
      }
    },
    
    async loadBookings() {
      try {
        const response = await this.$http.get('/api/admin/bookings')
        this.bookings = response.data.data || []
      } catch (error) {
        this.bookings = []
        this.$message.error('加载课程预约失败')
      }
    },
    
    async toggleUserStatus(user) {
      try {
        const action = user.active ? '禁用' : '启用'
        await this.$confirm(`确认${action}用户 ${user.realName} 吗？`, '确认操作', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        const response = await this.$http.post(`/api/admin/toggle-user-status/${user.id}`)
        this.$message.success(response.data.message)
        this.loadUsers()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(error.response?.data?.message || '操作失败')
        }
      }
    },
    
    async toggleCoachStatus(coach) {
      try {
        const action = coach.active ? '禁用' : '启用'
        await this.$confirm(`确认${action}教练 ${coach.realName} 吗？`, '确认操作', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        const response = await this.$http.post(`/api/admin/toggle-coach-status/${coach.id}`)
        this.$message.success(response.data.message)
        this.loadCoaches()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(error.response?.data?.message || '操作失败')
        }
      }
    },
    
    async toggleStudentStatus(student) {
      try {
        const action = student.active ? '禁用' : '启用'
        await this.$confirm(`确认${action}学员 ${student.realName} 吗？`, '确认操作', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        const response = await this.$http.post(`/api/admin/toggle-student-status/${student.id}`)
        this.$message.success(response.data.message)
        this.loadStudents()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(error.response?.data?.message || '操作失败')
        }
      }
    },
    
    showUserDetail(user) {
      this.$alert(`
        <div>
          <p><strong>用户名：</strong>${user.username}</p>
          <p><strong>真实姓名：</strong>${user.realName}</p>
          <p><strong>角色：</strong>${this.getRoleText(user.role)}</p>
          <p><strong>电话：</strong>${user.phone}</p>
          <p><strong>邮箱：</strong>${user.email || '未设置'}</p>
          <p><strong>状态：</strong>${user.active ? '活跃' : '禁用'}</p>
          <p><strong>注册时间：</strong>${this.formatDate(user.createdAt)}</p>
        </div>
      `, '用户详情', {
        dangerouslyUseHTMLString: true
      })
    },
    
    showCoachDetail(coach) {
      this.$alert(`
        <div>
          <p><strong>姓名：</strong>${coach.realName}</p>
          <p><strong>性别：</strong>${coach.gender === 'MALE' ? '男' : '女'}</p>
          <p><strong>年龄：</strong>${coach.age}</p>
          <p><strong>电话：</strong>${coach.phone}</p>
          <p><strong>等级：</strong>${this.getLevelText(coach.level)}</p>
          <p><strong>审核状态：</strong>${this.getApprovalText(coach.approvalStatus)}</p>
          <p><strong>获奖经历：</strong>${coach.achievements || '无'}</p>
          <p><strong>状态：</strong>${coach.active ? '活跃' : '禁用'}</p>
        </div>
      `, '教练详情', {
        dangerouslyUseHTMLString: true
      })
    },
    
    showStudentDetail(student) {
      this.$alert(`
        <div>
          <p><strong>姓名：</strong>${student.realName}</p>
          <p><strong>性别：</strong>${student.gender === 'MALE' ? '男' : '女'}</p>
          <p><strong>年龄：</strong>${student.age}</p>
          <p><strong>电话：</strong>${student.phone}</p>
          <p><strong>账户余额：</strong>¥${student.accountBalance || 0}</p>
          <p><strong>状态：</strong>${student.active ? '活跃' : '禁用'}</p>
          <p><strong>注册时间：</strong>${this.formatDate(student.createdAt)}</p>
        </div>
      `, '学员详情', {
        dangerouslyUseHTMLString: true
      })
    },
    
    getRoleText(role) {
      const roleMap = {
        'SUPER_ADMIN': '超级管理员',
        'CAMPUS_ADMIN': '校区管理员',
        'COACH': '教练',
        'STUDENT': '学员'
      }
      return roleMap[role] || '未知'
    },
    
    getApprovalText(status) {
      const statusMap = {
        'PENDING': '待审核',
        'APPROVED': '已通过',
        'REJECTED': '已拒绝'
      }
      return statusMap[status] || '未知'
    },
    
    getStatusType(status) {
      const typeMap = {
        'PENDING': 'warning',
        'APPROVED': 'success',
        'REJECTED': 'danger'
      }
      return typeMap[status] || 'info'
    },
    
    getBookingStatusType(status) {
      const typeMap = {
        'CONFIRMED': 'success',
        'CANCELLED': 'danger',
        'COMPLETED': 'info',
        'PENDING': 'warning'
      }
      return typeMap[status] || 'info'
    },
    
    getBookingStatusText(status) {
      const statusMap = {
        'CONFIRMED': '已确认',
        'CANCELLED': '已取消',
        'COMPLETED': '已完成',
        'PENDING': '待确认'
      }
      return statusMap[status] || '未知'
    },
    
    formatDate(dateTime) {
      return formatDate(dateTime)
    },
    
    formatDateTime(dateTime) {
      return formatDate(dateTime)
    }
  }
}
</script>

<style scoped>
.admin-panel {
  height: 100vh;
  background: #f5f5f5;
}

.header {
  background: #409EFF;
  color: white;
  padding: 0;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
  padding: 0 20px;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.main-content {
  padding: 20px;
}

.empty-container {
  text-align: center;
  padding: 40px;
}

.pagination-container {
  margin-top: 20px;
  text-align: center;
}

.dialog-footer {
  text-align: right;
}

.role-badge {
  margin-left: 15px;
  font-size: 14px;
}

.campus-info {
  color: #409EFF;
  font-weight: 500;
}
</style> 