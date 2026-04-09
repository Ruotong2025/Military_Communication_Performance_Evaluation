<template>
  <div class="ahp-shared-legends">
    <!-- Saaty 标度 -->
    <div class="scale-section scale-legend-block">
      <h3 class="scale-legend-title">
        <el-icon><InfoFilled /></el-icon>
        AHP 判断标度说明（Saaty 1～9 标度）
      </h3>
      <p class="scale-legend-intro">
        上三角「重要性」为 <strong>Saaty 正互反标度</strong>，取值区间 <strong>[1/9，9]</strong>（可填小数）：表示<strong>行要素相对列要素</strong>。<strong>1</strong> 为同等重要；<strong>&gt;1</strong> 表示行比列重要（如 3、5）；<strong>&lt;1</strong> 表示行不如列重要（如 0.33≈1/3、0.2=1/5）。下三角为互反倒数。
      </p>
      <el-table :data="ahpScaleFullLegend" border stripe size="small" class="scale-legend-table">
        <el-table-column prop="scale" label="标度" width="110" align="center" />
        <el-table-column prop="meaning" label="含义" min-width="100" />
        <el-table-column prop="note" label="说明" min-width="200" />
        <el-table-column prop="scene" label="适用场景" min-width="180" />
      </el-table>
    </div>

    <!-- 把握度 λ -->
    <div class="scale-section scale-legend-block confidence-legend-block">
      <h3 class="scale-legend-title">
        <el-icon><InfoFilled /></el-icon>
        把握度等级与判断可信度 λ
      </h3>
      <p class="scale-legend-intro">
        矩阵上三角中「把握度」为 0～1 的小数，表示您对该次<strong>重要性比较</strong>的可信程度，可按下表<strong>判断可信度 λ</strong>取值：
        1 级对应 λ=1，2 级对应 0.8，3 级对应 0.6；4 级表示不能确认，建议 λ 取 <strong>小于 0.6</strong> 的数值（如 0.5、0.4）。新选专家且无已存数据时，系统默认标度全为 1、把握度为 {{ defaultBlankConfidence }}（低于 0.6）。
      </p>
      <el-table :data="ahpConfidenceLevelLegend" border stripe size="small" class="scale-legend-table">
        <el-table-column prop="level" label="等级" width="72" align="center" />
        <el-table-column prop="meaning" label="等级含义" width="100" />
        <el-table-column prop="standard" label="等级标准" min-width="280" />
        <el-table-column prop="lambda" label="判断可信度 λ" width="120" align="center" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { InfoFilled } from '@element-plus/icons-vue'
import {
  defaultBlankConfidence,
  ahpScaleFullLegend,
  ahpConfidenceLevelLegend
} from '@/config/ahpComparisons'
</script>

<style scoped lang="scss">
.ahp-shared-legends {
  .scale-section {
    margin-bottom: 20px;
    padding: 10px;
    background: #f5f7fa;
    border-radius: 4px;
  }

  .scale-legend-block {
    padding: 16px 18px;
    background: #fff;
    border: 1px solid #e4e7ed;
    border-radius: 8px;
    box-shadow: 0 1px 6px rgba(0, 31, 63, 0.06);
  }

  .confidence-legend-block {
    margin-top: 20px;
  }

  .scale-legend-title {
    display: flex;
    align-items: center;
    gap: 8px;
    margin: 0 0 10px;
    font-size: 16px;
    font-weight: bold;
    color: var(--navy-primary, #0d3a66);
  }

  .scale-legend-intro {
    margin: 0 0 14px;
    font-size: 13px;
    line-height: 1.65;
    color: #606266;
  }

  .scale-legend-table {
    width: 100%;
  }
}
</style>
