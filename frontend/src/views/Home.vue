<template>
  <!-- 首页容器 -->
  <div class="home-container">
    <!-- 英雄区域 - 展示系统核心价值 -->
    <section class="hero-section">
      <div class="hero-content">
        <h1 class="hero-title">
          <span class="hero-emphasis">OceanGuard YOLO</span>
          <br />
          智能海漂垃圾识别与可视化系统
        </h1>
        <p class="hero-subtitle">
          利用先进的YOLO模型技术，实时识别海洋垃圾，通过直观的可视化界面，
          帮助您了解海洋垃圾的分布情况和变化趋势，共同守护蓝色家园。
        </p>
        <div class="hero-actions">
          <n-button 
            type="primary" 
            size="large" 
            class="hero-button"
            @click="goToGarbageData"
          >
            查看垃圾数据
          </n-button>
          <n-button 
            type="default" 
            size="large" 
            class="hero-button secondary"
            @click="goToStatistics"
          >
            查看数据统计
          </n-button>
          <n-button 
            type="info" 
            size="large" 
            class="hero-button"
            @click="goToGarbageDetection"
          >
            开始识别
          </n-button>
        </div>
      </div>
      <div class="hero-visual">
        <div class="hero-image">
          <img 
            src="https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=ocean%20garbage%20detection%20system%20interface%20with%20map%20visualization%20and%20data%20charts%2C%20modern%20UI%2C%20clean%20design&image_size=landscape_16_9" 
            alt="海漂垃圾识别系统" 
            class="hero-img"
          />
        </div>
      </div>
    </section>

    <!-- 核心功能区域 -->
    <section class="features-section">
      <h2 class="section-title">核心功能</h2>
      <div class="features-grid">
        <!-- 智能垃圾识别功能卡片 -->
        <div class="feature-card" @click="goToGarbageDetection" style="cursor: pointer;">
          <div class="feature-icon">
            <n-icon :size="48">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm3.88-11.71L10 15.09V17h-2v-2.71l5.88-8.09c.32-.45.27-1.07-.12-1.44-.39-.37-1.01-.37-1.4 0z"/>
              </svg>
            </n-icon>
          </div>
          <h3 class="feature-title">智能垃圾识别</h3>
          <p class="feature-description">
            使用先进的YOLO模型对海洋垃圾进行实时识别，识别准确率高，响应速度快。
          </p>
        </div>
        
        <!-- 数据管理功能卡片 -->
        <div class="feature-card" @click="goToGarbageData" style="cursor: pointer;">
          <div class="feature-icon">
            <n-icon :size="48">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor">
                <path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H5V5h14v14zM7 10h2v7H7zm4-3h2v10h-2zm4 6h2v4h-2z"/>
              </svg>
            </n-icon>
          </div>
          <h3 class="feature-title">数据管理</h3>
          <p class="feature-description">
            全面的垃圾识别数据管理功能，支持数据的存储、查询、筛选和导出，为您提供完整的数据管理体验。
          </p>
        </div>
        
        <!-- 可视化展示功能卡片 -->
        <div class="feature-card" @click="goToStatistics" style="cursor: pointer;">
          <div class="feature-icon">
            <n-icon :size="48">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor">
                <path d="M21.41 11.58l-9-9C12.05 2.22 11.55 2 11 2H4c-1.1 0-2 .9-2 2v7c0 .55.22 1.05.59 1.42l9 9c.36.36.86.58 1.41.58s1.05-.22 1.41-.59l7-7c.37-.36.59-.86.59-1.41s-.23-1.06-.59-1.42zM12 20l-8-8v-7h8l8 8v7h-8z"/>
                <path d="M12 6.5c1.38 0 2.5 1.12 2.5 2.5s-1.12 2.5-2.5 2.5-2.5-1.12-2.5-2.5 1.12-2.5 2.5-2.5zM12 10c.28 0 .5-.22.5-.5s-.22-.5-.5-.5-.5.22-.5.5.22.5.5.5z"/>
              </svg>
            </n-icon>
          </div>
          <h3 class="feature-title">可视化展示</h3>
          <p class="feature-description">
            通过交互式地图和数据图表，直观展示垃圾分布、数量变化及热点区域，让数据一目了然。
          </p>
        </div>
        
        <!-- 用户管理功能卡片（仅管理员可见） -->
        <div v-if="userStore.user?.role === 'admin'" class="feature-card" @click="goToUserManagement" style="cursor: pointer;">
          <div class="feature-icon">
            <n-icon :size="48">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor">
                <path d="M16 11c1.66 0 2.99-1.34 2.99-3L19 4c0-1.66-1.34-3-3-3S13 2.34 13 4v4c0 1.66 1.34 3 3 3zm-3 2c2.21 0 4 1.79 4 4s-1.79 4-4 4-4-1.79-4-4 1.79-4 4-4zm0 6c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zM7 11c1.66 0 2.99-1.34 2.99-3L10 4c0-1.66-1.34-3-3-3S4 2.34 4 4v4c0 1.66 1.34 3 3 3zm-3 2c2.21 0 4 1.79 4 4s-1.79 4-4 4-4-1.79-4-4 1.79-4 4-4zm0 6c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2z"/>
              </svg>
            </n-icon>
          </div>
          <h3 class="feature-title">用户管理</h3>
          <p class="feature-description">
            完善的用户管理系统，支持用户注册、登录、权限管理等功能，确保系统安全稳定运行。
          </p>
        </div>
      </div>
    </section>

    <!-- 地图可视化区域（登录后可见） -->
    <section v-if="userStore.isLoggedIn" class="map-section">
      <h2 class="section-title">垃圾分布地图</h2>
      <div class="map-card">
        <MapComponent />
      </div>
    </section>

    <!-- 数据统计区域（登录后可见） -->
    <section v-if="userStore.isLoggedIn" class="stats-section">
      <h2 class="section-title">系统概览</h2>
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-header">
            <h3 class="stat-title">垃圾类型统计</h3>
          </div>
          <div class="stat-content">
            <n-empty v-if="!garbageStore || garbageStore.garbageTypes?.length === 0" description="暂无数据" />
            <div v-else class="stats-container">
              <div v-for="type in garbageStore.garbageTypes" :key="type.id" class="stat-item">
                <div class="stat-label">{{ type.name }}</div>
                <div class="stat-number">{{ getTypeCount(type.name) }}</div>
              </div>
            </div>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-header">
            <h3 class="stat-title">系统状态</h3>
          </div>
          <div class="stat-content">
            <div class="stats-container">
              <div class="stat-item">
                <div class="stat-label">识别模型</div>
                <div class="stat-number">YOLOv11</div>
              </div>
              <div class="stat-item">
                <div class="stat-label">数据总量</div>
                <div class="stat-number">{{ getTotalCount() }}条</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 最近识别记录（登录后可见） -->
    <section v-if="userStore.isLoggedIn" class="recent-section">
      <h2 class="section-title">最近识别</h2>
      <div class="recent-card">
        <n-empty v-if="!garbageStore || garbageStore.garbageDataList?.length === 0" description="暂无数据" />
        <n-data-table 
          v-else 
          :columns="columns" 
          :data="(garbageStore.garbageDataList || []).slice(0, 5)" 
          class="recent-table"
        />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
/**
 * Home.vue - 首页组件
 * 
 * 此组件是应用的首页，展示系统核心功能和数据概览：
 * 1. 英雄区域：展示系统名称、简介和快捷操作按钮
 * 2. 核心功能：展示智能识别、数据管理、可视化、用户管理（管理员）
 * 3. 地图可视化：展示垃圾分布地图（登录后可见）
 * 4. 数据统计：展示垃圾类型统计和系统状态（登录后可见）
 * 5. 最近识别：展示最近的垃圾识别记录（登录后可见）
 */

import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useGarbageStore } from '../stores/garbageStore'
import MapComponent from '../components/MapComponent.vue'
import { useUserStore } from '../stores/userStore'

// 初始化路由和状态管理
const router = useRouter()
const garbageStore = useGarbageStore()
const userStore = useUserStore()

/**
 * 导航到垃圾数据页面
 */
const goToGarbageData = () => {
  router.push('/garbage-data')
}

/**
 * 导航到统计页面
 */
const goToStatistics = () => {
  router.push('/statistics')
}

/**
 * 导航到垃圾识别页面
 */
const goToGarbageDetection = () => {
  router.push('/garbage-detection')
}

/**
 * 导航到登录页面
 */
const goToLogin = () => {
  router.push('/login')
}

/**
 * 导航到用户管理页面（管理员）
 */
const goToUserManagement = () => {
  router.push('/user-management')
}

/**
 * 最近识别表格的列配置
 */
const columns = [
  { title: 'ID', key: 'id' },
  { title: '垃圾类型', key: 'garbageType.name' },
  { 
    title: '置信度', 
    key: 'confidence', 
    render: (row: any) => `${(row.confidence * 100).toFixed(2)}%` 
  },
  { title: '识别时间', key: 'timestamp' }
]

/**
 * 根据垃圾类型名称获取统计数量
 * @param {string} typeName - 垃圾类型名称
 * @returns {number} 该类型的数量
 */
const getTypeCount = (typeName: string) => {
  if (!garbageStore || !garbageStore.statistics) return 0
  const stat = garbageStore.statistics.find((item: any) => item.name === typeName)
  return stat ? stat.value : 0
}

/**
 * 获取垃圾数据总量
 * @returns {number} 数据总量
 */
const getTotalCount = () => {
  if (!garbageStore || !garbageStore.statistics) return 0
  return garbageStore.statistics.reduce((total: number, item: any) => total + (item.value || 0), 0)
}

/**
 * 组件挂载时执行的初始化操作
 */
onMounted(() => {
  console.log('Home component mounted')
  
  // 仅登录用户加载数据
  if (userStore.isLoggedIn) {
    // 加载垃圾类型列表
    garbageStore.fetchGarbageTypes().then(() => {
      console.log('Garbage types:', garbageStore.garbageTypes)
    })
    
    // 加载垃圾数据列表
    garbageStore.fetchGarbageData().then(() => {
      console.log('Garbage data:', garbageStore.garbageDataList)
    })
    // 设置默认时间范围为全部时间（传递空字符串获取所有数据）
    const endTime = ''
    const startTime = ''
    garbageStore.fetchStatisticsByType(startTime, endTime).then(() => {
      console.log('Statistics:', garbageStore.statistics)
    })
  }
})
</script>

<style scoped>
/* 首页容器 */
.home-container {
  display: flex;
  flex-direction: column;
  gap: 4rem;
  padding-bottom: 4rem;
}

/* 英雄区域 */
.hero-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4rem;
  align-items: center;
  padding: 4rem 0;
  background-color: var(--card-background);
  border-radius: 1rem;
  box-shadow: var(--shadow-lg);
  overflow: hidden;
  animation: fadeInUp 0.8s ease;
}

.hero-content {
  padding: 0 2rem;
}

.hero-title {
  font-size: 2.5rem;
  font-weight: 700;
  line-height: 1.2;
  margin-bottom: 1.5rem;
  color: var(--text-primary);
}

.hero-emphasis {
  color: var(--primary-color);
}

.hero-subtitle {
  font-size: 1.25rem;
  margin-bottom: 2rem;
  color: var(--text-secondary);
  line-height: 1.5;
}

.hero-actions {
  display: flex;
  gap: 1rem;
}

.hero-button {
  padding: 0.75rem 2rem;
  border-radius: 0.5rem;
  font-weight: 600;
  transition: all 0.3s ease;
}

.hero-button.secondary {
  background-color: transparent;
  border: 1px solid var(--primary-color);
  color: var(--primary-color);
}

.hero-button.secondary:hover {
  background-color: var(--primary-color);
  color: white;
}

.hero-visual {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 2rem;
}

.hero-image {
  border-radius: 1rem;
  overflow: hidden;
  box-shadow: var(--shadow-lg);
}

.hero-img {
  width: 100%;
  height: auto;
  border-radius: 1rem;
}

/* 功能区域 */
.features-section {
  animation: fadeInUp 0.8s ease 0.2s both;
}

.section-title {
  font-size: 2rem;
  font-weight: 700;
  margin-bottom: 2rem;
  color: var(--text-primary);
  text-align: center;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 2rem;
}

.feature-card {
  background-color: var(--card-background);
  border-radius: 1rem;
  padding: 2rem;
  box-shadow: var(--shadow-md);
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.feature-card:hover {
  transform: translateY(-5px);
  box-shadow: var(--shadow-lg);
}

.feature-icon {
  width: 80px;
  height: 80px;
  background-color: rgba(100, 108, 255, 0.1);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 1.5rem;
  color: var(--primary-color);
}

.feature-title {
  font-size: 1.25rem;
  font-weight: 600;
  margin-bottom: 1rem;
  color: var(--text-primary);
}

.feature-description {
  color: var(--text-secondary);
  line-height: 1.5;
}

/* 地图区域 */
.map-section {
  animation: fadeInUp 0.8s ease 0.4s both;
}

.map-card {
  background-color: var(--card-background);
  border-radius: 1rem;
  box-shadow: var(--shadow-md);
  overflow: hidden;
  height: 500px;
}

/* 统计区域 */
.stats-section {
  animation: fadeInUp 0.8s ease 0.6s both;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 2rem;
}

.stat-card {
  background-color: var(--card-background);
  border-radius: 1rem;
  box-shadow: var(--shadow-md);
  overflow: hidden;
  transition: all 0.3s ease;
}

.stat-card:hover {
  box-shadow: var(--shadow-lg);
}

.stat-header {
  padding: 1.5rem;
  border-bottom: 1px solid var(--border-color);
}

.stat-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.stat-content {
  padding: 2rem;
  min-height: 200px;
}

.stats-container {
  display: flex;
  justify-content: space-around;
  align-items: center;
  flex-wrap: wrap;
  gap: 1.5rem;
}

.stat-item {
  flex: 1;
  min-width: 120px;
  text-align: center;
}

.stat-value {
  font-size: 1.5rem;
}

.stat-value .n-statistic__title {
  font-size: 0.875rem;
  color: var(--text-secondary);
  margin-bottom: 0.5rem;
}

.stat-value .n-statistic__content {
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--text-primary);
}

.stat-label {
  font-size: 0.875rem;
  color: var(--text-secondary);
  margin-bottom: 0.5rem;
}

.stat-number {
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--text-primary);
}

/* 最近识别区域 */
.recent-section {
  animation: fadeInUp 0.8s ease 0.8s both;
}

.recent-card {
  background-color: var(--card-background);
  border-radius: 1rem;
  box-shadow: var(--shadow-md);
  overflow: hidden;
  transition: all 0.3s ease;
}

.recent-card:hover {
  box-shadow: var(--shadow-lg);
}

.recent-table {
  width: 100%;
}

/* 动画效果 */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .hero-section {
    grid-template-columns: 1fr;
    text-align: center;
  }
  
  .hero-actions {
    justify-content: center;
  }
  
  .stats-grid {
    grid-template-columns: 1fr;
  }
  
  .hero-title {
    font-size: 2rem;
  }
  
  .hero-subtitle {
    font-size: 1rem;
  }
  
  .section-title {
    font-size: 1.5rem;
  }
}
</style>