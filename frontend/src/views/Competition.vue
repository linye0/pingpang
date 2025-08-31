<template>
  <div class="competition">
    <el-container>
      <!-- 顶部导航 -->
      <el-header class="header">
        <div class="header-content">
          <h1 class="page-title">比赛管理</h1>
          <el-button @click="$router.push('/dashboard')">返回首页</el-button>
        </div>
      </el-header>
      
      <!-- 主要内容 -->
      <el-main class="main-content">
        <el-tabs v-model="activeTab" type="card">
          <!-- 可报名比赛 -->
          <el-tab-pane label="可报名比赛" name="available">
            <el-card>
              <div slot="header">
                <span>本校区可报名比赛</span>
                <el-button 
                  type="text" 
                  @click="loadAvailableCompetitions"
                  style="float: right; padding: 3px 0"
                >
                  刷新
                </el-button>
              </div>
              
              <div v-if="availableCompetitions.length === 0" class="empty-container">
                <el-empty description="暂无可报名比赛" />
              </div>
              
              <div v-else>
                <el-row :gutter="20">
                  <el-col :span="8" v-for="competition in availableCompetitions" :key="competition.id">
                    <el-card class="competition-card">
                      <div class="competition-info">
                        <h3>{{ competition.name }}</h3>
                        <p class="competition-date">
                          <i class="el-icon-date"></i>
                          {{ formatCompetitionDate(competition.competitionDate) }}
                        </p>
                        <p class="competition-fee">
                          <i class="el-icon-money"></i>
                          报名费: ¥{{ competition.registrationFee }}
                        </p>
                        <p class="competition-desc">{{ competition.description }}</p>
                      </div>
                      
                      <div class="competition-actions">
                        <el-button 
                          type="primary" 
                          @click.stop="handleRegisterClick(competition)"
                          :disabled="isRegistered(competition.id)"
                        >
                          {{ isRegistered(competition.id) ? '已报名' : '立即报名' }}
                        </el-button>
                        <el-button 
                          type="info" 
                          @click.stop="handleDetailClick(competition)"
                        >
                          查看详情
                        </el-button>
                      </div>
                    </el-card>
                  </el-col>
                </el-row>
              </div>
            </el-card>
          </el-tab-pane>
          
          <!-- 我的比赛 -->
          <el-tab-pane label="我的比赛" name="my">
            <el-card>
              <div slot="header">
                <span>我的比赛报名</span>
              </div>
              
              <el-table :data="myCompetitions" stripe>
                <el-table-column prop="competition.name" label="比赛名称" width="200" />
                <el-table-column prop="competition.competitionDate" label="比赛日期" width="120">
                  <template slot-scope="scope">
                    {{ formatCompetitionDate(scope.row.competition.competitionDate) }}
                  </template>
                </el-table-column>
                <el-table-column prop="competitionGroup" label="参赛组别" width="100">
                  <template slot-scope="scope">
                    <el-tag :type="getGroupTagType(scope.row.competitionGroup)">
                      {{ getGroupText(scope.row.competitionGroup) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="competition.registrationFee" label="报名费" width="100">
                  <template slot-scope="scope">
                    ¥{{ scope.row.competition.registrationFee }}
                  </template>
                </el-table-column>
                <el-table-column prop="createdAt" label="报名时间" width="150">
                  <template slot-scope="scope">
                    {{ formatDateTime(scope.row.createdAt) }}
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="120">
                  <template slot-scope="scope">
                    <el-button 
                      type="primary" 
                      size="mini"
                      @click="viewSchedule(scope.row)"
                    >
                      查看赛程
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-tab-pane>
        </el-tabs>
      </el-main>
    </el-container>
    
    <!-- 报名弹窗 -->
    <el-dialog
      title="比赛报名"
      :visible.sync="registerDialogVisible"
      width="500px"
    >
      <div v-if="selectedCompetition">
        <h3>{{ selectedCompetition.name }}</h3>
        <el-form :model="registerForm" ref="registerForm">
          <el-form-item label="选择组别" prop="group">
            <el-select v-model="registerForm.group" placeholder="请选择参赛组别" style="width: 100%">
              <el-option label="甲组" value="GROUP_A" />
              <el-option label="乙组" value="GROUP_B" />
              <el-option label="丙组" value="GROUP_C" />
            </el-select>
          </el-form-item>
          
          <div class="fee-info">
            <p>报名费: ¥{{ selectedCompetition.registrationFee }}</p>
            <p>当前余额: ¥{{ accountBalance }}</p>
            <p v-if="accountBalance < selectedCompetition.registrationFee" class="insufficient-balance">
              余额不足，请先充值
            </p>
          </div>
        </el-form>
      </div>
      
      <div slot="footer" class="dialog-footer">
        <el-button @click="registerDialogVisible = false">取消</el-button>
        <el-button 
          type="primary" 
          @click="confirmRegister"
          :disabled="!registerForm.group || accountBalance < (selectedCompetition?.registrationFee || 0)"
          :loading="registering"
        >
          确认报名
        </el-button>
      </div>
    </el-dialog>
    
    <!-- 比赛详情弹窗 -->
    <el-dialog
      title="比赛详情"
      :visible.sync="detailDialogVisible"
      width="600px"
    >
      <div v-if="selectedCompetition">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="比赛名称">{{ selectedCompetition.name }}</el-descriptions-item>
          <el-descriptions-item label="比赛日期">{{ formatCompetitionDate(selectedCompetition.competitionDate) }}</el-descriptions-item>
          <el-descriptions-item label="报名费">¥{{ selectedCompetition.registrationFee }}</el-descriptions-item>
          <el-descriptions-item label="报名状态">
            <el-tag :type="selectedCompetition.registrationOpen ? 'success' : 'danger'">
              {{ selectedCompetition.registrationOpen ? '开放报名' : '报名截止' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="比赛描述" :span="2">
            {{ selectedCompetition.description }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { formatDate, formatDateTime } from '@/utils/dateFormatter'

export default {
  name: 'Competition',
  
  data() {
    return {
      activeTab: 'available',
      availableCompetitions: [],
      myCompetitions: [],
      selectedCompetition: null,
      registerDialogVisible: false,
      detailDialogVisible: false,
      registerForm: {
        group: ''
      },
      registering: false,
      accountBalance: 0
    }
  },
  
  created() {
    this.loadData()
  },
  
  methods: {
    async loadData() {
      await this.loadAvailableCompetitions()
      await this.loadMyCompetitions()
      await this.loadAccountBalance()
    },
    
    async loadAvailableCompetitions() {
      try {
        console.log('=== 加载可报名比赛 ===')
        const response = await this.$http.get('/api/competition/available')
        console.log('可报名比赛响应:', response.data)
        this.availableCompetitions = response.data.data || []
        console.log('设置可报名比赛数量:', this.availableCompetitions.length)
      } catch (error) {
        console.error('加载可报名比赛失败:', error)
        console.error('错误详情:', {
          status: error.response?.status,
          data: error.response?.data,
          message: error.message
        })
        this.$message.error('加载可报名比赛失败: ' + (error.response?.data?.message || error.message))
      }
    },
    
    async loadMyCompetitions() {
      try {
        console.log('=== 加载我的比赛开始 ===')
        console.log('发送请求到: /api/competition/my-registrations')
        
        const response = await this.$http.get('/api/competition/my-registrations')
        console.log('我的比赛API响应状态:', response.status)
        console.log('我的比赛API响应headers:', response.headers)
        console.log('我的比赛原始响应:', response)
        console.log('我的比赛响应数据:', response.data)
        
        // 验证返回的数据结构
        const competitions = response.data.data || []
        console.log('解析出的比赛数组:', competitions)
        console.log('比赛数组类型:', typeof competitions)
        console.log('比赛数组长度:', competitions.length)
        
        if (competitions.length > 0) {
          console.log('第一个比赛记录的结构:', competitions[0])
          console.log('第一个比赛记录的所有属性:', Object.keys(competitions[0]))
        }
        
        // 过滤掉没有 competition 属性的数据
        const validCompetitions = competitions.filter((reg, index) => {
          console.log(`检查第${index}个比赛记录:`, reg)
          const isValid = reg && reg.competition && reg.competition.id
          if (!isValid) {
            console.warn(`第${index}个比赛注册数据无效:`, {
              reg,
              hasReg: !!reg,
              hasCompetition: !!(reg && reg.competition),
              hasCompetitionId: !!(reg && reg.competition && reg.competition.id)
            })
          } else {
            console.log(`第${index}个比赛记录有效:`, {
              registrationId: reg.id,
              competitionId: reg.competition.id,
              competitionName: reg.competition.name
            })
          }
          return isValid
        })
        
        this.myCompetitions = validCompetitions
        console.log('最终设置的我的比赛数量:', this.myCompetitions.length)
        console.log('最终的比赛数据:', this.myCompetitions)
        
        // 如果没有比赛数据，显示提示
        if (this.myCompetitions.length === 0) {
          console.warn('没有找到任何已报名的比赛')
        }
        
      } catch (error) {
        console.error('=== 加载我的比赛失败 ===')
        console.error('错误对象:', error)
        console.error('错误详情:', {
          message: error.message,
          status: error.response?.status,
          statusText: error.response?.statusText,
          data: error.response?.data,
          config: error.config
        })
        
        this.$message.error('加载我的比赛失败: ' + (error.response?.data?.message || error.message))
        
        // 确保在错误时也有一个空数组
        this.myCompetitions = []
      }
    },
    
    async loadAccountBalance() {
      try {
        const response = await this.$http.get('/api/student/balance')
        this.accountBalance = response.data.data
      } catch (error) {
        console.error('加载账户余额失败')
      }
    },
    
    isRegistered(competitionId) {
      return this.myCompetitions.some(reg => {
        // 安全检查：确保 reg 和 reg.competition 都存在
        return reg && reg.competition && reg.competition.id === competitionId
      })
    },
    
    showRegisterDialog(competition) {
      console.log('=== 显示报名弹窗 ===')
      console.log('选中的比赛:', competition)
      
      if (!competition) {
        console.error('没有选中的比赛')
        this.$message.error('比赛信息错误')
        return
      }
      
      this.selectedCompetition = competition
      this.registerForm.group = ''
      this.registerDialogVisible = true
      
      console.log('报名弹窗状态:', this.registerDialogVisible)
    },
    
    viewCompetitionDetail(competition) {
      console.log('=== 显示比赛详情 ===')
      console.log('选中的比赛:', competition)
      
      if (!competition) {
        console.error('没有选中的比赛')
        this.$message.error('比赛信息错误')
        return
      }
      
      this.selectedCompetition = competition
      this.detailDialogVisible = true
      
      console.log('详情弹窗状态:', this.detailDialogVisible)
    },
    
    async confirmRegister() {
      try {
        this.registering = true
        
        const response = await this.$http.post(
          `/api/competition/${this.selectedCompetition.id}/register`,
          null,
          {
            params: {
              group: this.registerForm.group
            }
          }
        )
        
        this.$message.success(response.data.message)
        this.registerDialogVisible = false
        this.loadData() // 刷新数据
      } catch (error) {
        this.$message.error(error.response?.data?.message || '报名失败')
      } finally {
        this.registering = false
      }
    },
    
    async viewSchedule(registration) {
      try {
        // 跳转到比赛赛程页面，使用正确的路由路径
        this.$router.push({
          path: `/competition/${registration.competition.id}/schedule`
        })
      } catch (error) {
        console.error('跳转到赛程页面失败:', error)
        this.$message.error('跳转失败')
      }
    },
    
    getGroupText(group) {
      const groupMap = {
        'GROUP_A': '甲组',
        'GROUP_B': '乙组', 
        'GROUP_C': '丙组'
      }
      return groupMap[group] || '未知'
    },
    
    getGroupTagType(group) {
      const typeMap = {
        'GROUP_A': 'danger',
        'GROUP_B': 'warning',
        'GROUP_C': 'success'
      }
      return typeMap[group] || 'info'
    },
    
    formatCompetitionDate(dateTime) {
      return formatDate(dateTime, 'YYYY-MM-DD')
    },

    formatDateTime(dateTime) {
      // 直接调用formatDate，避免递归
      return formatDate(dateTime, 'YYYY-MM-DD HH:mm')
    },

    handleRegisterClick(competition) {
      console.log('=== 点击报名按钮 ===')
      console.log('选中的比赛:', competition)
      this.showRegisterDialog(competition)
    },

    handleDetailClick(competition) {
      console.log('=== 点击详情按钮 ===')
      console.log('选中的比赛:', competition)
      this.viewCompetitionDetail(competition)
    }
  }
}
</script>

<style scoped>
.competition {
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

.competition-card {
  margin-bottom: 20px;
  cursor: default;
}

.competition-info h3 {
  margin: 0 0 15px 0;
  color: #333;
}

.competition-info p {
  margin: 8px 0;
  color: #666;
  display: flex;
  align-items: center;
  gap: 5px;
}

.competition-date {
  color: #409EFF !important;
}

.competition-fee {
  color: #67C23A !important;
  font-weight: 600;
}

.competition-actions {
  margin-top: 15px;
  text-align: center;
}

.fee-info {
  background: #f8f9fa;
  padding: 15px;
  border-radius: 4px;
  margin: 15px 0;
}

.fee-info p {
  margin: 5px 0;
}

.insufficient-balance {
  color: #F56C6C !important;
  font-weight: 600;
}

.dialog-footer {
  text-align: right;
}
</style>