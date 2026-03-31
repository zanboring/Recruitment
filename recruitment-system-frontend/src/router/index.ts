import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router';
import { useUserStore } from '@/store/user';

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/Home.vue')
      },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue')
      },
      {
        path: 'jobs',
        name: 'JobList',
        component: () => import('@/views/JobList.vue')
      },
      {
        path: 'data',
        name: 'DataManage',
        component: () => import('@/views/DataManage.vue')
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/Settings.vue')
      }
    ]
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach((to, from, next) => {
  const userStore = useUserStore();
  // Ensure token is loaded before auth guard runs (refresh/back-button scenario)
  userStore.loadFromStorage();
  if (!userStore.isLogin && to.path !== '/login') {
    next('/login');
  } else {
    next();
  }
});

export default router;

