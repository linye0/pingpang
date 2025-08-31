<template>
  <div class="payment">
    <el-container>
      <!-- 顶部导航 -->
      <el-header class="header">
        <div class="header-content">
          <h1 class="page-title">充值管理</h1>
          <el-button @click="$router.push('/dashboard')">返回首页</el-button>
        </div>
      </el-header>
      
      <!-- 主要内容 -->
      <el-main class="main-content">
        <el-row :gutter="20">
          <!-- 左侧：充值表单 -->
          <el-col :span="12">
            <el-card class="recharge-card">
              <div slot="header">
                <span>账户充值</span>
              </div>
              
              <div class="balance-info">
                <div class="current-balance">
                  <span class="label">当前余额：</span>
                  <span class="amount">¥{{ accountBalance }}</span>
                </div>
              </div>
              
              <el-form 
                :model="rechargeForm" 
                :rules="rechargeRules" 
                ref="rechargeForm"
                label-width="120px"
              >
                <el-form-item label="充值金额" prop="amount">
                  <el-input 
                    v-model="rechargeForm.amount"
                    placeholder="请输入充值金额"
                    type="number"
                    step="0.01"
                  >
                    <template slot="append">元</template>
                  </el-input>
                </el-form-item>
                
                <el-form-item label="支付方式" prop="paymentMethod">
                  <el-radio-group v-model="rechargeForm.paymentMethod">
                    <el-radio label="WECHAT">
                      <i class="payment-icon wechat-icon"></i>
                      微信支付
                    </el-radio>
                    <el-radio label="ALIPAY">
                      <i class="payment-icon alipay-icon"></i>
                      支付宝
                    </el-radio>
                    <el-radio label="OFFLINE">
                      <i class="payment-icon offline-icon"></i>
                      线下支付
                    </el-radio>
                  </el-radio-group>
                </el-form-item>
                
                <el-form-item label="备注">
                  <el-input 
                    v-model="rechargeForm.description"
                    placeholder="请输入备注信息（可选）"
                  />
                </el-form-item>
                
                <el-form-item>
                  <el-button 
                    type="primary" 
                    @click="processRecharge"
                    :loading="processing"
                    style="width: 100%"
                  >
                    立即充值
                  </el-button>
                </el-form-item>
              </el-form>
              
              <!-- 快速金额选择 -->
              <div class="quick-amounts">
                <span class="quick-label">快速选择：</span>
                <el-button 
                  v-for="amount in quickAmounts"
                  :key="amount"
                  size="small"
                  @click="selectQuickAmount(amount)"
                >
                  ¥{{ amount }}
                </el-button>
              </div>
            </el-card>
          </el-col>
          
          <!-- 右侧：支付记录 -->
          <el-col :span="12">
            <el-card class="records-card">
              <div slot="header">
                <span>充值记录</span>
                <el-button 
                  type="text" 
                  @click="loadPaymentRecords"
                  style="float: right; padding: 3px 0"
                >
                  刷新
                </el-button>
              </div>
              
              <div v-if="paymentRecords.length === 0" class="empty-container">
                <el-empty description="暂无充值记录" />
              </div>
              
              <div v-else class="records-list">
                <div 
                  v-for="record in paymentRecords" 
                  :key="record.id"
                  class="record-item"
                >
                  <div class="record-header">
                    <span class="amount">+¥{{ record.amount }}</span>
                    <span class="date">{{ formatDate(record.createdAt) }}</span>
                  </div>
                  
                  <div class="record-details">
                    <div class="detail-row">
                      <span class="label">支付方式：</span>
                      <span>{{ getPaymentMethodText(record.paymentMethod) }}</span>
                    </div>
                    <div class="detail-row">
                      <span class="label">交易号：</span>
                      <span class="transaction-no">{{ record.transactionNo }}</span>
                    </div>
                    <div class="detail-row" v-if="record.description">
                      <span class="label">备注：</span>
                      <span>{{ record.description }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-main>
    </el-container>
    
    <!-- 支付二维码弹窗 -->
    <el-dialog
      title="扫码支付"
      :visible.sync="qrDialogVisible"
      width="400px"
      center
    >
      <div class="qr-payment">
        <div class="payment-info">
          <h3>{{ getPaymentMethodText(currentPaymentMethod) }}</h3>
          <p class="amount">¥{{ rechargeForm.amount }}</p>
        </div>
        
        <div class="qr-code">
          <img :src="qrCodeUrl" alt="支付二维码" />
        </div>
        
        <p class="qr-tip">请使用相应的APP扫描二维码完成支付</p>
        
        <div class="payment-status">
          <el-button @click="qrDialogVisible = false">取消支付</el-button>
          <el-button type="primary" @click="confirmPayment" :loading="confirming">
            已完成支付
          </el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { formatDate, formatDateTime } from '@/utils/dateFormatter'

export default {
  name: 'Payment',
  
  data() {
    return {
      rechargeForm: {
        amount: '',
        paymentMethod: 'WECHAT',
        description: ''
      },
      rechargeRules: {
        amount: [
          { required: true, message: '请输入充值金额', trigger: 'blur' },
          { pattern: /^\d+(\.\d{1,2})?$/, message: '请输入正确的金额格式', trigger: 'blur' }
        ],
        paymentMethod: [
          { required: true, message: '请选择支付方式', trigger: 'change' }
        ]
      },
      accountBalance: 0,
      paymentRecords: [],
      quickAmounts: [100, 200, 500, 1000, 2000],
      processing: false,
      confirming: false,
      qrDialogVisible: false,
      qrCodeUrl: '',
      currentPaymentMethod: '',
      orderNo: ''
    }
  },
  
  created() {
    this.loadAccountBalance()
    this.loadPaymentRecords()
  },
  
  methods: {
    async loadAccountBalance() {
      try {
        const response = await this.$http.get('/api/payment/balance')
        this.accountBalance = response.data.data
      } catch (error) {
        this.$message.error('加载账户余额失败')
      }
    },
    
    async loadPaymentRecords() {
      try {
        const response = await this.$http.get('/api/payment/records')
        this.paymentRecords = response.data.data
      } catch (error) {
        this.$message.error('加载充值记录失败')
      }
    },
    
    async processRecharge() {
      try {
        await this.$refs.rechargeForm.validate()
        
        if (this.rechargeForm.paymentMethod === 'OFFLINE') {
          // 线下支付直接提交
          await this.submitRecharge()
        } else {
          // 在线支付先生成二维码
          await this.generateQRCode()
        }
      } catch (error) {
        console.error('充值验证失败:', error)
      }
    },
    
    async generateQRCode() {
      try {
        this.processing = true
        
        const response = await this.$http.post(
          `/api/payment/generate-qr/${this.rechargeForm.amount}`,
          null,
          { params: { method: this.rechargeForm.paymentMethod } }
        )
        
        const result = response.data.data
        this.qrCodeUrl = result.qrCodeUrl
        this.orderNo = result.orderNo
        this.currentPaymentMethod = this.rechargeForm.paymentMethod
        this.qrDialogVisible = true
      } catch (error) {
        this.$message.error('生成支付二维码失败')
      } finally {
        this.processing = false
      }
    },
    
    async confirmPayment() {
      try {
        this.confirming = true
        
        // 使用模拟支付API
        const response = await this.$http.post('/api/payment/mock-payment-success', null, {
          params: {
            orderNo: this.orderNo,
            amount: parseFloat(this.rechargeForm.amount),
            paymentMethod: this.rechargeForm.paymentMethod,
            description: this.rechargeForm.description || '在线充值'
          }
        })
        
        this.$message.success(response.data.message || '支付成功')
        
        // 关闭弹窗，重置表单，刷新数据
        this.qrDialogVisible = false
        this.resetForm()
        this.loadAccountBalance()
        this.loadPaymentRecords()
      } catch (error) {
        this.$message.error(error.response?.data?.message || '支付失败')
      } finally {
        this.confirming = false
      }
    },
    
    async submitRecharge() {
      try {
        this.processing = true
        
        const paymentData = {
          amount: parseFloat(this.rechargeForm.amount),
          paymentMethod: this.rechargeForm.paymentMethod,
          description: this.rechargeForm.description || '线下充值'
        }
        
        const response = await this.$http.post('/api/payment/recharge', paymentData)
        this.$message.success(response.data.message)
        
        // 重置表单，刷新数据
        this.resetForm()
        this.loadAccountBalance()
        this.loadPaymentRecords()
      } catch (error) {
        this.$message.error(error.response?.data?.message || '充值失败')
      } finally {
        this.processing = false
      }
    },
    
    selectQuickAmount(amount) {
      this.rechargeForm.amount = amount.toString()
    },
    
    resetForm() {
      this.rechargeForm = {
        amount: '',
        paymentMethod: 'WECHAT',
        description: ''
      }
      this.$refs.rechargeForm.resetFields()
    },
    
    getPaymentMethodText(method) {
      const methodMap = {
        'WECHAT': '微信支付',
        'ALIPAY': '支付宝',
        'OFFLINE': '线下支付'
      }
      return methodMap[method] || '未知'
    },
    
    formatDate(dateTime) {
      return formatDateTime(dateTime)
    }
  }
}
</script>

<style scoped>
.payment {
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

.recharge-card, .records-card {
  height: fit-content;
}

.balance-info {
  margin-bottom: 20px;
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  color: white;
  text-align: center;
}

.current-balance .label {
  font-size: 16px;
  margin-right: 10px;
}

.current-balance .amount {
  font-size: 32px;
  font-weight: 600;
}

.payment-icon {
  width: 20px;
  height: 20px;
  margin-right: 8px;
  vertical-align: middle;
}

.wechat-icon::before {
  content: 'W';
}

.alipay-icon::before {
  content: 'A';
}

.offline-icon::before {
  content: 'C';
}

.quick-amounts {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.quick-label {
  margin-right: 10px;
  color: #666;
}

.records-list {
  max-height: 500px;
  overflow-y: auto;
}

.record-item {
  padding: 15px;
  border: 1px solid #eee;
  border-radius: 8px;
  margin-bottom: 10px;
  background: white;
}

.record-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.record-header .amount {
  font-size: 18px;
  font-weight: 600;
  color: #67C23A;
}

.record-header .date {
  color: #999;
  font-size: 14px;
}

.record-details {
  font-size: 14px;
}

.detail-row {
  margin-bottom: 5px;
  color: #666;
}

.detail-row .label {
  color: #333;
  font-weight: 500;
}

.transaction-no {
  font-family: monospace;
  font-size: 12px;
}

.empty-container {
  text-align: center;
  padding: 40px;
}

/* 二维码支付弹窗样式 */
.qr-payment {
  text-align: center;
}

.payment-info h3 {
  margin: 0 0 10px 0;
  color: #333;
}

.payment-info .amount {
  font-size: 24px;
  font-weight: 600;
  color: #E6A23C;
  margin: 0 0 20px 0;
}

.qr-code {
  margin: 20px 0;
}

.qr-code img {
  width: 200px;
  height: 200px;
  border: 1px solid #eee;
}

.qr-tip {
  color: #666;
  font-size: 14px;
  margin: 20px 0;
}

.payment-status {
  display: flex;
  justify-content: center;
  gap: 10px;
}
</style> 