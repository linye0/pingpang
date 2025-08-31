<template>
  <div class="coach-working-time">
    <el-container>
      <!-- 顶部导航 -->
      <el-header class="header">
        <div class="header-content">
          <h1 class="page-title">工作时间管理</h1>
          <div class="header-actions">
            <el-button @click="loadWorkingTime">刷新</el-button>
            <el-button type="primary" @click="showBatchSetDialog">批量设置</el-button>
            <el-button @click="$router.push('/dashboard')">返回首页</el-button>
          </div>
        </div>
      </el-header>
      
      <!-- 主要内容 -->
      <el-main class="main-content">
        
        <!-- 工作时间概览 -->
        <el-card class="overview-card">
          <div slot="header">
            <span>本周工作时间安排</span>
            <el-button type="text" @click="loadWorkingTime" style="float: right;">
              <i class="el-icon-refresh"></i> 刷新
            </el-button>
          </div>
          
          <div v-if="loading" class="loading-container">
            <el-loading text="加载工作时间中..."></el-loading>
          </div>
          
          <div v-else class="working-time-grid">
            <div 
              v-for="day in weekDays" 
              :key="day.value"
              class="day-column"
            >
              <div class="day-header">
                <h3>{{ day.label }}</h3>
                <el-button 
                  size="mini" 
                  type="primary" 
                  @click="showAddTimeDialog(day.value)"
                >
                  添加时间段
                </el-button>
              </div>
              
              <div class="time-slots">
                <div 
                  v-for="slot in getTimeSlotsForDay(day.value)" 
                  :key="slot.id"
                  class="time-slot"
                  :class="{ 'unavailable': !slot.isAvailable }"
                >
                  <div class="time-range">
                    {{ formatTime(slot.startTime) }} - {{ formatTime(slot.endTime) }}
                  </div>
                  <div class="slot-actions">
                    <el-switch
                      v-model="slot.isAvailable"
                      @change="updateAvailability(slot)"
                      active-text="可预约"
                      inactive-text="不可用"
                      size="mini"
                    />
                    <el-button 
                      size="mini" 
                      type="danger" 
                      @click="deleteTimeSlot(slot)"
                      style="margin-left: 10px;"
                    >
                      删除
                    </el-button>
                  </div>
                  <div v-if="slot.remarks" class="slot-remarks">
                    {{ slot.remarks }}
                  </div>
                </div>
                
                <div v-if="getTimeSlotsForDay(day.value).length === 0" class="no-slots">
                  暂无工作时间安排
                </div>
              </div>
            </div>
          </div>
        </el-card>
        
      </el-main>
    </el-container>
    
    <!-- 添加时间段对话框 -->
    <el-dialog
      title="添加工作时间段"
      :visible.sync="addTimeDialogVisible"
      width="500px"
    >
      <el-form :model="newTimeSlot" :rules="timeSlotRules" ref="timeSlotForm" label-width="80px">
        <el-form-item label="星期" prop="dayOfWeek">
          <el-select v-model="newTimeSlot.dayOfWeek" placeholder="选择星期" style="width: 100%;">
            <el-option 
              v-for="day in weekDays" 
              :key="day.value" 
              :label="day.label" 
              :value="day.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-time-picker
            v-model="newTimeSlot.startTime"
            format="HH:mm"
            placeholder="选择开始时间"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-time-picker
            v-model="newTimeSlot.endTime"
            format="HH:mm"
            placeholder="选择结束时间"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch
            v-model="newTimeSlot.isAvailable"
            active-text="可预约"
            inactive-text="不可用"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="newTimeSlot.remarks"
            type="textarea"
            rows="2"
            placeholder="可选，如：临时调整等"
          />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="addTimeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="addTimeSlot">确定</el-button>
      </span>
    </el-dialog>
    
    <!-- 批量设置对话框 -->
    <el-dialog
      title="批量设置工作时间"
      :visible.sync="batchSetDialogVisible"
      width="800px"
    >
      <div class="batch-set-content">
        <el-alert
          title="批量设置说明"
          description="选择标准工作时间模板，系统将自动为您设置整周的工作时间安排。注意：此操作将覆盖现有设置。"
          type="info"
          :closable="false"
          style="margin-bottom: 20px;"
        />
        
        <el-form label-width="120px">
          <el-form-item label="工作时间模板">
            <el-radio-group v-model="selectedTemplate">
              <el-radio label="standard">标准工作时间（9:00-21:00，中午休息）</el-radio>
              <el-radio label="flexible">灵活工作时间（10:00-20:00）</el-radio>
              <el-radio label="weekend">周末加班（9:00-18:00）</el-radio>
              <el-radio label="custom">自定义设置</el-radio>
            </el-radio-group>
          </el-form-item>
          
          <div v-if="selectedTemplate === 'custom'" class="custom-template">
            <el-form-item label="工作日">
              <el-checkbox-group v-model="customTemplate.workDays">
                <el-checkbox 
                  v-for="day in weekDays" 
                  :key="day.value" 
                  :label="day.value"
                >
                  {{ day.label }}
                </el-checkbox>
              </el-checkbox-group>
            </el-form-item>
            <el-form-item label="开始时间">
              <el-time-picker
                v-model="customTemplate.startTime"
                format="HH:mm"
                placeholder="选择开始时间"
              />
            </el-form-item>
            <el-form-item label="结束时间">
              <el-time-picker
                v-model="customTemplate.endTime"
                format="HH:mm"
                placeholder="选择结束时间"
              />
            </el-form-item>
            <el-form-item label="休息时间">
              <el-time-picker
                v-model="customTemplate.breakStart"
                format="HH:mm"
                placeholder="休息开始时间（可选）"
              />
              <el-time-picker
                v-model="customTemplate.breakEnd"
                format="HH:mm"
                placeholder="休息结束时间（可选）"
                style="margin-left: 10px;"
              />
            </el-form-item>
          </div>
        </el-form>
      </div>
      <span slot="footer">
        <el-button @click="batchSetDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="applyBatchSet">应用设置</el-button>
      </span>
    </el-dialog>
    
  </div>
</template>

<script>
export default {
  name: 'CoachWorkingTime',
  
  data() {
    return {
      loading: false,
      workingTimeData: [],
      addTimeDialogVisible: false,
      batchSetDialogVisible: false,
      selectedTemplate: 'standard',
      
      weekDays: [
        { value: 1, label: '周一' },
        { value: 2, label: '周二' },
        { value: 3, label: '周三' },
        { value: 4, label: '周四' },
        { value: 5, label: '周五' },
        { value: 6, label: '周六' },
        { value: 7, label: '周日' }
      ],
      
      newTimeSlot: {
        dayOfWeek: 1,
        startTime: null,
        endTime: null,
        isAvailable: true,
        remarks: ''
      },
      
      customTemplate: {
        workDays: [1, 2, 3, 4, 5],
        startTime: null,
        endTime: null,
        breakStart: null,
        breakEnd: null
      },
      
      timeSlotRules: {
        dayOfWeek: [
          { required: true, message: '请选择星期', trigger: 'change' }
        ],
        startTime: [
          { required: true, message: '请选择开始时间', trigger: 'change' }
        ],
        endTime: [
          { required: true, message: '请选择结束时间', trigger: 'change' }
        ]
      }
    }
  },
  
  mounted() {
    this.loadWorkingTime()
  },
  
  methods: {
    async loadWorkingTime() {
      this.loading = true
      try {
        const response = await this.$http.get('/api/coach/working-time')
        this.workingTimeData = response.data.data.weeklySchedule || []
        console.log('工作时间数据:', this.workingTimeData)
      } catch (error) {
        console.error('加载工作时间失败:', error)
        this.$message.error('加载工作时间失败')
      } finally {
        this.loading = false
      }
    },
    
    getTimeSlotsForDay(dayOfWeek) {
      return this.workingTimeData.filter(slot => slot.dayOfWeek === dayOfWeek)
        .sort((a, b) => a.startTime.localeCompare(b.startTime))
    },
    
    showAddTimeDialog(dayOfWeek = null) {
      this.newTimeSlot = {
        dayOfWeek: dayOfWeek || 1,
        startTime: null,
        endTime: null,
        isAvailable: true,
        remarks: ''
      }
      this.addTimeDialogVisible = true
    },
    
    async addTimeSlot() {
      try {
        await this.$refs.timeSlotForm.validate()
        
        // 转换时间格式
        const timeSlotData = {
          dayOfWeek: this.newTimeSlot.dayOfWeek,
          startTime: this.formatTimeForAPI(this.newTimeSlot.startTime),
          endTime: this.formatTimeForAPI(this.newTimeSlot.endTime),
          isAvailable: this.newTimeSlot.isAvailable,
          remarks: this.newTimeSlot.remarks
        }
        
        const response = await this.$http.post('/api/coach/working-time', timeSlotData)
        this.$message.success(response.data.message)
        this.addTimeDialogVisible = false
        this.loadWorkingTime()
      } catch (error) {
        console.error('添加时间段失败:', error)
        this.$message.error(error.response?.data?.message || '添加时间段失败')
      }
    },
    
    async updateAvailability(slot) {
      try {
        const response = await this.$http.put(`/api/coach/working-time/${slot.id}/availability`, null, {
          params: {
            isAvailable: slot.isAvailable,
            remarks: slot.remarks
          }
        })
        this.$message.success(response.data.message)
      } catch (error) {
        console.error('更新可用状态失败:', error)
        this.$message.error(error.response?.data?.message || '更新失败')
        // 恢复原状态
        slot.isAvailable = !slot.isAvailable
      }
    },
    
    async deleteTimeSlot(slot) {
      try {
        await this.$confirm('确认删除此时间段吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        const response = await this.$http.delete(`/api/coach/working-time/${slot.id}`)
        this.$message.success(response.data.message)
        this.loadWorkingTime()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除时间段失败:', error)
          this.$message.error(error.response?.data?.message || '删除失败')
        }
      }
    },
    
    showBatchSetDialog() {
      this.selectedTemplate = 'standard'
      this.customTemplate = {
        workDays: [1, 2, 3, 4, 5],
        startTime: null,
        endTime: null,
        breakStart: null,
        breakEnd: null
      }
      this.batchSetDialogVisible = true
    },
    
    async applyBatchSet() {
      try {
        const batchData = this.generateBatchData()
        
        if (batchData.length === 0) {
          this.$message.warning('请设置有效的工作时间')
          return
        }
        
        const response = await this.$http.post('/api/coach/working-time/weekly', batchData)
        this.$message.success(response.data.message)
        this.batchSetDialogVisible = false
        this.loadWorkingTime()
      } catch (error) {
        console.error('批量设置失败:', error)
        this.$message.error(error.response?.data?.message || '批量设置失败')
      }
    },
    
    generateBatchData() {
      let workingTimes = []
      
      if (this.selectedTemplate === 'standard') {
        // 标准工作时间：9:00-12:00, 14:00-21:00
        for (let day = 1; day <= 7; day++) {
          // 上午时间段
          workingTimes.push(...this.generateTimeSlots(day, '09:00', '12:00'))
          // 下午时间段
          workingTimes.push(...this.generateTimeSlots(day, '14:00', '21:00'))
        }
      } else if (this.selectedTemplate === 'flexible') {
        // 灵活工作时间：10:00-20:00
        for (let day = 1; day <= 7; day++) {
          workingTimes.push(...this.generateTimeSlots(day, '10:00', '20:00'))
        }
      } else if (this.selectedTemplate === 'weekend') {
        // 周末加班：只有周六周日 9:00-18:00
        workingTimes.push(...this.generateTimeSlots(6, '09:00', '18:00'))
        workingTimes.push(...this.generateTimeSlots(7, '09:00', '18:00'))
      } else if (this.selectedTemplate === 'custom') {
        // 自定义设置
        const { workDays, startTime, endTime, breakStart, breakEnd } = this.customTemplate
        
        if (!startTime || !endTime) {
          this.$message.warning('请设置开始和结束时间')
          return []
        }
        
        for (let day of workDays) {
          if (breakStart && breakEnd) {
            // 有休息时间，分成两段
            workingTimes.push(...this.generateTimeSlots(day, this.formatTimeForAPI(startTime), this.formatTimeForAPI(breakStart)))
            workingTimes.push(...this.generateTimeSlots(day, this.formatTimeForAPI(breakEnd), this.formatTimeForAPI(endTime)))
          } else {
            // 无休息时间，整段时间
            workingTimes.push(...this.generateTimeSlots(day, this.formatTimeForAPI(startTime), this.formatTimeForAPI(endTime)))
          }
        }
      }
      
      return workingTimes
    },
    
    generateTimeSlots(dayOfWeek, startTime, endTime) {
      const slots = []
      const start = new Date(`2000-01-01 ${startTime}`)
      const end = new Date(`2000-01-01 ${endTime}`)
      
      while (start < end) {
        const slotEnd = new Date(start.getTime() + 60 * 60 * 1000) // 1小时后
        if (slotEnd <= end) {
          slots.push({
            dayOfWeek: dayOfWeek,
            startTime: this.formatTime(start),
            endTime: this.formatTime(slotEnd),
            isAvailable: true,
            remarks: `批量设置 - ${this.selectedTemplate}`
          })
        }
        start.setHours(start.getHours() + 1)
      }
      
      return slots
    },
    
    formatTime(time) {
      if (typeof time === 'string') return time
      if (time instanceof Date) {
        return time.toTimeString().substring(0, 5)
      }
      return time
    },
    
    formatTimeForAPI(time) {
      if (time instanceof Date) {
        return time.toTimeString().substring(0, 8) // HH:mm:ss
      }
      return time
    }
  }
}
</script>

<style scoped>
.coach-working-time {
  min-height: 100vh;
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

.overview-card {
  margin-bottom: 20px;
}

.working-time-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
}

.day-column {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: white;
  overflow: hidden;
}

.day-header {
  background: #f5f7fa;
  padding: 15px;
  border-bottom: 1px solid #ebeef5;
  text-align: center;
}

.day-header h3 {
  margin: 0 0 10px 0;
  color: #303133;
  font-size: 16px;
}

.time-slots {
  padding: 10px;
  min-height: 200px;
}

.time-slot {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 10px;
  margin-bottom: 10px;
  background: #f9f9f9;
  transition: all 0.3s ease;
}

.time-slot.unavailable {
  background: #fdf2f2;
  border-color: #fbc4c4;
}

.time-range {
  font-weight: 500;
  color: #303133;
  margin-bottom: 8px;
}

.slot-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.slot-remarks {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
  font-style: italic;
}

.no-slots {
  text-align: center;
  color: #909399;
  padding: 40px 0;
  font-style: italic;
}

.loading-container {
  text-align: center;
  padding: 40px 0;
}

.batch-set-content {
  max-height: 500px;
  overflow-y: auto;
}

.custom-template {
  background: #f5f7fa;
  padding: 15px;
  border-radius: 4px;
  margin-top: 10px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .working-time-grid {
    grid-template-columns: 1fr;
  }
  
  .slot-actions {
    flex-direction: column;
    align-items: flex-start;
    gap: 5px;
  }
  
  .main-content {
    padding: 10px;
  }
}
</style> 