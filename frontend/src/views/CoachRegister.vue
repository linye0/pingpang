<template>
  <div class="register-container">
    <div class="register-form">
      <h2 class="title">教练注册</h2>
      
      <el-form 
        :model="form" 
        :rules="rules" 
        ref="registerForm"
        class="form"
        label-width="120px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="3-20个字符" />
        </el-form-item>
        
        <el-form-item label="密码" prop="password">
          <el-input 
            v-model="form.password" 
            type="password" 
            placeholder="8-16位，包含字母、数字和特殊字符" 
          />
        </el-form-item>
        
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input 
            v-model="form.confirmPassword" 
            type="password" 
            placeholder="请再次输入密码" 
          />
        </el-form-item>
        
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="form.gender">
            <el-radio label="MALE">男</el-radio>
            <el-radio label="FEMALE">女</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item label="年龄" prop="age">
          <el-input-number v-model="form.age" :min="18" :max="65" />
        </el-form-item>
        
        <el-form-item label="手机号码" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号码" />
        </el-form-item>
        
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱地址" />
        </el-form-item>
        
        <el-form-item label="选择校区" prop="campusId">
          <el-select v-model="form.campusId" placeholder="请选择校区" style="width: 100%">
            <el-option 
              v-for="campus in campuses" 
              :key="campus.id" 
              :label="campus.name" 
              :value="campus.id"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="教练等级" prop="level">
          <el-select v-model="form.level" placeholder="请选择教练等级" style="width: 100%">
            <el-option label="初级教练员 (80元/小时)" value="JUNIOR" />
            <el-option label="中级教练员 (150元/小时)" value="INTERMEDIATE" />
            <el-option label="高级教练员 (200元/小时)" value="SENIOR" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="获奖经历" prop="achievements">
          <el-input 
            v-model="form.achievements"
            type="textarea"
            :rows="4"
            placeholder="请简要描述您的乒乓球比赛获奖经历和教学经验"
          />
        </el-form-item>
        
        <el-form-item label="教练照片">
          <el-upload
            class="avatar-uploader"
            action="#"
            :show-file-list="false"
            :before-upload="beforeAvatarUpload"
            :http-request="uploadAvatar"
          >
            <img v-if="form.avatar" :src="form.avatar" class="avatar">
            <i v-else class="el-icon-plus avatar-uploader-icon"></i>
          </el-upload>
        </el-form-item>
        
        <el-form-item>
          <el-button 
            type="primary" 
            :loading="loading"
            @click="handleRegister"
            style="width: 100%"
          >
            提交注册申请
          </el-button>
        </el-form-item>
      </el-form>
      
      <div class="back-link">
        <el-button type="text" @click="$router.push('/login')">
          返回登录
        </el-button>
      </div>
    </div>
  </div>
</template>

<script>
import userService from '../services/userService'

export default {
  name: 'CoachRegister',
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
      if (value !== this.form.password) {
        callback(new Error('两次输入的密码不一致'))
      } else {
        callback()
      }
    }
    
    return {
      form: {
        username: '',
        password: '',
        confirmPassword: '',
        realName: '',
        gender: '',
        age: null,
        phone: '',
        email: '',
        campusId: null,
        level: '',
        achievements: '',
        avatar: '',
        campus: {}
      },
      rules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' },
          { min: 3, max: 20, message: '用户名长度在3-20个字符', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { validator: validatePassword, trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, message: '请确认密码', trigger: 'blur' },
          { validator: validateConfirmPassword, trigger: 'blur' }
        ],
        realName: [
          { required: true, message: '请输入真实姓名', trigger: 'blur' }
        ],
        phone: [
          { required: true, message: '请输入手机号码', trigger: 'blur' },
          { pattern: /^1[3-9]\d{9}$/, message: '手机号码格式不正确', trigger: 'blur' }
        ],
        campusId: [
          { required: true, message: '请选择校区', trigger: 'change' }
        ],
        level: [
          { required: true, message: '请选择教练等级', trigger: 'change' }
        ],
        achievements: [
          { required: true, message: '请填写获奖经历', trigger: 'blur' }
        ]
      },
      campuses: [],
      loading: false
    }
  },
  
  async created() {
    await this.fetchCampuses()
  },
  
  methods: {
    async fetchCampuses() {
      try {
        const response = await this.$http.get('/api/public/campuses')
        this.campuses = response.data.data
      } catch (error) {
        this.$message.error('获取校区列表失败')
      }
    },
    
    async handleRegister() {
      try {
        await this.$refs.registerForm.validate()
        this.loading = true
        
        // 设置校区对象
        this.form.campus = { id: this.form.campusId }
        
        const response = await userService.registerCoach(this.form)
        
        if (response.code === 200) {
          this.$message.success('注册申请已提交，请等待管理员审核！')
          this.$router.push('/login')
        } else {
          this.$message.error(response.message)
        }
      } catch (error) {
        this.$message.error(error.response?.data?.message || '注册失败')
      } finally {
        this.loading = false
      }
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
      // 模拟上传头像（实际项目中应该上传到服务器）
      const reader = new FileReader()
      reader.onload = (e) => {
        this.form.avatar = e.target.result
      }
      reader.readAsDataURL(params.file)
    }
  }
}
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.register-form {
  width: 600px;
  max-height: 90vh;
  overflow-y: auto;
  padding: 40px;
  background: white;
  border-radius: 10px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
}

.title {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
  font-weight: 600;
}

.form {
  margin-bottom: 20px;
}

.back-link {
  text-align: center;
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
</style> 