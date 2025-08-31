import moment from 'moment'

export function formatDate(dateTime, format = 'YYYY-MM-DD HH:mm') {
  if (!dateTime) {
    console.warn('formatDate: 传入的dateTime为空', dateTime)
    return '未知时间'
  }
  try {
    console.log('formatDate: 原始数据', dateTime, '类型:', typeof dateTime)
    const date = moment(dateTime)
    console.log('formatDate: moment对象', date, '是否有效:', date.isValid())
    if (!date.isValid()) {
      console.error('formatDate: 日期无效', dateTime)
      return '日期格式错误'
    }
    const result = date.format(format)
    console.log('formatDate: 格式化结果', result)
    return result
  } catch (error) {
    console.error('formatDate: 日期格式化错误:', error, '原始数据:', dateTime)
    return '日期解析失败'
  }
}

export function formatDateOnly(dateTime) {
  if (!dateTime) {
    return '未知日期'
  }
  try {
    const date = moment(dateTime)
    if (!date.isValid()) {
      return '日期格式错误'
    }
    return date.format('YYYY-MM-DD')
  } catch (error) {
    console.error('日期格式化错误:', error, dateTime)
    return '日期解析失败'
  }
}

export function formatTime(dateTime) {
  if (!dateTime) {
    return '未知时间'
  }
  try {
    const date = moment(dateTime)
    if (!date.isValid()) {
      return '时间格式错误'
    }
    return date.format('HH:mm')
  } catch (error) {
    console.error('时间格式化错误:', error, dateTime)
    return '时间解析失败'
  }
}

export function formatDateTime(dateTime) {
  console.log('formatDateTime: 调用formatDate', dateTime)
  return formatDate(dateTime, 'YYYY-MM-DD HH:mm')
}

export function formatDateTimeShort(dateTime) {
  return formatDate(dateTime, 'MM-DD HH:mm')
}

export function formatBookingTime(startTime, endTime) {
  if (!startTime || !endTime) {
    return '时间待定'
  }
  try {
    const start = moment(startTime)
    const end = moment(endTime)
    if (!start.isValid() || !end.isValid()) {
      return '时间格式错误'
    }
    
    if (start.isSame(end, 'day')) {
      // 同一天，只显示一次日期
      return `${start.format('YYYY-MM-DD')} ${start.format('HH:mm')}-${end.format('HH:mm')}`
    } else {
      // 跨天
      return `${start.format('YYYY-MM-DD HH:mm')} - ${end.format('YYYY-MM-DD HH:mm')}`
    }
  } catch (error) {
    console.error('预约时间格式化错误:', error, startTime, endTime)
    return '时间解析失败'
  }
}

export function isToday(dateTime) {
  if (!dateTime) return false
  try {
    return moment(dateTime).isSame(moment(), 'day')
  } catch (error) {
    return false
  }
}

export function isFuture(dateTime) {
  if (!dateTime) return false
  try {
    return moment(dateTime).isAfter(moment())
  } catch (error) {
    return false
  }
}