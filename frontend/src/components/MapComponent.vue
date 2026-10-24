<template>
  <div class="map-container">
    <div ref="mapRef" class="map"></div>
    <div class="map-controls">
      <h3>垃圾分布地图</h3>
      <n-button type="primary" @click="showHeatmap">显示热点图</n-button>
      <n-button @click="hideHeatmap">隐藏热点图</n-button>
      <n-button @click="refreshData">刷新数据</n-button>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * MapComponent.vue - 地图可视化组件
 * 
 * 此组件基于百度地图API实现垃圾分布的可视化展示：
 * 1. 地图初始化和基本控件配置
 * 2. 垃圾位置标记点展示
 * 3. 热力图热点分析
 * 4. 信息窗口交互
 * 
 * 功能特点：
 * - 支持普通地图和卫星地图切换
 * - 热力图显示垃圾密度分布
 * - 点击标记点查看详细信息
 * - 自动响应数据变化更新地图
 */

import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useGarbageStore } from '../stores/garbageStore'

// 声明百度地图API的全局类型
declare global {
  interface Window {
    BMap: any
    BMapLib: any
  }
}

const mapRef = ref<HTMLElement | null>(null)
const map = ref<any>(null)
const heatmap = ref<any>(null)
const markers = ref<any[]>([]) // 存储所有标记点实例

const garbageStore = useGarbageStore()

// 初始化地图
let initMapRetryTimer: any = null
let destroyed = false

const initMap = () => {

  if (!mapRef.value) {
    console.log('Map ref not available')
    initMapRetryTimer = setTimeout(initMap, 200)
    return
  }
  
  if (!window.BMap) {
    console.log('Baidu Map API not loaded yet, retrying...')
    initMapRetryTimer = setTimeout(initMap, 200)
    return
  }

  console.log('Baidu Map API loaded, creating map instance...')
  // 创建地图实例
  map.value = new window.BMap.Map(mapRef.value)
  
  // 设置地图中心点和缩放级别
  const point = new window.BMap.Point(121.4737, 31.2304) // 上海
  console.log('Setting map center to Shanghai:', point)
  map.value.centerAndZoom(point, 10) // 设置地图中心点为上海，缩放级别：城市级别
  
  // 添加控件
  console.log('Adding map controls...')
  map.value.addControl(new window.BMap.NavigationControl()) // 添加导航控件
  map.value.addControl(new window.BMap.ScaleControl()) // 添加比例尺控件
  map.value.addControl(new window.BMap.OverviewMapControl()) // 添加总览地图控件
  // 添加地图类型控件（只显示地图和卫星模式，移除三维模式）
  map.value.addControl(new window.BMap.MapTypeControl({
    mapTypes: [
      window.BMAP_NORMAL_MAP,
      window.BMAP_SATELLITE_MAP
    ]
  }))
   
  // 启用滚轮缩放
  map.value.enableScrollWheelZoom(true)
  console.log('Map initialized successfully')

  // 初始化热力图
  console.log('Initializing heatmap...')
  initHeatmap()

  // 添加标记
  console.log('Adding markers...')
  addMarkers()
}

// 初始化热力图
const initHeatmap = () => {
  if (!map.value) {
    console.log('Map not initialized, cannot create heatmap')
    return
  }
  
  if (!window.BMapLib) {
    // 动态加载热力图库
    console.log('Loading BMapLib.Heatmap...')
    const script = document.createElement('script')
    script.src = 'https://api.map.baidu.com/library/Heatmap/2.0/src/Heatmap_min.js'
    script.onload = () => {
      console.log('BMapLib.Heatmap loaded successfully')
      if (!destroyed) createHeatmap()
    }
    script.onerror = () => {
      console.error('Failed to load BMapLib.Heatmap')
    }
    document.head.appendChild(script)
  } else {
    createHeatmap()
  }
}

// 创建热力图
const createHeatmap = () => {
  if (!map.value || !window.BMapLib) {
    console.log('Cannot create heatmap: map or BMapLib not available')
    return
  }

  console.log('Creating heatmap overlay...')
  heatmap.value = new window.BMapLib.HeatmapOverlay({
    radius: 25,
    opacity: 0.7,
    gradient: {
      0.4: 'blue',
      0.6: 'cyan', // 青色
      0.7: 'lime', // 绿色
      0.8: 'yellow',
      1.0: 'red'
    }
  })

  console.log('Adding heatmap overlay to map...')
  map.value.addOverlay(heatmap.value)
  console.log('Updating heatmap data...')
  updateHeatmap()
  console.log('Showing heatmap by default...')
  heatmap.value.show()
  console.log('Heatmap initialized successfully')
}

// 更新热力图数据
const updateHeatmap = () => {
  if (!heatmap.value) {
    console.log('Cannot update heatmap: heatmap not initialized')
    return
  }

  console.log('Updating heatmap data...')
  console.log('Garbage data list length:', garbageStore.garbageDataList?.length || 0)
  
  const garbageList = Array.isArray(garbageStore.garbageDataList) ? garbageStore.garbageDataList : []
  
  const locationMap = new Map<string, number>()
  
  garbageList.forEach(item => {
    const lng = Number(item.longitude)
    const lat = Number(item.latitude)
    if (!Number.isFinite(lng) || !Number.isFinite(lat)) {
      console.log('Invalid coordinates:', item)
      return
    }
    const key = `${lng.toFixed(4)},${lat.toFixed(4)}` // 约10米精度
    locationMap.set(key, (locationMap.get(key) || 0) + 1)
  })
  
  const heatmapData = Array.from(locationMap.entries()).map(([key, count]) => {
    const [lng, lat] = key.split(',').map(Number)
    return { lng, lat, count }
  })

  console.log('Heatmap data points (after aggregation):', heatmapData.length)
  console.log('Heatmap data sample:', heatmapData.slice(0, 5))
  
  const maxCount = Math.max(...locationMap.values(), 10)

  heatmap.value.setDataSet({
    data: heatmapData,
    max: maxCount
  })
  console.log('Heatmap data updated successfully')
}

// 显示热力图
const showHeatmap = () => {
  if (heatmap.value) {
    heatmap.value.show()
  }
}

// 隐藏热力图
const hideHeatmap = () => {
  if (heatmap.value) {
    heatmap.value.hide()
  }
}

// 添加标记
const addMarkers = () => {
  if (!map.value) {
    return
  }

  // 清除现有标记
  markers.value.forEach(marker => {
    map.value.removeOverlay(marker)
  })
  markers.value = []

  // 添加新标记
  garbageStore.garbageDataList.forEach(item => {
    const lon = Number(item.longitude)
    const lat = Number(item.latitude)
    if (!Number.isFinite(lon) || !Number.isFinite(lat)) return
    const point = new window.BMap.Point(lon, lat)
    const marker = new window.BMap.Marker(point)
    
    // 添加信息窗口
    const infoWindow = new window.BMap.InfoWindow(`
      <div>
        <h4>垃圾信息</h4>
        <p>类型: ${item.garbageType?.name ?? ''}</p>
        <p>置信度: ${(item.confidence * 100).toFixed(2)}%</p>
        <p>时间: ${item.timestamp ? new Date(item.timestamp + 'Z').toLocaleString('zh-CN') : ''}</p>
        <p>位置: ${item.latitude}, ${item.longitude}</p>
      </div>
    `)

    marker.addEventListener('click', () => {
      marker.openInfoWindow(infoWindow)
    })

    map.value.addOverlay(marker)
    markers.value.push(marker)
  })
}

// 刷新数据
const refreshData = () => {
  garbageStore.fetchGarbageData(1, 50)
  addMarkers()
  updateHeatmap()
}

// 监听垃圾数据变化
watch(() => garbageStore.garbageDataList, () => {
  addMarkers()
  updateHeatmap()
}, { deep: true })

onMounted(() => {
  // 初始化地图
  initMap()
  // 加载垃圾数据
  garbageStore.fetchGarbageData(1, 50)
})

onUnmounted(() => {
  // 清除地图实例
  if (map.value) {
    map.value.clearOverlays() // 清除所有覆盖物
  }
  destroyed = true
  if (initMapRetryTimer) {
    clearTimeout(initMapRetryTimer)
  }
})
</script>

<style scoped>
.map-container {
  position: relative;
  width: 100%;
  height: 100%;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.map {
  width: 100%;
  height: 100%;
}

.map-controls {
  position: absolute;
  top: 10px;
  left: 10px;
  z-index: 100;
  background: white;
  padding: 10px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.map-controls h3 {
  margin: 0 0 10px 0;
  font-size: 16px;
  color: #333;
}

.map-controls .n-button {
  margin-right: 8px;
  margin-bottom: 8px;
}
</style>
