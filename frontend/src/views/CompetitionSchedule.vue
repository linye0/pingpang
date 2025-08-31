<template>
  <div class="competition-schedule">
    <el-container>
      <!-- 顶部导航 -->
      <el-header class="header">
        <div class="header-content">
          <h1 class="page-title">比赛赛程</h1>
          <div class="header-actions">
            <el-button @click="refreshSchedule">刷新赛程</el-button>
            <el-button @click="$router.go(-1)">返回</el-button>
          </div>
        </div>
      </el-header>
      
      <!-- 主要内容 -->
      <el-main class="main-content">
        <div v-if="loading" class="loading-container">
                        <i class="el-icon-loading" style="font-size: 30px;"></i>
          <p>正在加载赛程...</p>
        </div>
        
        <div v-else-if="schedule" class="schedule-container">
          <!-- 比赛基本信息 -->
          <el-card class="competition-info" shadow="hover">
            <h2>{{ schedule.competition.name }}</h2>
            <p><strong>比赛时间：</strong>{{ formatDate(schedule.competition.competitionDate) }}</p>
            <p><strong>最大参赛人数：</strong>{{ schedule.competition.maxParticipants }}人</p>
            <p><strong>报名费用：</strong>¥{{ schedule.competition.registrationFee }}</p>
          </el-card>
          
          <!-- 各组别赛程 -->
          <div v-for="(groupSchedule, groupName) in schedule.schedule" 
               :key="groupName" 
               class="group-schedule">
            <el-card shadow="hover">
              <div slot="header" class="group-header">
                <h3>{{ getGroupDisplayName(groupName) }}组</h3>
                <el-tag :type="getGroupTagType(groupSchedule.type)">{{ groupSchedule.type }}</el-tag>
              </div>
              
              <!-- 全循环赛程 -->
              <div v-if="groupSchedule.type === '全循环'" class="round-robin">
                <div class="schedule-stats">
                  <p><strong>参赛人数：</strong>{{ groupSchedule.playerCount }}人</p>
                  <p><strong>总轮次：</strong>{{ groupSchedule.totalRounds }}轮</p>
                </div>
                
                <div v-if="groupSchedule.message" class="warning-message">
                  <el-alert :title="groupSchedule.message" type="warning" :closable="false" />
                </div>
                
                <div v-else class="rounds-container">
                  <el-collapse>
                    <el-collapse-item 
                      v-for="round in groupSchedule.rounds" 
                      :key="round.round"
                      :title="`第${round.round}轮 (${round.matches.length}场比赛)`"
                    >
                      <div class="matches-grid">
                        <el-card v-for="(match, index) in round.matches" 
                                :key="index" 
                                class="match-card"
                                :class="{'bye-match': match.type === '轮空'}">
                          <div v-if="match.type === '轮空'" class="bye-match-content">
                            <i class="el-icon-user"></i>
                            <p><strong>{{ match.player }}</strong></p>
                            <p class="bye-text">本轮轮空</p>
                          </div>
                          <div v-else class="vs-match-content">
                            <div class="player player1">
                              <i class="el-icon-user"></i>
                              <span>{{ match.player1 }}</span>
                            </div>
                            <div class="vs-divider">
                              <span>VS</span>
                            </div>
                            <div class="player player2">
                              <i class="el-icon-user"></i>
                              <span>{{ match.player2 }}</span>
                            </div>
                            <div class="table-info">
                              <i class="el-icon-office-building"></i>
                              <span>{{ match.table }}</span>
                            </div>
                          </div>
                        </el-card>
                      </div>
                    </el-collapse-item>
                  </el-collapse>
                </div>
              </div>
              
              <!-- 分组+淘汰赛程 -->
              <div v-else-if="groupSchedule.type === '分组+淘汰'" class="group-knockout">
                <div class="schedule-stats">
                  <p><strong>参赛人数：</strong>{{ groupSchedule.playerCount }}人</p>
                </div>
                
                <!-- 小组赛阶段 -->
                <div class="group-stage">
                  <h4><i class="el-icon-s-grid"></i> 小组赛阶段</h4>
                  <el-row :gutter="20">
                    <el-col :span="12" v-for="(group, index) in groupSchedule.groupStage" :key="index">
                      <el-card class="small-group-card">
                        <div slot="header">
                          <span>{{ group.groupName }}</span>
                          <small>（前{{ group.qualifyCount }}名晋级）</small>
                        </div>
                        
                        <div class="small-group-stats">
                          <p><strong>人数：</strong>{{ group.playerCount }}人</p>
                          <p><strong>轮次：</strong>{{ group.totalRounds }}轮</p>
                        </div>
                        
                        <el-button type="text" @click="showGroupDetails(group)">
                          查看详细赛程
                        </el-button>
                      </el-card>
                    </el-col>
                  </el-row>
                </div>
                
                <!-- 淘汰赛阶段 -->
                <div class="knockout-stage">
                  <h4><i class="el-icon-trophy"></i> 淘汰赛阶段</h4>
                  <el-card class="knockout-info">
                    <p>{{ groupSchedule.knockoutStage.description }}</p>
                    <p><strong>赛制：</strong>{{ groupSchedule.knockoutStage.format }}</p>
                  </el-card>
                </div>
              </div>
            </el-card>
          </div>
        </div>
        
        <div v-else class="error-container">
          <el-empty description="暂无赛程数据" />
        </div>
      </el-main>
    </el-container>
    
    <!-- 小组详情对话框 -->
    <el-dialog title="小组赛程详情" :visible.sync="groupDetailVisible" width="80%">
      <div v-if="selectedGroup" class="group-detail">
        <h3>{{ selectedGroup.groupName }}</h3>
        <div class="rounds-container">
          <el-collapse>
            <el-collapse-item 
              v-for="round in selectedGroup.rounds" 
              :key="round.round"
              :title="`第${round.round}轮`"
            >
              <div class="matches-grid">
                <el-card v-for="(match, index) in round.matches" 
                        :key="index" 
                        class="match-card"
                        :class="{'bye-match': match.type === '轮空'}">
                  <div v-if="match.type === '轮空'" class="bye-match-content">
                    <p><strong>{{ match.player }}</strong> 轮空</p>
                  </div>
                  <div v-else class="vs-match-content">
                    <div class="match-players">
                      <span>{{ match.player1 }}</span>
                      <span class="vs">VS</span>
                      <span>{{ match.player2 }}</span>
                    </div>
                    <div class="table-info">球台: {{ match.table }}</div>
                  </div>
                </el-card>
              </div>
            </el-collapse-item>
          </el-collapse>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { formatDate } from '@/utils/dateFormatter'

export default {
  name: 'CompetitionSchedule',
  
  data() {
    return {
      loading: false,
      schedule: null,
      groupDetailVisible: false,
      selectedGroup: null
    }
  },
  
  created() {
    this.loadSchedule()
  },
  
  methods: {
    async loadSchedule() {
      this.loading = true
      try {
        const competitionId = this.$route.params.competitionId
        if (!competitionId) {
          this.$message.error('缺少比赛ID参数')
          return
        }
        
        // 使用新的API端点
        const response = await this.$http.get(`/api/competition/${competitionId}/schedule`)
        this.schedule = response.data.data
        
        console.log('比赛赛程数据:', this.schedule)
        
        // 处理新的数据结构
        if (this.schedule && this.schedule.schedule) {
          this.processScheduleData()
        }
      } catch (error) {
        console.error('加载比赛赛程失败:', error)
        this.$message.error(error.response?.data?.message || '加载比赛赛程失败')
      } finally {
        this.loading = false
      }
    },
    
    // 处理新的赛程数据结构
    processScheduleData() {
      // 将新的数据结构转换为前端显示格式
      for (const [groupKey, groupData] of Object.entries(this.schedule.schedule)) {
        if (groupData.rounds) {
          // 将轮次数据从对象转换为数组格式
          const roundsArray = []
          for (const [roundNumber, matches] of Object.entries(groupData.rounds)) {
            const roundMatches = matches.map(match => this.formatMatchForDisplay(match))
            roundsArray.push({
              round: parseInt(roundNumber),
              matches: roundMatches
            })
          }
          
          // 按轮次排序
          roundsArray.sort((a, b) => a.round - b.round)
          
          // 根据比赛数量判断类型
          const totalMatches = groupData.totalMatches || 0
          const playerCount = this.estimatePlayerCount(totalMatches)
          
          // 更新组数据结构
          groupData.type = playerCount <= 6 ? '全循环' : '分组+淘汰'
          groupData.playerCount = playerCount
          groupData.totalRounds = roundsArray.length
          groupData.rounds = roundsArray
        }
      }
    },
    
    // 格式化比赛数据用于显示
    formatMatchForDisplay(match) {
      if (match.isBye || match.player2 === null) {
        return {
          type: '轮空',
          player: match.player1 ? match.player1.realName : '未知选手',
          table: '无需球台'
        }
      } else {
        return {
          type: '对战',
          player1: match.player1 ? match.player1.realName : '未知选手',
          player2: match.player2 ? match.player2.realName : '未知选手',
          table: match.tableNumber || 'T001',
          matchType: match.matchType || '全循环',
          groupName: match.groupName || ''
        }
      }
    },
    
    // 根据比赛场次估算参赛人数
    estimatePlayerCount(totalMatches) {
      // 这是一个简单的估算，实际应该从后端获取准确数据
      if (totalMatches <= 15) return totalMatches <= 3 ? 3 : totalMatches <= 6 ? 4 : totalMatches <= 10 ? 5 : 6
      return Math.ceil(Math.sqrt(totalMatches * 2))
    },
    
    refreshSchedule() {
      this.loadSchedule()
    },
    
    showGroupDetails(group) {
      this.selectedGroup = group
      this.groupDetailVisible = true
    },
    
    getGroupDisplayName(groupName) {
      const groupMap = {
        'GROUP_A': 'A',
        'GROUP_B': 'B', 
        'GROUP_C': 'C'
      }
      return groupMap[groupName] || groupName
    },
    
    getGroupTagType(type) {
      return type === '全循环' ? 'primary' : 'success'
    },
    
    formatDate(dateTime) {
      return formatDate(dateTime)
    }
  },
  
  computed: {
    userRole() {
      return this.$store.getters.userRole
    }
  }
}
</script>

<style scoped>
.competition-schedule {
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

.header-actions {
  display: flex;
  gap: 10px;
}

.main-content {
  padding: 20px;
}

.loading-container {
  text-align: center;
  padding: 100px;
}

.competition-info {
  margin-bottom: 20px;
}

.competition-info h2 {
  margin: 0 0 15px 0;
  color: #333;
}

.group-schedule {
  margin-bottom: 30px;
}

.group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.group-header h3 {
  margin: 0;
  color: #333;
}

.schedule-stats {
  margin-bottom: 20px;
}

.schedule-stats p {
  margin: 5px 0;
  color: #666;
}

.warning-message {
  margin: 20px 0;
}

.rounds-container {
  margin-top: 20px;
}

.matches-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 15px;
  margin-top: 15px;
}

.match-card {
  min-height: 120px;
  transition: transform 0.2s;
}

.match-card:hover {
  transform: translateY(-2px);
}

.bye-match {
  background: #f9f9f9;
}

.bye-match-content {
  text-align: center;
  padding: 20px;
}

.bye-match-content i {
  font-size: 24px;
  color: #909399;
  margin-bottom: 10px;
}

.bye-text {
  color: #909399;
  font-style: italic;
}

.vs-match-content {
  padding: 15px;
}

.player {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
  font-weight: 500;
}

.player i {
  margin-right: 8px;
  color: #409eff;
}

.vs-divider {
  text-align: center;
  margin: 10px 0;
  font-weight: bold;
  color: #e6a23c;
}

.table-info {
  text-align: center;
  margin-top: 10px;
  padding: 5px;
  background: #f0f9ff;
  border-radius: 4px;
  color: #409eff;
  font-size: 14px;
}

.table-info i {
  margin-right: 5px;
}

.group-stage, .knockout-stage {
  margin: 30px 0;
}

.group-stage h4, .knockout-stage h4 {
  color: #333;
  margin-bottom: 15px;
}

.small-group-card {
  margin-bottom: 15px;
  height: 200px;
}

.small-group-stats {
  margin: 15px 0;
}

.knockout-info {
  padding: 20px;
  background: #fff7e6;
}

.error-container {
  text-align: center;
  padding: 100px;
}

.group-detail .matches-grid {
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
}

.group-detail .match-players {
  text-align: center;
  font-weight: 500;
}

.group-detail .vs {
  margin: 0 10px;
  color: #e6a23c;
}

.group-detail .table-info {
  text-align: center;
  margin-top: 10px;
  font-size: 12px;
}
</style> 