package com.qbank.service;

import com.qbank.common.BusinessException;
import com.qbank.entity.Category;
import com.qbank.mapper.CategoryMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 分类服务测试: 树构建 / 父级与子级保护 / 合并 / 影响面统计
 */
class CategoryServiceTest {

    private Category category(long id, String name, Long parentId, Integer sort) {
        Category c = new Category();
        c.setId(id);
        c.setName(name);
        c.setParentId(parentId);
        c.setSort(sort);
        return c;
    }

    @Test
    void treeBuildsHierarchyAndSortsChildren() {
        CategoryMapper mapper = mock(CategoryMapper.class);
        when(mapper.selectAll()).thenReturn(List.of(
                category(1L, "Java", 0L, 2),
                category(2L, "数据库", null, 1),
                category(3L, "语法", 1L, 1),
                category(4L, "框架", 1L, 0),
                category(5L, "MySQL", 2L, 3)
        ));
        CategoryService service = new CategoryService(mapper);
        List<Category> roots = service.tree(1L);
        // 根按 sort 升序: 数据库(1) 在前, Java(2) 在后
        assertThat(roots).extracting(Category::getId).containsExactly(2L, 1L);
        assertThat(roots.get(1).getChildren()).extracting(Category::getId).containsExactly(4L, 3L);
        assertThat(roots.get(0).getChildren()).extracting(Category::getId).containsExactly(5L);
    }

    @Test
    void listReturnsAll() {
        CategoryMapper mapper = mock(CategoryMapper.class);
        when(mapper.selectAll()).thenReturn(List.of(category(1L, "a", null, 1)));
        CategoryService service = new CategoryService(mapper);
        assertThat(service.list(1L)).hasSize(1);
    }

    @Test
    void addWithoutParentDefaultsRoot() {
        CategoryMapper mapper = mock(CategoryMapper.class);
        CategoryService service = new CategoryService(mapper);
        Category c = category(0, "网络", null, null);
        service.add(7L, c);
        assertThat(c.getParentId()).isEqualTo(0L);
        assertThat(c.getSort()).isEqualTo(0);
        assertThat(c.getUserId()).isEqualTo(7L);
        verify(mapper).insert(c);
    }

    @Test
    void addMissingParentThrows() {
        CategoryMapper mapper = mock(CategoryMapper.class);
        when(mapper.findById(5L)).thenReturn(null);
        CategoryService service = new CategoryService(mapper);
        Category c = category(0, "网络", 5L, 1);
        assertThatThrownBy(() -> service.add(7L, c))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("父分类不存在");
        verify(mapper, never()).insert(any());
    }

    @Test
    void addInvalidNameThrows() {
        CategoryMapper mapper = mock(CategoryMapper.class);
        CategoryService service = new CategoryService(mapper);
        assertThatThrownBy(() -> service.add(7L, category(0, " ", null, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("分类名称不能为空");
        assertThatThrownBy(() -> service.add(7L, category(0, "x".repeat(21), null, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能超过20个字符");
    }

    @Test
    void updateMissingIdThrows() {
        CategoryService service = new CategoryService(mock(CategoryMapper.class));
        Category c = new Category();
        c.setName("x");  // id 为 null
        assertThatThrownBy(() -> service.update(7L, c))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("分类ID不能为空");
    }

    @Test
    void updateSelfParentThrows() {
        CategoryMapper mapper = mock(CategoryMapper.class);
        when(mapper.findById(1L)).thenReturn(category(1L, "x", null, 1));
        CategoryService service = new CategoryService(mapper);
        assertThatThrownBy(() -> service.update(7L, category(1L, "x", 1L, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("父分类不能是自身");
    }

    @Test
    void updateParentNotFoundThrows() {
        CategoryMapper mapper = mock(CategoryMapper.class);
        when(mapper.findById(1L)).thenReturn(category(1L, "x", null, 1));
        when(mapper.findById(5L)).thenReturn(null);
        CategoryService service = new CategoryService(mapper);
        assertThatThrownBy(() -> service.update(7L, category(1L, "x", 5L, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("父分类不存在");
    }

    @Test
    void updateRootWithChildrenToSubThrows() {
        CategoryMapper mapper = mock(CategoryMapper.class);
        // 1 号是根分类(父级 0)且含子分类
        when(mapper.findById(1L)).thenReturn(category(1L, "根", 0L, 1));
        when(mapper.findById(5L)).thenReturn(category(5L, "新父", 0L, 1));
        when(mapper.countChildren(1L)).thenReturn(2);
        CategoryService service = new CategoryService(mapper);
        assertThatThrownBy(() -> service.update(7L, category(1L, "根", 5L, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能改为二级分类");
    }

    @Test
    void updateOk() {
        CategoryMapper mapper = mock(CategoryMapper.class);
        when(mapper.findById(1L)).thenReturn(category(1L, "旧", null, 1));
        when(mapper.findById(5L)).thenReturn(category(5L, "父", 0L, 1));
        CategoryService service = new CategoryService(mapper);
        Category c = category(1L, "新名", 5L, 2);
        service.update(7L, c);
        verify(mapper).update(c);
    }

    @Test
    void deleteWithChildrenThrows() {
        CategoryMapper mapper = mock(CategoryMapper.class);
        when(mapper.findById(1L)).thenReturn(category(1L, "x", null, 1));
        when(mapper.countChildren(1L)).thenReturn(2);
        CategoryService service = new CategoryService(mapper);
        assertThatThrownBy(() -> service.delete(7L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("子分类");
        verify(mapper, never()).deleteById(anyLong());
    }

    @Test
    void deleteWithQuestionsThrows() {
        CategoryMapper mapper = mock(CategoryMapper.class);
        when(mapper.findById(1L)).thenReturn(category(1L, "x", null, 1));
        when(mapper.countChildren(1L)).thenReturn(0);
        when(mapper.countQuestions(1L)).thenReturn(3);
        CategoryService service = new CategoryService(mapper);
        assertThatThrownBy(() -> service.delete(7L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("存在题目");
        verify(mapper, never()).deleteById(anyLong());
    }

    @Test
    void deleteOk() {
        CategoryMapper mapper = mock(CategoryMapper.class);
        when(mapper.findById(1L)).thenReturn(category(1L, "x", null, 1));
        when(mapper.countChildren(1L)).thenReturn(0);
        when(mapper.countQuestions(1L)).thenReturn(0);
        CategoryService service = new CategoryService(mapper);
        service.delete(7L, 1L);
        verify(mapper).deleteById(1L);
    }

    @Test
    void sortWritesIndexOrder() {
        CategoryMapper mapper = mock(CategoryMapper.class);
        CategoryService service = new CategoryService(mapper);
        // List.of 不允许 null, 用 Arrays.asList 模拟拖拽列表中的空位
        service.sort(1L, java.util.Arrays.asList(5L, null, 3L));
        verify(mapper).updateSort(5L, 0);
        verify(mapper).updateSort(3L, 2);
    }

    @Test
    void sortNullIdsNoop() {
        CategoryMapper mapper = mock(CategoryMapper.class);
        CategoryService service = new CategoryService(mapper);
        service.sort(1L, null);
        service.sort(1L, List.of());
        verify(mapper, never()).updateSort(anyLong(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void mergeSameFromToThrows() {
        CategoryService service = new CategoryService(mock(CategoryMapper.class));
        assertThatThrownBy(() -> service.merge(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("两个不同的分类");
    }

    @Test
    void mergeMissingCategoryThrows() {
        CategoryMapper mapper = mock(CategoryMapper.class);
        when(mapper.findById(1L)).thenReturn(null);
        CategoryService service = new CategoryService(mapper);
        assertThatThrownBy(() -> service.merge(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("分类不存在");
    }

    @Test
    void mergeIntoOwnChildThrows() {
        CategoryMapper mapper = mock(CategoryMapper.class);
        when(mapper.findById(1L)).thenReturn(category(1L, "父", null, 1));
        when(mapper.findById(3L)).thenReturn(category(3L, "子", 1L, 1));
        when(mapper.countChildren(1L)).thenReturn(2);
        when(mapper.selectChildren(1L)).thenReturn(List.of(category(3L, "子", 1L, 1)));
        CategoryService service = new CategoryService(mapper);
        assertThatThrownBy(() -> service.merge(1L, 3L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能合并到自己的子分类");
        verify(mapper, never()).moveQuestions(anyLong(), anyLong());
    }

    @Test
    void mergeOkReturnsMovedCount() {
        CategoryMapper mapper = mock(CategoryMapper.class);
        when(mapper.findById(1L)).thenReturn(category(1L, "a", null, 1));
        when(mapper.findById(2L)).thenReturn(category(2L, "b", null, 1));
        when(mapper.countChildren(1L)).thenReturn(0);
        when(mapper.moveQuestions(1L, 2L)).thenReturn(3);
        CategoryService service = new CategoryService(mapper);
        assertThat(service.merge(1L, 2L)).isEqualTo(3);
    }

    @Test
    void countMissingThrows() {
        CategoryMapper mapper = mock(CategoryMapper.class);
        when(mapper.findById(1L)).thenReturn(null);
        CategoryService service = new CategoryService(mapper);
        assertThatThrownBy(() -> service.count(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("分类不存在");
    }

    @Test
    void countOk() {
        CategoryMapper mapper = mock(CategoryMapper.class);
        when(mapper.findById(1L)).thenReturn(category(1L, "a", null, 1));
        when(mapper.countQuestionsInSubtree(1L)).thenReturn(4);
        when(mapper.countChildren(1L)).thenReturn(2);
        CategoryService service = new CategoryService(mapper);
        Map<String, Object> map = service.count(1L);
        assertThat(map.get("questionCount")).isEqualTo(4);
        assertThat(map.get("childCount")).isEqualTo(2);
    }
}
