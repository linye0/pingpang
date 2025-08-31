import Vue from 'vue'
import VueRouter from 'vue-router'
import store from '../store'

// 页面组件
import Login from '../views/Login.vue'
import Dashboard from '../views/Dashboard.vue'
import StudentRegister from '../views/StudentRegister.vue'
import CoachRegister from '../views/CoachRegister.vue'
import CoachSearch from '../views/CoachSearch.vue'
import CourseBooking from '../views/CourseBooking.vue'
import CourseList from '../views/CourseList.vue'
import CoachSchedule from '../views/CoachSchedule.vue'
import CoachWorkingTime from '../views/CoachWorkingTime.vue'
import SystemMessages from '../views/SystemMessages.vue'
import Profile from '../views/Profile.vue'
import Payment from '../views/Payment.vue'
import AdminPanel from '../views/AdminPanel.vue'
import Competition from '../views/Competition.vue'
import CoachStudents from '../views/CoachStudents.vue'
import Evaluation from '../views/Evaluation.vue'

Vue.use(VueRouter)

const routes = [
  {
    path: '/',
    name: 'Home'
  },
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { requiresAuth: false }
  },
  {
    path: '/register/student',
    name: 'StudentRegister',
    component: StudentRegister,
    meta: { requiresAuth: false }
  },
  {
    path: '/register/coach',
    name: 'CoachRegister',
    component: CoachRegister,
    meta: { requiresAuth: false }
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: Dashboard,
    meta: { requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: Profile,
    meta: { requiresAuth: true }
  },
  {
    path: '/coach/search',
    name: 'CoachSearch',
    component: CoachSearch,
    meta: { 
      requiresAuth: true,
      roles: ['STUDENT']
    }
  },
  {
    path: '/course/booking',
    name: 'CourseBooking',
    component: () => import('../views/CourseBooking.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/course/list',
    name: 'CourseList',
    component: CourseList,
    meta: { 
      requiresAuth: true,
      roles: ['STUDENT', 'COACH']
    }
  },
  {
    path: '/coach/schedule',
    name: 'CoachSchedule',
    component: CoachSchedule,
    meta: { 
      requiresAuth: true,
      roles: ['COACH']
    }
  },
  {
    path: '/coach/working-time',
    name: 'CoachWorkingTime',
    component: CoachWorkingTime,
    meta: { 
      requiresAuth: true,
      roles: ['COACH']
    }
  },
  {
    path: '/messages',
    name: 'SystemMessages',
    component: SystemMessages,
    meta: { 
      requiresAuth: true,
      roles: ['COACH', 'STUDENT']
    }
  },
  {
    path: '/payment',
    name: 'Payment',
    component: () => import('../views/Payment.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/admin',
    name: 'AdminPanel',
    component: AdminPanel,
    meta: { 
      requiresAuth: true,
      roles: ['CAMPUS_ADMIN', 'SUPER_ADMIN']
    }
  },
  {
    path: '/competition',
    name: 'Competition',
    component: Competition,
    meta: { 
      requiresAuth: true,
      roles: ['STUDENT']
    }
  },
  {
    path: '/coach/students',
    name: 'CoachStudents',
    component: CoachStudents,
    meta: { 
      requiresAuth: true,
      roles: ['COACH']
    }
  },
  {
    path: '/evaluation',
    name: 'Evaluation',
    component: () => import('../views/Evaluation.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/competition/:competitionId/schedule',
    name: 'CompetitionSchedule',
    component: () => import('../views/CompetitionSchedule.vue'),
    meta: { requiresAuth: true }
  }
]

const router = new VueRouter({
  mode: 'history',
  base: process.env.BASE_URL,
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  // 初始化store
  store.dispatch('initStore')
  
  const isLoggedIn = store.getters.isLoggedIn
  const userRole = store.getters.userRole
  
  console.log('路由守卫:', { 
    to: to.path, 
    from: from.path, 
    isLoggedIn, 
    userRole,
    requiresAuth: to.meta.requiresAuth
  })
  
  // 默认路由重定向
  if (to.path === '/') {
    if (isLoggedIn) {
      next('/dashboard')
    } else {
      next('/login')
    }
    return
  }
  
  // 检查是否需要认证
  if (to.meta.requiresAuth && !isLoggedIn) {
    next('/login')
    return
  }
  
  // 检查角色权限
  if (to.meta.roles && isLoggedIn) {
    if (!to.meta.roles.includes(userRole)) {
      next('/dashboard')
      return
    }
  }
  
  next()
})

export default router 