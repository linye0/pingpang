<template>
  <div class="profile">
    <el-container>
      <!-- 顶部导航 -->
      <el-header class="header">
        <div class="header-content">
          <h1 class="page-title">个人信息</h1>
          <el-button @click="$router.push('/dashboard')">返回首页</el-button>
        </div>
      </el-header>
      
      <!-- 主要内容 -->
      <el-main class="main-content">
        <el-row :gutter="20">
          <!-- 左侧：基本信息 -->
          <el-col :span="16">
            <el-card class="profile-card">
              <div slot="header">
                <span>基本信息</span>
                <el-button 
                  type="text" 
                  @click="editing = !editing"
                  style="float: right; padding: 3px 0"
                >
                  {{ editing ? '取消编辑' : '编辑信息' }}
                </el-button>
              </div>
              
              <el-form 
                :model="profileForm" 
                :rules="rules" 
                ref="profileForm"
                label-width="120px"
                :disabled="!editing"
              >
                <el-row :gutter="20">
                  <el-col :span="12">
                    <el-form-item label="用户名">
                      <el-input v-model="profileForm.username" disabled />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="用户角色">
                      <el-input :value="getRoleText(profileForm.role)" disabled />
                    </el-form-item>
                  </el-col>
                </el-row>
                
                <el-row :gutter="20">
                  <el-col :span="12">
                    <el-form-item label="真实姓名" prop="realName">
                      <el-input v-model="profileForm.realName" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="性别" prop="gender">
                      <el-radio-group v-model="profileForm.gender">
                        <el-radio label="MALE">男</el-radio>
                        <el-radio label="FEMALE">女</el-radio>
                      </el-radio-group>
                    </el-form-item>
                  </el-col>
                </el-row>
                
                <el-row :gutter="20">
                  <el-col :span="12">
                    <el-form-item label="年龄" prop="age">
                      <el-input-number v-model="profileForm.age" :min="1" :max="100" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="手机号码">
                      <el-input v-model="profileForm.phone" disabled />
                    </el-form-item>
                  </el-col>
                </el-row>
                
                <el-row :gutter="20">
                  <el-col :span="12">
                    <el-form-item label="邮箱" prop="email">
                      <el-input v-model="profileForm.email" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="所属校区">
                      <el-input :value="profileForm.campus?.name" disabled />
                    </el-form-item>
                  </el-col>
                </el-row>
                
                <!-- 教练特有信息 -->
                <div v-if="profileForm.role === 'COACH'">
                  <el-divider>教练信息</el-divider>
                  
                  <el-row :gutter="20">
                    <el-col :span="12">
                      <el-form-item label="教练等级">
                        <el-input :value="getLevelText(coachInfo.level)" disabled />
                      </el-form-item>
                    </el-col>
                    <el-col :span="12">
                      <el-form-item label="审核状态">
                        <el-tag :type="getApprovalTagType(coachInfo.approvalStatus)">
                          {{ getApprovalText(coachInfo.approvalStatus) }}
                        </el-tag>
                      </el-form-item>
                    </el-col>
                  </el-row>
                  
                  <el-form-item label="获奖经历">
                    <el-input 
                      v-model="coachInfo.achievements"
                      type="textarea"
                      :rows="4"
                      disabled
                    />
                  </el-form-item>
                </div>
                
                <!-- 学员特有信息 -->
                <div v-if="profileForm.role === 'STUDENT'">
                  <el-divider>学员信息</el-divider>
                  
                  <el-row :gutter="20">
                    <el-col :span="12">
                      <el-form-item label="账户余额">
                        <div class="balance-display">
                          <span class="balance-amount">¥{{ studentInfo.accountBalance }}</span>
                          <el-button type="text" @click="$router.push('/payment')">
                            充值
                          </el-button>
                        </div>
                      </el-form-item>
                    </el-col>
                    <el-col :span="12">
                      <el-form-item label="本月取消次数">
                        <span>{{ studentInfo.cancellationCount }}/3</span>
                      </el-form-item>
                    </el-col>
                  </el-row>
                </div>
                
                <el-form-item label="头像">
                  <el-upload
                    class="avatar-uploader"
                    action="#"
                    :show-file-list="false"
                    :before-upload="beforeAvatarUpload"
                    :http-request="uploadAvatar"
                    :disabled="!editing"
                  >
                    <img v-if="profileForm.avatar" :src="profileForm.avatar" class="avatar">
                    <i v-else class="el-icon-plus avatar-uploader-icon"></i>
                  </el-upload>
                </el-form-item>
                
                <el-form-item v-if="editing">
                  <el-button 
                    type="primary" 
                    @click="updateProfile"
                    :loading="updating"
                  >
                    保存修改
                  </el-button>
                  <el-button @click="cancelEdit">取消</el-button>
                </el-form-item>
              </el-form>
            </el-card>
          </el-col>
          
          <!-- 右侧：操作区域 -->
          <el-col :span="8">
            <!-- 修改密码 -->
            <el-card class="password-card">
              <div slot="header">
                <span>修改密码</span>
              </div>
              
              <el-form 
                :model="passwordForm" 
                :rules="passwordRules" 
                ref="passwordForm"
                label-width="100px"
              >
                <el-form-item label="原密码" prop="oldPassword">
                  <el-input 
                    v-model="passwordForm.oldPassword"
                    type="password"
                    placeholder="请输入原密码"
                  />
                </el-form-item>
                
                <el-form-item label="新密码" prop="newPassword">
                  <el-input 
                    v-model="passwordForm.newPassword"
                    type="password"
                    placeholder="请输入新密码"
                  />
                </el-form-item>
                
                <el-form-item label="确认密码" prop="confirmPassword">
                  <el-input 
                    v-model="passwordForm.confirmPassword"
                    type="password"
                    placeholder="请再次输入新密码"
                  />
                </el-form-item>
                
                <el-form-item>
                  <el-button 
                    type="primary" 
                    @click="changePassword"
                    :loading="changingPassword"
                    style="width: 100%"
                  >
                    修改密码
                  </el-button>
                </el-form-item>
              </el-form>
            </el-card>
            
            <!-- 我的教练（学员） -->
            <el-card v-if="profileForm.role === 'STUDENT'" class="coaches-card">
              <div slot="header">
                <span>我的教练</span>
              </div>
              
              <div v-if="myCoaches.length === 0" class="empty-container">
                <el-empty description="暂无教练" />
                <el-button type="primary" @click="$router.push('/coach/search')">
                  选择教练
                </el-button>
              </div>
              
              <div v-else class="coach-list">
                <div 
                  v-for="relation in myCoaches" 
                  :key="relation.id"
                  class="coach-item"
                >
                  <div class="coach-info">
                    <img 
                      v-if="relation.coach.avatar" 
                      :src="relation.coach.avatar" 
                      class="coach-avatar"
                    />
                    <i v-else class="el-icon-user-solid default-avatar"></i>
                    <div class="coach-details">
                      <h4>{{ relation.coach.realName }}</h4>
                      <p>{{ getLevelText(relation.coach.level) }}</p>
                    </div>
                  </div>
                  <el-tag 
                    :type="relation.status === 'APPROVED' ? 'success' : 'warning'"
                    size="small"
                  >
                    {{ getStatusText(relation.status) }}
                  </el-tag>
                </div>
              </div>
            </el-card>
            
            <!-- 我的学员（教练） -->
            <el-card v-if="profileForm.role === 'COACH'" class="students-card">
              <div slot="header">
                <span>我的学员</span>
              </div>
              
              <div v-if="myStudents.length === 0" class="empty-container">
                <el-empty description="暂无学员" />
              </div>
              
              <div v-else class="student-list">
                <div 
                  v-for="relation in myStudents" 
                  :key="relation.id"
                  class="student-item"
                >
                  <div class="student-info">
                    <i class="el-icon-user-solid"></i>
                    <div class="student-details">
                      <h4>{{ relation.student.realName }}</h4>
                      <p>{{ getGenderText(relation.student.gender) }} · {{ relation.student.age }}岁</p>
                    </div>
                  </div>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-main>
    </el-container>
  </div>
</template>

<script>
export default {
  name: 'Profile',
  
  data() {
    const validatePassword = (rule, value, callback) => {
      const passwordRegex = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,16}$/
      if (!passwordRegex.test(value)) {
        callback(new Error('密码必须8-16位，包含字母、数字和特殊字符'))
      } else {
        callback()
      }
    }
    
    const validateConfirmPassword = (rule, value, callback) => {
      if (value !== this.passwordForm.newPassword) {
        callback(new Error('两次输入的密码不一致'))
      } else {
        callback()
      }
    }
    
    return {
      profileForm: {
        username: '',
        realName: '',
        gender: '',
        age: null,
        phone: '',
        email: '',
        role: '',
        campus: {},
        avatar: ''
      },
      originalProfile: {},
      passwordForm: {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
      },
      coachInfo: {},
      studentInfo: {},
      myCoaches: [],
      myStudents: [],
      editing: false,
      updating: false,
      changingPassword: false,
      rules: {
        realName: [
          { required: true, message: '请输入真实姓名', trigger: 'blur' }
        ],
        email: [
          { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
        ]
      },
      passwordRules: {
        oldPassword: [
          { required: true, message: '请输入原密码', trigger: 'blur' }
        ],
        newPassword: [
          { required: true, message: '请输入新密码', trigger: 'blur' },
          { validator: validatePassword, trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, message: '请确认密码', trigger: 'blur' },
          { validator: validateConfirmPassword, trigger: 'blur' }
        ]
      }
    }
  },
  
  created() {
    this.loadProfile()
    if (this.$store.getters.userRole === 'STUDENT') {
      this.loadMyCoaches()
    } else if (this.$store.getters.userRole === 'COACH') {
      this.loadMyStudents()
    }
  },
  
  methods: {
    async loadProfile() {
      try {
        // 根据用户角色选择不同的API端点
        let endpoint = '/api/user/profile'
        if (this.$store.getters.userRole === 'COACH') {
          endpoint = '/api/coach/profile'
        } else if (this.$store.getters.userRole === 'STUDENT') {
          endpoint = '/api/user/profile' // 学员使用通用用户接口
        }
        
        const response = await this.$http.get(endpoint)
        this.profileForm = { ...response.data.data }
        this.originalProfile = { ...response.data.data }
        
        if (this.profileForm.role === 'COACH') {
          this.coachInfo = { ...response.data.data }
        } else if (this.profileForm.role === 'STUDENT') {
          this.studentInfo = { ...response.data.data }
        }
      } catch (error) {
        console.error('加载个人信息失败:', error)
        this.$message.error('加载个人信息失败')
      }
    },
    
    async loadMyCoaches() {
      try {
        const response = await this.$http.get('/api/student/my-coaches')
        this.myCoaches = response.data.data || []
      } catch (error) {
        console.error('加载我的教练失败:', error)
        this.myCoaches = []
      }
    },
    
    async loadMyStudents() {
      try {
        const response = await this.$http.get('/api/coach/my-students')
        this.myStudents = response.data.data
      } catch (error) {
        console.error('加载我的学员失败:', error)
      }
    },
    
    async updateProfile() {
      try {
        await this.$refs.profileForm.validate()
        this.updating = true
        
        // 根据用户角色选择不同的API端点
        let endpoint = '/api/user/profile'
        if (this.$store.getters.userRole === 'COACH') {
          endpoint = '/api/coach/profile'
        } else if (this.$store.getters.userRole === 'STUDENT') {
          endpoint = '/api/user/profile' // 学员使用通用用户接口
        }
        
        const response = await this.$http.put(endpoint, this.profileForm)
        this.$message.success(response.data.message || '更新成功')
        
        this.editing = false
        this.originalProfile = { ...this.profileForm }
        
        // 更新store中的用户信息
        this.$store.commit('SET_USER', this.profileForm)
      } catch (error) {
        console.error('更新个人信息失败:', error)
        this.$message.error(error.response?.data?.message || '更新失败')
      } finally {
        this.updating = false
      }
    },
    
    async changePassword() {
      try {
        await this.$refs.passwordForm.validate()
        this.changingPassword = true
        
        console.log('发送修改密码请求:', {
          url: '/api/user/change-password',
          data: {
            oldPassword: this.passwordForm.oldPassword ? '***' : '',
            newPassword: this.passwordForm.newPassword ? '***' : '',
            confirmPassword: this.passwordForm.confirmPassword ? '***' : ''
          }
        })
        
        const response = await this.$http.post('/api/user/change-password', this.passwordForm)
        console.log('修改密码响应:', response.data)
        this.$message.success(response.data.message || '密码修改成功')
        
        // 重置表单
        this.passwordForm = {
          oldPassword: '',
          newPassword: '',
          confirmPassword: ''
        }
        this.$refs.passwordForm.resetFields()
      } catch (error) {
        console.error('修改密码失败:', error)
        console.error('错误详情:', {
          response: error.response,
          message: error.message,
          status: error.response?.status,
          data: error.response?.data
        })
        this.$message.error(error.response?.data?.message || '修改密码失败')
      } finally {
        this.changingPassword = false
      }
    },
    
    cancelEdit() {
      this.profileForm = { ...this.originalProfile }
      this.editing = false
    },
    
    beforeAvatarUpload(file) {
      const isJPG = file.type === 'image/jpeg' || file.type === 'image/png'
      const isLt2M = file.size / 1024 / 1024 < 2
      
      if (!isJPG) {
        this.$message.error('头像图片只能是 JPG/PNG 格式!')
      }
      if (!isLt2M) {
        this.$message.error('头像图片大小不能超过 2MB!')
      }
      return isJPG && isLt2M
    },
    
    uploadAvatar(params) {
      const reader = new FileReader()
      reader.onload = (e) => {
        this.profileForm.avatar = e.target.result
      }
      reader.readAsDataURL(params.file)
    },
    
    getRoleText(role) {
      const roleMap = {
        'STUDENT': '学员',
        'COACH': '教练员',
        'CAMPUS_ADMIN': '校区管理员',
        'SUPER_ADMIN': '超级管理员'
      }
      return roleMap[role] || '未知'
    },
    
    getLevelText(level) {
      const levelMap = {
        'SENIOR': '高级教练员',
        'INTERMEDIATE': '中级教练员',
        'JUNIOR': '初级教练员'
      }
      return levelMap[level] || '未知'
    },
    
    getApprovalText(status) {
      const statusMap = {
        'PENDING': '待审核',
        'APPROVED': '已通过',
        'REJECTED': '已拒绝'
      }
      return statusMap[status] || '未知'
    },
    
    getApprovalTagType(status) {
      const typeMap = {
        'PENDING': 'warning',
        'APPROVED': 'success',
        'REJECTED': 'danger'
      }
      return typeMap[status] || 'info'
    },
    
    getStatusText(status) {
      const statusMap = {
        'PENDING': '待确认',
        'APPROVED': '已确认',
        'REJECTED': '已拒绝'
      }
      return statusMap[status] || '未知'
    },
    
    getGenderText(gender) {
      return gender === 'MALE' ? '男' : '女'
    }
  }
}
</script>

<style scoped>
.profile {
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

.profile-card, .password-card, .coaches-card, .students-card {
  margin-bottom: 20px;
}

.balance-display {
  display: flex;
  align-items: center;
  gap: 10px;
}

.balance-amount {
  font-size: 18px;
  font-weight: 600;
  color: #67C23A;
}

.avatar-uploader .el-upload {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

.avatar-uploader .el-upload:hover {
  border-color: #409EFF;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 100px;
  height: 100px;
  line-height: 100px;
  text-align: center;
}

.avatar {
  width: 100px;
  height: 100px;
  display: block;
}

.empty-container {
  text-align: center;
  padding: 20px;
}

.coach-list, .student-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.coach-item, .student-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
  border: 1px solid #eee;
  border-radius: 6px;
  background: #fafafa;
}

.coach-info, .student-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.coach-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.default-avatar {
  width: 40px;
  height: 40px;
  line-height: 40px;
  text-align: center;
  background: #f0f0f0;
  border-radius: 50%;
  font-size: 20px;
  color: #ccc;
}

.coach-details h4, .student-details h4 {
  margin: 0 0 5px 0;
  font-size: 14px;
  color: #333;
}

.coach-details p, .student-details p {
  margin: 0;
  color: #666;
  font-size: 12px;
}
</style> 