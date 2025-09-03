<template>
  <div class="login-container">
    <div class="login-form">
      <h2 class="title">乒乓球培训管理系统</h2>
      
      <el-form 
        :model="loginForm" 
        :rules="rules" 
        ref="loginForm"
        class="form"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            prefix-icon="el-icon-user"
            size="large"
          />
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="el-icon-lock"
            size="large"
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        
        <el-form-item>
          <el-button 
            type="primary" 
            size="large" 
            :loading="loading"
            @click="handleLogin"
            class="login-btn"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>
      
      <div class="register-links">
        <el-button type="text" @click="$router.push('/register/student')">
          学员注册
        </el-button>
        <el-divider direction="vertical"></el-divider>
        <el-button type="text" @click="$router.push('/register/coach')">
          教练注册
        </el-button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Login',
  data() {
    return {
      loginForm: {
        username: '',
        password: ''
      },
      rules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' }
        ]
      },
      loading: false
    }
  },
  
  created() {
    this.$store.dispatch('initStore')
    if (this.$store.getters.isLoggedIn) {
      this.$router.push('/dashboard')
    }
  },
  
  methods: {
    async handleLogin() {
      try {
        await this.$refs.loginForm.validate()
        this.loading = true
        
        const result = await this.$store.dispatch('login', this.loginForm)
        
        if (result.success) {
          this.$message.success('登录成功')
          // 等待一下确保store状态更新完成
          await this.$nextTick()
          // 再等一下确保token保存完成
          setTimeout(() => {
            this.$router.replace('/dashboard')
          }, 100)
        } else {
          this.$message.error(result.message || '登录失败')
        }
      } catch (error) {
        console.error('登录失败:', error)
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%);
}

.login-form {
  width: 400px;
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

.login-btn {
  width: 100%;
}

.register-links {
  text-align: center;
  color: #666;
}
</style> 