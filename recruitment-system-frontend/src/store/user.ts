import { defineStore } from 'pinia';

interface UserState {
  id: number | null;
  username: string;
  role: string;
  token: string;
}

export const useUserStore = defineStore('user', {
  state: (): { user: UserState | null } => {
    const str = localStorage.getItem('user');
    if (str) {
      return { user: JSON.parse(str) as UserState };
    }
    return { user: null };
  },
  getters: {
    isLogin: (state) => !!state.user?.token,
    token: (state) => state.user?.token || ''
  },
  actions: {
    setUser(user: UserState) {
      this.user = user;
      localStorage.setItem('user', JSON.stringify(user));
    },
    loadFromStorage() {
      const str = localStorage.getItem('user');
      if (str) {
        this.user = JSON.parse(str);
      }
    },
    logout() {
      this.user = null;
      localStorage.removeItem('user');
    }
  }
});

