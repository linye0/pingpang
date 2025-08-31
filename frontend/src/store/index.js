import Vue from 'vue'
import Vuex from 'vuex'
import axios from 'axios'

Vue.use(Vuex)

export default new Vuex.Store({
  state: {
    user: null,
    token: null,
    campuses: []
  },
  
  mutations: {
    SET_USER(state, user) {
      state.user = user
      localStorage.setItem('user', JSON.stringify(user))
    },
    
    SET_TOKEN(state, token) {
      state.token = token
      localStorage.setItem('token', token)
    },
    
    SET_CAMPUSES(state, campuses) {
      state.campuses = campuses
    },
    
    LOGOUT(state) {
      state.user = null
      state.token = null
      localStorage.removeItem('user')
      localStorage.removeItem('token')
    }
  },
  
  actions: {
    async login({ commit }, credentials) {
      try {
        // 确保发送的是普通对象而不是Vue响应式对象
        const loginData = {
          username: credentials.username,
          password: credentials.password
        }
        console.log('发送登录请求:', loginData)
        const response = await axios.post('/api/auth/login', loginData)
        console.log('登录响应:', response.data)
        
        // 根据实际API响应结构调整
        if (response.data.code === 200) {
          const { token, user } = response.data.data
          
          commit('SET_TOKEN', token)
          commit('SET_USER', user)
          
          console.log('登录成功，token和user已设置')
          return { success: true }
        } else {
          return { 
            success: false, 
            message: response.data.message || '登录失败' 
          }
        }
      } catch (error) {
        console.error('登录错误:', error)
        return { 
          success: false, 
          message: error.response?.data?.message || '登录失败' 
        }
      }
    },
    
    async fetchCampuses({ commit }) {
      try {
        const response = await axios.get('/api/public/campuses')
        commit('SET_CAMPUSES', response.data.data)
      } catch (error) {
        console.error('获取校区列表失败:', error)
      }
    },
    
    logout({ commit }) {
      commit('LOGOUT')
    },
    
    initStore({ commit }) {
      const token = localStorage.getItem('token')
      const user = localStorage.getItem('user')
      
      if (token) {
        commit('SET_TOKEN', token)
      }
      
      if (user) {
        commit('SET_USER', JSON.parse(user))
      }
    }
  },
  
  getters: {
    isLoggedIn: state => !!state.token,
    userRole: state => state.user?.role,
    userName: state => state.user?.realName,
    userCampus: state => state.user?.campusName
  }
}) 