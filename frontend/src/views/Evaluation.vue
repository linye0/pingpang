<template>
  <div class="evaluation">
    <el-container>
      <!-- 顶部导航 -->
      <el-header class="header">
        <div class="header-content">
          <h1 class="page-title">课程评价</h1>
          <el-button @click="$router.push('/dashboard')">返回首页</el-button>
        </div>
      </el-header>
      
      <!-- 主要内容 -->
      <el-main class="main-content">
        <el-tabs v-model="activeTab" type="card">
          <!-- 待评价课程 -->
          <el-tab-pane label="待评价课程" name="pending">
            <el-card>
              <div slot="header">
                <span>需要您评价的课程</span>
                <el-button 
                  type="text" 
                  @click="loadPendingEvaluations"
                  style="float: right; padding: 3px 0"
                >
                  刷新
                </el-button>
              </div>
              
              <div v-if="pendingEvaluations.length === 0" class="empty-container">
                <el-empty description="暂无待评价课程" />
              </div>
              
              <div v-else>
                <el-row :gutter="20">
                  <el-col :span="24">
                    <div class="evaluation-list">
                      <el-card 
                        v-for="evaluation in pendingEvaluations" 
                        :key="evaluation.id"
                        class="evaluation-card"
                        shadow="hover"
                      >
                        <div class="evaluation-header">
                          <div class="course-info">
                            <h3>
                              <span v-if="userRole === 'STUDENT'">
                                教练：{{ evaluation.booking.coach.realName }}
                              </span>
                              <span v-else>
                                学员：{{ evaluation.booking.student.realName }}
                              </span>
                            </h3>
                            <p class="course-details">
                              上课时间：{{ formatDate(evaluation.booking.startTime) }} - {{ formatDate(evaluation.booking.endTime) }}
                            </p>
                            <p class="course-details">
                              球台：{{ evaluation.booking.tableNumber }} | 费用：¥{{ evaluation.booking.cost }}
                            </p>
                          </div>
                          <div class="status-badge">
                            <el-tag type="warning">待评价</el-tag>
                          </div>
                        </div>
                        
                        <el-form :model="evaluation.form || {}" label-width="80px" class="evaluation-form">
                          <el-form-item label="评价内容">
                            <el-input
                              type="textarea"
                              :rows="4"
                              v-model="evaluation.form.evaluation"
                              :placeholder="userRole === 'STUDENT' ? '请评价教练的教学质量、态度等' : '请评价学员的学习态度、表现等'"
                              maxlength="500"
                              show-word-limit
                            />
                          </el-form-item>
                          
                          <el-form-item v-if="userRole === 'STUDENT'" label="教学评分">
                            <el-rate
                              v-model="evaluation.form.rating"
                              :max="5"
                              :colors="['#99A9BF', '#F7BA2A', '#FF9900']"
                              void-color="#C6D1DE"
                              show-text
                              :texts="['很差', '差', '一般', '好', '很好']"
                            />
                          </el-form-item>
                          
                          <el-form-item>
                            <el-button 
                              type="primary" 
                              @click="submitEvaluation(evaluation)"
                              :disabled="!evaluation.form.evaluation"
                            >
                              提交评价
                            </el-button>
                          </el-form-item>
                        </el-form>
                      </el-card>
                    </div>
                  </el-col>
                </el-row>
              </div>
            </el-card>
          </el-tab-pane>
          
          <!-- 历史评价 -->
          <el-tab-pane label="历史评价" name="history">
            <el-card>
              <div slot="header">
                <span>评价记录</span>
                <el-button 
                  type="text" 
                  @click="loadMyEvaluations"
                  style="float: right; padding: 3px 0"
                >
                  刷新
                </el-button>
              </div>
              
              <div v-if="myEvaluations.length === 0" class="empty-container">
                <el-empty description="暂无评价记录" />
              </div>
              
              <el-table v-else :data="myEvaluations" stripe>
                <el-table-column 
                  :label="userRole === 'STUDENT' ? '教练' : '学员'" 
                  width="120"
                >
                  <template slot-scope="scope">
                    <span v-if="userRole === 'STUDENT'">
                      {{ scope.row.booking.coach.realName }}
                    </span>
                    <span v-else>
                      {{ scope.row.booking.student.realName }}
                    </span>
                  </template>
                </el-table-column>
                <el-table-column prop="booking.startTime" label="上课时间" width="180">
                  <template slot-scope="scope">
                    {{ formatDate(scope.row.booking.startTime) }}
                  </template>
                </el-table-column>
                <el-table-column prop="booking.tableNumber" label="球台" width="80" />
                <el-table-column 
                  :label="userRole === 'STUDENT' ? '我的评价' : '我的评价'" 
                  min-width="200"
                >
                  <template slot-scope="scope">
                    <span v-if="userRole === 'STUDENT'">
                      {{ scope.row.studentEvaluation || '未评价' }}
                    </span>
                    <span v-else>
                      {{ scope.row.coachEvaluation || '未评价' }}
                    </span>
                  </template>
                </el-table-column>
                <el-table-column 
                  v-if="userRole === 'STUDENT'"
                  label="评分" 
                  width="120"
                >
                  <template slot-scope="scope">
                    <el-rate
                      v-if="scope.row.studentRating"
                      :value="scope.row.studentRating"
                      disabled
                      show-score
                      text-color="#ff9900"
                      :max="5"
                    />
                    <span v-else>未评分</span>
                  </template>
                </el-table-column>
                <el-table-column 
                  :label="userRole === 'STUDENT' ? '教练评价' : '学员评价'" 
                  min-width="200"
                >
                  <template slot-scope="scope">
                    <span v-if="userRole === 'STUDENT'">
                      {{ scope.row.coachEvaluation || '教练暂未评价' }}
                    </span>
                    <span v-else>
                      {{ scope.row.studentEvaluation || '学员暂未评价' }}
                    </span>
                  </template>
                </el-table-column>
                <el-table-column prop="createdAt" label="创建时间" width="150">
                  <template slot-scope="scope">
                    {{ formatDate(scope.row.createdAt) }}
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-tab-pane>
        </el-tabs>
      </el-main>
    </el-container>
  </div>
</template>

<script>
import { formatDate } from '@/utils/dateFormatter'

export default {
  name: 'Evaluation',
  
  data() {
    return {
      activeTab: 'pending',
      pendingEvaluations: [],
      myEvaluations: []
    }
  },
  
  computed: {
    userRole() {
      return this.$store.getters.userRole
    }
  },
  
  created() {
    this.loadPendingEvaluations()
    this.loadMyEvaluations()
  },
  
  methods: {
    async loadPendingEvaluations() {
      try {
        const endpoint = this.userRole === 'STUDENT' ? '/api/student/pending-evaluations' : '/api/coach/pending-evaluations'
        const response = await this.$http.get(endpoint)
        this.pendingEvaluations = (response.data.data || []).map(evaluation => ({
          ...evaluation,
          form: {
            evaluation: '',
            rating: 0
          }
        }))
      } catch (error) {
        this.$message.error('加载待评价课程失败')
        console.error(error)
      }
    },
    
    async loadMyEvaluations() {
      try {
        const endpoint = this.userRole === 'STUDENT' ? '/api/student/my-evaluations' : '/api/coach/my-evaluations'
        const response = await this.$http.get(endpoint)
        this.myEvaluations = response.data.data || []
      } catch (error) {
        this.$message.error('加载评价记录失败')
        console.error(error)
      }
    },
    
    async submitEvaluation(evaluation) {
      try {
        if (!evaluation.form.evaluation.trim()) {
          this.$message.warning('请填写评价内容')
          return
        }
        
        const data = {
          evaluation: evaluation.form.evaluation
        }
        
        if (this.userRole === 'STUDENT' && evaluation.form.rating > 0) {
          data.rating = evaluation.form.rating
        }
        
        const endpoint = this.userRole === 'STUDENT' 
          ? `/api/student/evaluations/${evaluation.id}`
          : `/api/coach/evaluations/${evaluation.id}`
        
        // Convert to query parameters for the backend
        const params = new URLSearchParams(data).toString()
        const response = await this.$http.post(`${endpoint}?${params}`)
        
        this.$message.success(response.data.message || '评价提交成功')
        
        // 刷新数据
        await this.loadPendingEvaluations()
        await this.loadMyEvaluations()
      } catch (error) {
        this.$message.error(error.response?.data?.message || '提交评价失败')
      }
    },
    
    formatDate(dateTime) {
      return formatDate(dateTime)
    }
  }
}
</script>

<style scoped>
.evaluation {
  height: 100vh;
  background: #f5f5f5;
}

.header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
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

.evaluation-list {
  margin-bottom: 20px;
}

.evaluation-card {
  margin-bottom: 20px;
}

.evaluation-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.course-info h3 {
  margin: 0 0 10px 0;
  color: #333;
  font-size: 16px;
}

.course-details {
  margin: 5px 0;
  color: #666;
  font-size: 14px;
}

.status-badge {
  flex-shrink: 0;
}

.evaluation-form {
  margin-top: 15px;
}
</style>