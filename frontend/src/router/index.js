import { createRouter, createWebHistory } from 'vue-router'
import DataView from '../views/DataView.vue'
import EvaluationView from '../views/EvaluationView.vue'
import ExpertCredibility from '../views/ExpertCredibility.vue'

const routes = [
  {
    path: '/',
    redirect: '/data'
  },
  {
    path: '/data',
    name: 'Data',
    component: DataView
  },
  {
    path: '/evaluation',
    name: 'Evaluation',
    component: EvaluationView
  },
  {
    path: '/expert',
    name: 'Expert',
    component: ExpertCredibility
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
