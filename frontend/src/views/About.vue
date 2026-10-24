<template>
  <div class="about-container">
    <n-card title="关于系统" class="about-card">
      <div class="about-content">
        <h2>基于YOLO的海漂垃圾识别与可视化系统</h2>
        <p>版本：2.0.0</p>
        <div class="section">
          <h3>系统介绍</h3>
          <p>基于YOLO的海漂垃圾识别与可视化系统是一个集成了人工智能识别、数据管理、可视化展示和智能助手的综合性平台。该系统通过YOLO模型对海漂垃圾进行智能识别，并将识别结果通过地图可视化的方式展示出来，帮助用户直观了解海洋垃圾的分布情况和变化趋势。系统还集成了AI智能助手，为用户提供实时咨询和网站改进建议。</p>
        </div>
        <div class="section">
          <h3>技术栈</h3>
          <ul class="tech-stack">
            <li><strong>后端</strong>：Spring Boot 3 + MySQL8 + MyBatis</li>
            <li><strong>前端</strong>：Vue3 + Vite + Pinia + Naive UI + ECharts</li>
            <li><strong>模型推理</strong>：FastAPI + YOLO</li>
            <li><strong>AI服务</strong>：DeepSeek + GLM-4.7-Flash</li>
            <li><strong>地图SDK</strong>：百度地图</li>
          </ul>
        </div>
        <div class="section">
          <h3>核心功能</h3>
          <ul class="tech-stack">
            <li><strong>垃圾识别</strong>：使用YOLO模型对海洋垃圾进行实时识别</li>
            <li><strong>数据管理</strong>：存储、管理和查询垃圾识别数据</li>
            <li><strong>可视化展示</strong>：通过地图和图表展示垃圾分布、数量变化及热点区域</li>
            <li><strong>统计分析</strong>：垃圾类型分布和时间趋势分析</li>
            <li><strong>AI智能助手</strong>：提供实时咨询和网站改进建议</li>
            <li><strong>用户管理</strong>：管理系统用户和权限</li>
          </ul>
        </div>
        <div class="section">
          <h3>开发团队</h3>
          <p>本系统致力于通过技术手段解决海洋垃圾问题，保护海洋生态环境，共同致力于开发高效、智能的海洋垃圾监测解决方案。</p>
        </div>
        <div class="section">
          <h3>联系方式</h3>
          <p>如有任何问题或建议，欢迎联系我们。我们将不断优化系统功能，为海洋环境保护贡献力量。</p>
        </div>
      </div>
    </n-card>
  </div>
</template>

<script setup lang="ts">
/**
 * About.vue - 关于系统页面组件
 * 
 * 此组件展示系统的基本信息，包括：
 * 1. 系统介绍和版本信息
 * 2. 技术栈说明
 * 3. 核心功能列表
 * 4. 开发团队和联系方式
 */
</script>

<style scoped>
.about-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.about-content {
  line-height: 1.6;
}

.section {
  margin-top: 20px;
}

.section h3 {
  margin-bottom: 10px;
  color: #1890ff;
}

.tech-stack {
  padding-left: 20px;
}

.tech-stack li {
  margin-bottom: 5px;
}
</style>
