<template>
  <div class="coach-search">
    <el-container>
      <!-- 顶部导航 -->
      <el-header class="header">
        <div class="header-content">
          <h1 class="page-title">查找教练</h1>
          <el-button @click="$router.push('/dashboard')">返回首页</el-button>
        </div>
      </el-header>
      
      <!-- 主要内容 -->
      <el-main class="main-content">
        <!-- 搜索表单 -->
        <el-card class="search-card">
          <div slot="header">
            <span>搜索条件</span>
          </div>
          
          <el-form :model="searchForm" inline class="search-form">
            <el-form-item label="教练姓名">
              <el-input 
                v-model="searchForm.name" 
                placeholder="请输入教练姓名"
                clearable
              />
            </el-form-item>
            
            <el-form-item label="性别">
              <el-select v-model="searchForm.gender" placeholder="请选择性别" clearable>
                <el-option label="男" value="MALE" />
                <el-option label="女" value="FEMALE" />
              </el-select>
            </el-form-item>
            
            <el-form-item label="年龄">
              <el-input-number 
                v-model="searchForm.age" 
                :min="18" 
                :max="65" 
                placeholder="请输入年龄"
              />
            </el-form-item>
            
            <el-form-item>
              <el-button type="primary" @click="searchCoaches" :loading="searching">
                搜索
              </el-button>
              <el-button @click="resetSearch">重置</el-button>
              <el-button @click="loadAllCoaches">浏览所有教练</el-button>
            </el-form-item>
          </el-form>
        </el-card>
        
        <!-- 教练列表 -->
        <el-card class="coach-list-card">
          <div slot="header">
            <span>教练列表 ({{ coaches.length }}人)</span>
          </div>
          
          <div v-if="loading" class="loading-container">
            <el-loading-directive></el-loading-directive>
          </div>
          
          <div v-else-if="coaches.length === 0" class="empty-container">
            <el-empty description="暂无教练数据" />
          </div>
          
          <div v-else class="coach-grid">
            <el-card 
              v-for="coach in coaches" 
              :key="coach.id" 
              class="coach-card"
              :body-style="{ padding: '20px' }"
            >
              <div class="coach-info">
                <div class="coach-avatar">
                  <img 
                    v-if="coach.avatar" 
                    :src="coach.avatar" 
                    :alt="coach.realName"
                    class="avatar-img"
                  />
                  <i v-else class="el-icon-user-solid default-avatar"></i>
                </div>
                
                <div class="coach-details">
                  <h3 class="coach-name">{{ coach.realName }}</h3>
                  <p class="coach-level">{{ getLevelText(coach.level) }}</p>
                  <p class="coach-rate">{{ coach.level && getLevelRate(coach.level) }}元/小时</p>
                  
                  <div class="coach-basic-info">
                    <span class="info-item">
                      <i class="el-icon-user"></i>
                      {{ getGenderText(coach.gender) }}
                    </span>
                    <span class="info-item">
                      <i class="el-icon-time"></i>
                      {{ coach.age }}岁
                    </span>
                    <span class="info-item">
                      <i class="el-icon-phone"></i>
                      {{ coach.phone }}
                    </span>
                  </div>
                  
                  <div v-if="coach.achievements" class="coach-achievements">
                    <h4>获奖经历：</h4>
                    <p>{{ coach.achievements }}</p>
                  </div>
                </div>
                
                <div class="coach-actions">
                  <el-button 
                    type="primary" 
                    @click="selectCoach(coach)"
                    :disabled="isCoachSelected(coach.id)"
                  >
                    {{ isCoachSelected(coach.id) ? '已选择' : '选择教练' }}
                  </el-button>
                </div>
              </div>
            </el-card>
          </div>
        </el-card>
        
        <!-- 我的教练 -->
        <el-card class="my-coaches-card">
          <div slot="header">
            <span>我的教练 ({{ myCoaches.length }}/2)</span>
          </div>
          
          <div v-if="myCoaches.length === 0" class="empty-container">
            <el-empty description="您还没有选择教练" />
          </div>
          
          <div v-else class="my-coach-list">
            <div 
              v-for="relation in myCoaches" 
              :key="relation.id"
              class="my-coach-item"
            >
              <div class="coach-basic">
                <img 
                  v-if="relation.coach.avatar" 
                  :src="relation.coach.avatar" 
                  class="small-avatar"
                />
                <i v-else class="el-icon-user-solid small-default-avatar"></i>
                <div class="coach-info">
                  <h4>{{ relation.coach.realName }}</h4>
                  <p>{{ getLevelText(relation.coach.level) }}</p>
                </div>
              </div>
              
              <div class="relation-status">
                <el-tag 
                  :type="relation.status === 'APPROVED' ? 'success' : 'warning'"
                >
                  {{ getStatusText(relation.status) }}
                </el-tag>
              </div>
            </div>
          </div>
        </el-card>
      </el-main>
    </el-container>
  </div>
</template>

<script>
export default {
  name: 'CoachSearch',
  
  data() {
    return {
      searchForm: {
        name: '',
        gender: '',
        age: null
      },
      coaches: [],
      myCoaches: [],
      loading: false,
      searching: false
    }
  },
  
  created() {
    this.loadAllCoaches()
    this.loadMyCoaches()
  },
  
  methods: {
    async searchCoaches() {
      // 验证搜索条件
      if (!this.searchForm.name && !this.searchForm.gender && !this.searchForm.age) {
        this.$message.warning('请至少填写一个搜索条件')
        return
      }
      
      this.searching = true
      try {
        const params = {}
        if (this.searchForm.name) params.name = this.searchForm.name
        if (this.searchForm.gender) params.gender = this.searchForm.gender
        if (this.searchForm.age) params.age = this.searchForm.age
        
        const response = await this.$http.get('/api/student/coaches/search', { params })
        this.coaches = response.data.data || []
        
        if (this.coaches.length === 0) {
          this.$message.info('没有找到符合条件的教练')
        }
      } catch (error) {
        this.$message.error('搜索失败: ' + (error.response?.data?.message || error.message))
        console.error('搜索教练失败:', error)
      } finally {
        this.searching = false
      }
    },
    
    async loadAllCoaches() {
      this.loading = true
      try {
        const response = await this.$http.get('/api/student/coaches')
        this.coaches = response.data.data || []
        if (this.coaches.length === 0) {
          this.$message.info('当前校区暂无可选教练')
        }
      } catch (error) {
        this.$message.error('加载教练列表失败: ' + (error.response?.data?.message || error.message))
        console.error('加载教练列表失败:', error)
      } finally {
        this.loading = false
      }
    },
    
    async loadMyCoaches() {
      try {
        const response = await this.$http.get('/api/student/my-coaches')
        this.myCoaches = response.data.data || []
      } catch (error) {
        console.error('加载我的教练失败:', error)
      }
    },
    
    resetSearch() {
      this.searchForm = {
        name: '',
        gender: '',
        age: null
      }
      this.loadAllCoaches()
    },
    
    async selectCoach(coach) {
      if (this.myCoaches.length >= 2) {
        this.$message.warning('最多只能选择2位教练')
        return
      }
      
      try {
        const response = await this.$http.post(`/api/student/select-coach/${coach.id}`)
        this.$message.success(response.data.message)
        this.loadMyCoaches() // 刷新我的教练列表
      } catch (error) {
        this.$message.error(error.response?.data?.message || '选择教练失败')
      }
    },
    
    isCoachSelected(coachId) {
      return this.myCoaches.some(relation => relation.coach.id === coachId)
    },
    
    getLevelText(level) {
      const levelMap = {
        'SENIOR': '高级教练员',
        'INTERMEDIATE': '中级教练员',
        'JUNIOR': '初级教练员'
      }
      return levelMap[level] || '未知'
    },
    
    getLevelRate(level) {
      const rateMap = {
        'SENIOR': '200',
        'INTERMEDIATE': '150',
        'JUNIOR': '80'
      }
      return rateMap[level] || '0'
    },
    
    getGenderText(gender) {
      return gender === 'MALE' ? '男' : '女'
    },
    
    getStatusText(status) {
      const statusMap = {
        'PENDING': '待确认',
        'APPROVED': '已确认',
        'REJECTED': '已拒绝'
      }
      return statusMap[status] || '未知'
    }
  }
}
</script>

<style scoped>
.coach-search {
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

.search-card, .coach-list-card, .my-coaches-card {
  margin-bottom: 20px;
}

.search-form {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
}

.coach-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 20px;
}

.coach-card {
  transition: all 0.3s;
}

.coach-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.coach-info {
  display: flex;
  gap: 15px;
}

.coach-avatar {
  width: 80px;
  height: 80px;
  flex-shrink: 0;
}

.avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.default-avatar {
  width: 80px;
  height: 80px;
  line-height: 80px;
  text-align: center;
  background: #f0f0f0;
  border-radius: 50%;
  font-size: 32px;
  color: #ccc;
}

.coach-details {
  flex: 1;
}

.coach-name {
  margin: 0 0 5px 0;
  color: #333;
  font-size: 18px;
}

.coach-level {
  margin: 0 0 5px 0;
  color: #409EFF;
  font-weight: 600;
}

.coach-rate {
  margin: 0 0 10px 0;
  color: #E6A23C;
  font-weight: 600;
}

.coach-basic-info {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  margin-bottom: 10px;
}

.info-item {
  color: #666;
  font-size: 14px;
}

.info-item i {
  margin-right: 5px;
}

.coach-achievements h4 {
  margin: 0 0 5px 0;
  color: #333;
  font-size: 14px;
}

.coach-achievements p {
  margin: 0;
  color: #666;
  font-size: 13px;
  line-height: 1.4;
}

.coach-actions {
  display: flex;
  align-items: center;
}

.loading-container, .empty-container {
  text-align: center;
  padding: 40px;
}

.my-coach-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.my-coach-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  border: 1px solid #eee;
  border-radius: 8px;
  background: #fafafa;
}

.coach-basic {
  display: flex;
  align-items: center;
  gap: 10px;
}

.small-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.small-default-avatar {
  width: 40px;
  height: 40px;
  line-height: 40px;
  text-align: center;
  background: #f0f0f0;
  border-radius: 50%;
  font-size: 20px;
  color: #ccc;
}

.coach-info h4 {
  margin: 0 0 5px 0;
  font-size: 16px;
}

.coach-info p {
  margin: 0;
  color: #666;
  font-size: 14px;
}
</style> 