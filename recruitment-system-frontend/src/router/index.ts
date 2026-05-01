import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router';
import { useUserStore } from '@/store/user';
import { autoLoginApi } from '@/api/auth';

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
        path: 'compare',
        name: 'DataCompare',
        component: () => import('@/views/DataCompare.vue')
      },
      {
        path: 'analysis',
        name: 'Analysis',
        component: () => import('@/views/Analysis.vue')
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/Settings.vue')
      },
      {
        path: 'ai',
        name: 'AIChat',
        component: () => import('@/views/AIChat.vue')
      }
    ]
  },
  // [优化] 添加404通配路由，访问不存在的路径时重定向到首页
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

let autoLoginPromise: Promise<void> | null = null;

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore();
  // Ensure token is loaded before auth guard runs (refresh/back-button scenario)
  userStore.loadFromStorage();

  // 已登录或正在访问登录页：直接放行
  if (userStore.isLogin || to.path === '/login') {
    next();
    return;
  }

  // 未登录但需要访问其它页面：尝试自动登录
  if (!autoLoginPromise) {
    autoLoginPromise = (async () => {
      const user = await autoLoginApi();
      userStore.setUser(user);
    })();
  }

  try {
    await autoLoginPromise;
    if (userStore.isLogin) {
      next();
    } else {
      next('/login');
    }
  } catch (_) {
    autoLoginPromise = null;
    // [安全优化] 移除 prefillLoginCredentials，不再自动填充密码
    next('/login');
  }
});

export default router;

