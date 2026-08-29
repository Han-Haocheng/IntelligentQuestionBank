<template>
  <div>
    <!-- 管理页: 分类管理 + 用户管理 (仅管理员可见) -->
    <el-tabs v-model="tab">
      <el-tab-pane label="分类管理" name="categories">
        <CategoriesView />
      </el-tab-pane>
      <el-tab-pane label="用户管理" name="users">
        <UsersView />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import CategoriesView from './CategoriesView.vue'
import UsersView from './UsersView.vue'

const tab = ref('categories')
</script>
