<template>
  <el-container class="layout">
    <el-aside width="200px">
      <div class="logo">招聘可视化系统</div>
      <el-menu :default-active="active" router>
        <el-menu-item index="/">首页</el-menu-item>
        <el-menu-item index="/dashboard">数据可视化</el-menu-item>
        <el-menu-item index="/jobs">岗位列表</el-menu-item>
        <el-menu-item index="/data">数据管理</el-menu-item>
        <el-menu-item index="/settings">系统设置</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="right">
          <span class="user">{{ username }}</span>
          <el-button type="text" @click="logout">退出</el-button>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useUserStore } from '@/store/user';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
userStore.loadFromStorage();

const active = computed(() => route.path);
const username = computed(() => userStore.user?.username || '');

const logout = () => {
  userStore.logout();
  router.push('/login');
};
</script>

<style scoped>
.layout {
  height: 100vh;
}
.logo {
  padding: 16px;
  font-weight: bold;
  text-align: center;
}
.header {
  display: flex;
  justify-content: flex-end;
  align-items: center;
}
.right {
  display: flex;
  align-items: center;
}
.user {
  margin-right: 12px;
}
</style>

