<template>
  <div class="coach-students">
    <el-container>
      <!-- 顶部导航 -->
      <el-header class="header">
        <div class="header-content">
          <h1 class="page-title">学员管理</h1>
          <el-button @click="$router.push('/dashboard')">返回首页</el-button>
        </div>
      </el-header>
      
      <!-- 主要内容 -->
      <el-main class="main-content">
        <el-tabs v-model="activeTab" type="card">
          
          <!-- 待审核申请 -->
          <el-tab-pane label="待审核申请" name="pending">
            <el-card>
              <div slot="header">
                <span>学员申请列表</span>
                <el-button 
                  type="text" 
                  @click="loadPendingRelations"
                  style="float: right; padding: 3px 0"
                >
                  刷新
                </el-button>
              </div>
              
              <div v-if="pendingRelations.length === 0" class="empty-container">
                <el-empty description="暂无待审核申请" />
              </div>
              
              <el-table v-else :data="pendingRelations" stripe>
                <el-table-column prop="student.realName" label="学员姓名" width="120" />
                <el-table-column prop="student.phone" label="联系电话" width="130" />
                <el-table-column prop="student.email" label="邮箱" min-width="180" />
                <el-table-column prop="student.age" label="年龄" width="80" />
                <el-table-column prop="student.level" label="水平" width="100">
                  <template slot-scope="scope">
                    <el-tag size="small">{{ getLevelText(scope.row.student.level) }}</el-tag>
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
                      size="mini" 
                      type="success" 
                      @click="approveRelation(scope.row)"
                    >
                      同意
                    </el-button>
                    <el-button 
                      size="mini" 
                      type="danger" 
                      @click="rejectRelation(scope.row)"
                    >
                      拒绝
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-tab-pane>
          
          <!-- 我的学员 -->
          <el-tab-pane label="我的学员" name="approved">
            <el-card>
              <div slot="header">
                <span>已确认学员 ({{ myStudents.length }}/20)</span>
                <el-button 
                  type="text" 
                  @click="loadMyStudents"
                  style="float: right; padding: 3px 0"
                >
                  刷新
                </el-button>
              </div>
              
              <div v-if="myStudents.length === 0" class="empty-container">
                <el-empty description="暂无学员" />
              </div>
              
              <el-row :gutter="20" v-else>
                <el-col :span="8" v-for="relation in myStudents" :key="relation.id">
                  <el-card class="student-card" shadow="hover">
                    <div class="student-header">
                      <h3>{{ relation.student.realName }}</h3>
                      <el-tag size="small" type="success">已确认</el-tag>
                    </div>
                    
                    <div class="student-info">
                      <p><i class="el-icon-phone"></i> {{ relation.student.phone }}</p>
                      <p><i class="el-icon-message"></i> {{ relation.student.email }}</p>
                      <p><i class="el-icon-user"></i> {{ relation.student.age }}岁</p>
                      <p><i class="el-icon-trophy"></i> {{ getLevelText(relation.student.level) }}</p>
                      <p><i class="el-icon-date"></i> 确认时间: {{ formatDate(relation.updatedAt) }}</p>
                    </div>
                    
                    <div class="student-actions">
                      <el-button 
                        size="small" 
                        type="primary"
                        @click="viewStudentSchedule(relation.student)"
                      >
                        查看课表
                      </el-button>
                      <el-button 
                        size="small" 
                        type="info"
                        @click="viewStudentEvaluations(relation.student)"
                      >
                        课程评价
                      </el-button>
                    </div>
                  </el-card>
                </el-col>
              </el-row>
            </el-card>
          </el-tab-pane>
          
          <!-- 课程评价 -->
          <el-tab-pane label="课程评价" name="evaluations">
            <el-card>
              <div slot="header">
                <span>待评价课程</span>
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
                            <h3>学员：{{ evaluation.booking.student.realName }}</h3>
                            <p class="course-details">
                              上课时间：{{ formatDate(evaluation.booking.startTime) }} - {{ formatDate(evaluation.booking.endTime) }}
                            </p>
                            <p class="course-details">
                              球台：{{ evaluation.booking.tableNumber }} | 费用：¥{{ evaluation.booking.cost }}
                            </p>
                            <div v-if="evaluation.studentEvaluation" class="student-feedback">
                              <p><strong>学员评价：</strong>{{ evaluation.studentEvaluation }}</p>
                              <div v-if="evaluation.studentRating" class="student-rating">
                                <span>学员评分：</span>
                                <el-rate
                                  :value="evaluation.studentRating"
                                  disabled
                                  show-score
                                  text-color="#ff9900"
                                  :max="5"
                                />
                              </div>
                            </div>
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
                              placeholder="请评价学员的学习态度、表现等"
                              maxlength="500"
                              show-word-limit
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
          
        </el-tabs>
      </el-main>
    </el-container>
    
    <!-- 学员课表对话框 -->
    <el-dialog title="学员课表" :visible.sync="scheduleDialogVisible" width="70%">
      <div v-if="selectedStudent">
        <h3>{{ selectedStudent.realName }} 的课程安排</h3>
        
        <div v-if="studentBookings.length === 0" class="empty-container">
          <el-empty description="暂无课程安排" />
        </div>
        
        <el-table v-else :data="studentBookings" stripe>
          <el-table-column prop="startTime" label="上课时间" width="150">
            <template slot-scope="scope">
              {{ formatDate(scope.row.startTime, 'MM-DD HH:mm') }}
            </template>
          </el-table-column>
          <el-table-column prop="endTime" label="结束时间" width="150">
            <template slot-scope="scope">
              {{ formatDate(scope.row.endTime, 'MM-DD HH:mm') }}
            </template>
          </el-table-column>
          <el-table-column prop="tableNumber" label="球台" width="100" />
          <el-table-column prop="cost" label="费用" width="100">
            <template slot-scope="scope">
              ¥{{ scope.row.cost }}
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template slot-scope="scope">
              <el-tag :type="getBookingStatusType(scope.row.status)">
                {{ getBookingStatusText(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="remarks" label="备注" min-width="200" />
        </el-table>
      </div>
    </el-dialog>
    
    <!-- 学员评价对话框 -->
    <el-dialog title="学员评价记录" :visible.sync="evaluationDialogVisible" width="70%">
      <div v-if="selectedStudent">
        <h3>{{ selectedStudent.realName }} 的评价记录</h3>
        
        <div v-if="studentEvaluations.length === 0" class="empty-container">
          <el-empty description="暂无评价记录" />
        </div>
        
        <div v-else>
          <el-card 
            v-for="evaluation in studentEvaluations" 
            :key="evaluation.id"
            class="evaluation-record-card"
            shadow="hover"
          >
            <div class="evaluation-record-header">
              <h4>课程时间：{{ formatDate(evaluation.booking.startTime, 'YYYY-MM-DD HH:mm') }} - {{ formatDate(evaluation.booking.endTime, 'HH:mm') }}</h4>
              <span class="course-cost">费用：¥{{ evaluation.booking.cost }}</span>
            </div>
            
            <div class="evaluation-content">
              <!-- 学员评价 -->
              <div v-if="evaluation.studentEvaluation" class="student-eval-section">
                <h5><i class="el-icon-user"></i> 学员评价</h5>
                <p class="eval-text">{{ evaluation.studentEvaluation }}</p>
                <div v-if="evaluation.studentRating" class="rating-section">
                  <span>评分：</span>
                  <el-rate
                    :value="evaluation.studentRating"
                    disabled
                    show-score
                    text-color="#ff9900"
                    :max="5"
                  />
                </div>
              </div>
              
              <!-- 教练评价 -->
              <div v-if="evaluation.coachEvaluation" class="coach-eval-section">
                <h5><i class="el-icon-star-on"></i> 我的评价</h5>
                <p class="eval-text">{{ evaluation.coachEvaluation }}</p>
              </div>
              
              <!-- 如果都没有评价 -->
              <div v-if="!evaluation.studentEvaluation && !evaluation.coachEvaluation" class="no-eval">
                <p>暂无评价记录</p>
              </div>
            </div>
            
            <div class="eval-time">
              评价时间：{{ formatDate(evaluation.updatedAt) }}
            </div>
          </el-card>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { formatDate } from '@/utils/dateFormatter'

export default {
  name: 'CoachStudents',
  
  data() {
    return {
      activeTab: 'pending',
      pendingRelations: [],
      myStudents: [],
      pendingEvaluations: [],
      scheduleDialogVisible: false,
      evaluationDialogVisible: false,
      selectedStudent: null,
      studentBookings: [], // 新增：用于存储学员的课程安排
      studentEvaluations: [] // 新增：用于存储学员的评价记录
    }
  },
  
  created() {
    this.loadPendingRelations()
    this.loadMyStudents()
    this.loadPendingEvaluations()
  },
  
  methods: {
    async loadPendingRelations() {
      try {
        console.log('=== 加载待审核申请 ===')
        const response = await this.$http.get('/api/coach/pending-relations')
        console.log('待审核申请响应:', response.data)
        this.pendingRelations = response.data.data || []
      } catch (error) {
        console.error('加载待审核申请失败:', error)
        this.$message.error('加载待审核申请失败')
      }
    },
    
    async loadMyStudents() {
      try {
        console.log('=== 加载我的学员 ===')
        const response = await this.$http.get('/api/coach/my-students')
        console.log('我的学员响应:', response.data)
        this.myStudents = response.data.data || []
      } catch (error) {
        console.error('加载我的学员失败:', error)
        this.$message.error('加载我的学员失败')
      }
    },
    
    async loadPendingEvaluations() {
      try {
        console.log('=== 加载待评价课程 ===')
        const response = await this.$http.get('/api/coach/pending-evaluations')
        console.log('待评价课程响应:', response.data)
        this.pendingEvaluations = (response.data.data || []).map(evaluation => ({
          ...evaluation,
          form: {
            evaluation: ''
          }
        }))
      } catch (error) {
        console.error('加载待评价课程失败:', error)
        this.$message.error('加载待评价课程失败')
      }
    },
    
    async approveRelation(relation) {
      try {
        await this.$confirm('确认同意该学员的申请吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'success'
        })
        
        const response = await this.$http.post(`/api/coach/approve-relation/${relation.id}`)
        this.$message.success(response.data.message || '已同意申请')
        
        // 刷新列表
        this.loadPendingRelations()
        this.loadMyStudents()
      } catch (error) {
        if (error === 'cancel') return
        console.error('同意申请失败:', error)
        this.$message.error(error.response?.data?.message || '同意申请失败')
      }
    },
    
    async rejectRelation(relation) {
      try {
        await this.$confirm('确认拒绝该学员的申请吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        const response = await this.$http.post(`/api/coach/reject-relation/${relation.id}`)
        this.$message.success(response.data.message || '已拒绝申请')
        
        // 刷新列表
        this.loadPendingRelations()
      } catch (error) {
        if (error === 'cancel') return
        console.error('拒绝申请失败:', error)
        this.$message.error(error.response?.data?.message || '拒绝申请失败')
      }
    },
    
    async submitEvaluation(evaluation) {
      try {
        if (!evaluation.form.evaluation.trim()) {
          this.$message.warning('请填写评价内容')
          return
        }
        
        const params = new URLSearchParams({
          evaluation: evaluation.form.evaluation
        }).toString()
        
        const response = await this.$http.post(`/api/coach/evaluations/${evaluation.id}?${params}`)
        this.$message.success(response.data.message || '评价提交成功')
        
        // 刷新数据
        this.loadPendingEvaluations()
      } catch (error) {
        console.error('提交评价失败:', error)
        this.$message.error(error.response?.data?.message || '提交评价失败')
      }
    },
    
    async viewStudentSchedule(student) {
      try {
        this.selectedStudent = student
        const response = await this.$http.get(`/api/coach/student-bookings/${student.id}`)
        this.studentBookings = response.data.data || []
        this.scheduleDialogVisible = true
      } catch (error) {
        console.error('加载学员课表失败:', error)
        this.$message.error('加载学员课表失败')
      }
    },
    
    async viewStudentEvaluations(student) {
      try {
        this.selectedStudent = student
        const response = await this.$http.get(`/api/coach/student-evaluations/${student.id}`)
        this.studentEvaluations = response.data.data || []
        this.evaluationDialogVisible = true
      } catch (error) {
        console.error('加载学员评价记录失败:', error)
        this.$message.error('加载学员评价记录失败')
      }
    },
    
    getLevelText(level) {
      const levelMap = {
        'BEGINNER': '初级',
        'INTERMEDIATE': '中级',
        'ADVANCED': '高级'
      }
      return levelMap[level] || level
    },
    
    formatDate(dateTime, format = 'YYYY-MM-DD HH:mm') {
      return formatDate(dateTime, format)
    },

    getBookingStatusType(status) {
      switch (status) {
        case 'PENDING':
          return 'warning'
        case 'CONFIRMED':
          return 'success'
        case 'COMPLETED':
          return 'success'
        case 'CANCELLED':
          return 'danger'
        case 'PENDING_CANCELLATION':
          return 'warning'
        default:
          return 'info'
      }
    },

    getBookingStatusText(status) {
      switch (status) {
        case 'PENDING':
          return '待确认'
        case 'CONFIRMED':
          return '已确认'
        case 'COMPLETED':
          return '已完成'
        case 'CANCELLED':
          return '已取消'
        case 'PENDING_CANCELLATION':
          return '待取消确认'
        default:
          return '未知状态'
      }
    }
  }
}
</script>

<style scoped>
.coach-students {
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

.student-card {
  margin-bottom: 20px;
  height: 280px;
}

.student-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.student-header h3 {
  margin: 0;
  color: #333;
  font-size: 16px;
}

.student-info {
  margin-bottom: 15px;
}

.student-info p {
  margin: 8px 0;
  color: #666;
  font-size: 14px;
  display: flex;
  align-items: center;
}

.student-info i {
  margin-right: 8px;
  color: #409eff;
  width: 16px;
}

.student-actions {
  display: flex;
  gap: 10px;
  justify-content: center;
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

.student-feedback {
  margin-top: 15px;
  padding: 10px;
  background: #f5f7fa;
  border-radius: 4px;
}

.student-feedback p {
  margin: 5px 0;
  color: #333;
}

.student-rating {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.status-badge {
  flex-shrink: 0;
}

.evaluation-form {
  margin-top: 15px;
}

/* New styles for evaluation record dialog */
.evaluation-record-card {
  margin-bottom: 20px;
  padding: 15px;
}

.evaluation-record-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
}

.evaluation-record-header h4 {
  margin: 0;
  color: #333;
  font-size: 16px;
}

.course-cost {
  color: #409eff;
  font-weight: bold;
  font-size: 16px;
}

.evaluation-content {
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
}

.student-eval-section, .coach-eval-section {
  margin-bottom: 15px;
}

.student-eval-section h5, .coach-eval-section h5 {
  margin-bottom: 8px;
  color: #555;
  font-size: 15px;
}

.eval-text {
  margin: 5px 0;
  color: #333;
  font-size: 14px;
  line-height: 1.6;
}

.rating-section {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.no-eval {
  color: #999;
  font-style: italic;
}

.eval-time {
  text-align: right;
  color: #666;
  font-size: 13px;
  margin-top: 10px;
}
</style>