<template>
  <div class="evaluation-view">
    <!-- AHP权重配置 -->
    <AHPConfig @weights-calculated="handleWeightsCalculated" />

    <!-- 评估结果展示 -->
    <div v-if="evaluationResult" class="results-section">
      <DimensionCharts :evaluation-result="evaluationResult" :ahp-weights="ahpWeights" />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import AHPConfig from '@/components/AHPConfig.vue'
import DimensionCharts from '@/components/DimensionCharts.vue'

const ahpWeights = ref(null)
const evaluationResult = ref(null)

const handleWeightsCalculated = (data) => {
  ahpWeights.value = data.weights
  evaluationResult.value = data.evaluationResult
}
</script>

<style scoped lang="scss">
.evaluation-view {
  .results-section {
    margin-top: 20px;
  }
}
</style>
