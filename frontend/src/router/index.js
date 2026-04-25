import { createRouter, createWebHistory } from 'vue-router'
import DataView from '../views/DataView.vue'
import EvaluationView from '../views/EvaluationView.vue'
import ExpertCredibility from '../views/ExpertCredibility.vue'
import ExpertAggregation from '../views/ExpertAggregation.vue'
import SimulationTrainingLayout from '../views/SimulationTrainingLayout.vue'
import SimulationTrainingHome from '../views/SimulationTrainingHome.vue'
import ComprehensiveScoringView from '../views/ComprehensiveScoringView.vue'
import PenaltyFactorAnalysisView from '../views/PenaltyFactorAnalysisView.vue'
import CostEffectivenessAnalysisView from '../views/CostEffectivenessAnalysisView.vue'
import ExpertEquipmentEvaluation from '../views/ExpertEquipmentEvaluation.vue'
import DynamicIndicatorView from '../views/DynamicIndicatorView.vue'
import DynamicQuantitativeView from '../views/DynamicQuantitativeView.vue'

const routes = [
  {
    path: '/',
    redirect: '/simulation-training'
  },
  {
    path: '/data',
    redirect: '/simulation-training/data'
  },
  {
    path: '/evaluation',
    redirect: '/simulation-training/weights/evaluation'
  },
  {
    path: '/expert',
    redirect: '/simulation-training/weights/expert'
  },
  {
    path: '/expert-aggregation',
    redirect: '/simulation-training/weights/aggregation-scoring'
  },
  {
    path: '/simulation-training/expert-aggregation',
    redirect: '/simulation-training/weights/aggregation-scoring'
  },
  {
    path: '/simulation-training',
    component: SimulationTrainingLayout,
    children: [
      {
        path: '',
        name: 'SimulationTrainingNav',
        component: SimulationTrainingHome
      },
      {
        path: 'data',
        name: 'SimulationData',
        component: DataView
      },
      {
        path: 'weights/expert',
        name: 'SimulationWeightsExpert',
        component: ExpertCredibility
      },
      {
        path: 'weights/evaluation',
        name: 'SimulationWeightsEvaluation',
        component: EvaluationView
      },
      {
        path: 'weights/ahp-dispersion',
        name: 'ExpertAhpDispersion',
        component: ExpertAggregation,
        props: () => ({ pageMode: 'dispersion' })
      },
      {
        path: 'weights/aggregation-scoring',
        name: 'WeightAggregationScoring',
        component: ExpertAggregation,
        props: () => ({ pageMode: 'collective' })
      },
      {
        path: 'collective-scoring',
        redirect: { name: 'WeightAggregationScoring' }
      },
      {
        path: 'results/comprehensive-scoring',
        name: 'ResultsComprehensiveScoring',
        component: ComprehensiveScoringView
      },
      {
        path: 'results/penalty-factor',
        name: 'ResultsPenaltyFactor',
        component: PenaltyFactorAnalysisView
      },
      {
        path: 'results/cost-effectiveness',
        name: 'ResultsCostEffectiveness',
        component: CostEffectivenessAnalysisView
      },
      {
        path: 'equipment-evaluation',
        name: 'EquipmentEvaluation',
        component: ExpertEquipmentEvaluation
      },
      {
        path: 'dynamic-indicator',
        name: 'DynamicIndicator',
        component: DynamicIndicatorView
      },
      {
        path: 'dynamic-quantitative',
        name: 'DynamicQuantitative',
        component: DynamicQuantitativeView
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
