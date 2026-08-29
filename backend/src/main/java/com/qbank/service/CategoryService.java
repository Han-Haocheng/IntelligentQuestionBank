package com.qbank.service;

import com.qbank.common.BusinessException;
import com.qbank.entity.Category;
import com.qbank.mapper.CategoryMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 分类服务(二级树)
 */
@Service
public class CategoryService {

    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    public List<Category> tree(Long userId) {
        List<Category> all = categoryMapper.selectByUser(userId);
        Map<Long, List<Category>> byParent = all.stream()
                .filter(c -> c.getParentId() != null && c.getParentId() != 0)
                .collect(Collectors.groupingBy(Category::getParentId));
        List<Category> roots = all.stream()
                .filter(c -> c.getParentId() == null || c.getParentId() == 0)
                .sorted(Comparator.comparing(c -> c.getSort() == null ? 0 : c.getSort()))
                .collect(Collectors.toList());
        for (Category root : roots) {
            List<Category> children = byParent.getOrDefault(root.getId(), new ArrayList<>());
            children.sort(Comparator.comparing(c -> c.getSort() == null ? 0 : c.getSort()));
            root.setChildren(children);
        }
        return roots;
    }

    public List<Category> list(Long userId) {
        return categoryMapper.selectByUser(userId);
    }

    public void add(Long userId, Category category) {
        validate(category);
        if (category.getParentId() == null) {
            category.setParentId(0L);
        }
        if (category.getParentId() != 0) {
            Category parent = categoryMapper.findById(category.getParentId());
            if (parent == null || !parent.getUserId().equals(userId)) {
                throw new BusinessException("父分类不存在");
            }
        }
        category.setUserId(userId);
        if (category.getSort() == null) {
            category.setSort(0);
        }
        categoryMapper.insert(category);
    }

    public void update(Long userId, Category category) {
        if (category.getId() == null) {
            throw new BusinessException("分类ID不能为空");
        }
        Category exist = requireOwned(userId, category.getId());
        validate(category);
        if (category.getParentId() != null && category.getParentId() != 0) {
            if (category.getParentId().equals(category.getId())) {
                throw new BusinessException("父分类不能是自身");
            }
            Category parent = categoryMapper.findById(category.getParentId());
            if (parent == null || !parent.getUserId().equals(userId)) {
                throw new BusinessException("父分类不存在");
            }
            if (exist.getParentId() != null && exist.getParentId() == 0
                    && categoryMapper.countChildren(userId, category.getId()) > 0) {
                throw new BusinessException("该分类包含子分类, 不能改为二级分类");
            }
        }
        category.setUserId(userId);
        categoryMapper.update(category);
    }

    public void delete(Long userId, Long id) {
        requireOwned(userId, id);
        if (categoryMapper.countChildren(userId, id) > 0) {
            throw new BusinessException("请先删除该分类下的子分类");
        }
        if (categoryMapper.countQuestions(id) > 0) {
            throw new BusinessException("该分类下存在题目, 不能删除");
        }
        categoryMapper.deleteById(id);
    }

    private Category requireOwned(Long userId, Long id) {
        Category category = categoryMapper.findById(id);
        if (category == null || !category.getUserId().equals(userId)) {
            throw new BusinessException("分类不存在或无权操作");
        }
        return category;
    }

    private void validate(Category category) {
        if (!StringUtils.hasText(category.getName())) {
            throw new BusinessException("分类名称不能为空");
        }
        if (category.getName().length() > 20) {
            throw new BusinessException("分类名称不能超过20个字符");
        }
    }
}
