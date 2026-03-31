<template>
  <div class="login-wrapper">
    <el-card class="login-card">
      <h2>招聘数据可视化系统</h2>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSubmit" :loading="loading">登录</el-button>
          <el-button type="text" @click="onRegister">注册</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { loginApi, registerApi } from '@/api/auth';
import { useUserStore } from '@/store/user';
import { ElMessage, FormInstance, FormRules } from 'element-plus';

const router = useRouter();
const userStore = useUserStore();

const form = reactive({
  username: '',
  password: ''
});

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
};

const formRef = ref<FormInstance>();
const loading = ref(false);

const onSubmit = () => {
  if (!formRef.value) return;
  formRef.value.validate(async (valid) => {
    if (!valid) return;
    loading.value = true;
    try {
      const user = await loginApi(form);
      userStore.setUser(user);
      ElMessage.success('登录成功');
      router.push('/');
    } finally {
      loading.value = false;
    }
  });
};

const onRegister = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning('请先输入用户名和密码');
    return;
  }
  await registerApi({
    username: form.username,
    password: form.password,
    email: form.username + '@example.com'
  });
  ElMessage.success('注册成功，请再次点击登录');
};
</script>

<style scoped>
.login-wrapper {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(120deg, #4facfe, #00f2fe);
}
.login-card {
  width: 360px;
}
h2 {
  text-align: center;
  margin-bottom: 20px;
}
</style>

