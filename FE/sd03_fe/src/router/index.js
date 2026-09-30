import { createRouter, createWebHistory } from 'vue-router'
import ProductListView from '../views/ProductListView.vue'
import ProductCreateView from '../views/ProductCreateView.vue'

const routes = [
  {
    path: '/',
    redirect: '/san-pham'
  },
  {
    path: '/san-pham',
    name: 'ProductList',
    component: ProductListView
  },
  {
    path: '/san-pham/them',
    name: 'ProductCreate',
    component: ProductCreateView
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

export default router
