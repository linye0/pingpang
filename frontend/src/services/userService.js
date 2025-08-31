import axios from 'axios'

const API_BASE_URL = process.env.VUE_APP_API_BASE_URL || 'http://localhost:8081'

class UserService {
  
  // 学员注册
  async registerStudent(studentData) {
    try {
      const response = await axios.post(`${API_BASE_URL}/api/public/student/register`, studentData)
      return response.data
    } catch (error) {
      throw error
    }
  }

  // 教练注册
  async registerCoach(coachData) {
    try {
      const response = await axios.post(`${API_BASE_URL}/api/public/coach/register`, coachData)
      return response.data
    } catch (error) {
      throw error
    }
  }

  // 获取用户信息
  async getUserProfile() {
    try {
      const response = await axios.get(`${API_BASE_URL}/api/user/profile`)
      return response.data
    } catch (error) {
      throw error
    }
  }

  // 更新用户信息
  async updateProfile(userData) {
    try {
      const response = await axios.put(`${API_BASE_URL}/api/user/profile`, userData)
      return response.data
    } catch (error) {
      throw error
    }
  }

  // 修改密码
  async changePassword(passwordData) {
    try {
      const response = await axios.post(`${API_BASE_URL}/api/user/change-password`, passwordData)
      return response.data
    } catch (error) {
      throw error
    }
  }

  // 获取我的教练
  async getMyCoaches() {
    try {
      const response = await axios.get(`${API_BASE_URL}/api/user/my-coaches`)
      return response.data
    } catch (error) {
      throw error
    }
  }
}

export default new UserService() 