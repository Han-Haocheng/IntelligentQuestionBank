<template>
  <div>
    <!-- 我的: 收藏夹 + 共享 + 个人资料 -->
    <el-tabs v-model="tab">
      <el-tab-pane label="收藏夹" name="favorites">
        <FavoritesView />
      </el-tab-pane>
      <el-tab-pane label="共享" name="shares">
        <SharesView />
      </el-tab-pane>
      <el-tab-pane label="个人资料" name="profile">
        <ProfileView />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import FavoritesView from './FavoritesView.vue'
import SharesView from './SharesView.vue'
import ProfileView from './ProfileView.vue'

const route = useRoute()
const tab = ref(['favorites', 'shares', 'profile'].includes(route.query.tab) ? route.query.tab : 'favorites')
</script>
